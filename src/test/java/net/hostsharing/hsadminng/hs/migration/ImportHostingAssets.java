package net.hostsharing.hsadminng.hs.migration;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hash.HashGenerator;
import net.hostsharing.hsadminng.hash.HashGenerator.Algorithm;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ALIAS;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV4_NUMBER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/*
 * This 'test' includes the complete legacy 'office' data import.
 *
 * There is no code in 'main' because the import is not needed a normal runtime.
 * There is some test data in Java resources to verify the data conversion.
 * For a real import a main method will be added later
 * which reads CSV files from the file system.
 *
 * When run on a Hostsharing database, it needs the following settings (hsh99_... just examples).
 *
 * In a real Hostsharing environment, these are created via (the old) hsadmin:

    CREATE USER hsh99_admin WITH PASSWORD 'password';
    CREATE DATABASE hsh99_hsadminng  ENCODING 'UTF8' TEMPLATE template0;
    REVOKE ALL ON DATABASE hsh99_hsadminng FROM public; -- why does hsadmin do that?
    ALTER DATABASE hsh99_hsadminng OWNER TO hsh99_admin;

    CREATE USER hsh99_restricted WITH PASSWORD 'password';

    \c hsh99_hsadminng

    GRANT ALL PRIVILEGES ON SCHEMA public to hsh99_admin;

 * Additionally, we need these settings (because the Hostsharing DB-Admin has no CREATE right):

    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    -- maybe something like that is needed for the 2nd user
    -- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public to hsh99_restricted;

 * Then copy the file .tc-environment to a file named .environment (excluded from git) and fill in your specific values.

 * To finally import the office data, run:
 *
 *   gw-importHostingAssets # comes from .aliases file and uses .environment
 */
@Tag("importHostingAssets")
@DataJpaTest(properties = {
        "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:15.5-bookworm:///spring_boot_testcontainers}",
        "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:ADMIN}",
        "spring.datasource.password=${HSADMINNG_POSTGRES_ADMIN_PASSWORD:password}",
        "hsadminng.superuser=${HSADMINNG_SUPERUSER:superuser-alex@hostsharing.net}"
})
@DirtiesContext
@Import({ Context.class, JpaAttempt.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(OrderedDependedTestsExtension.class)
public class ImportHostingAssets extends ImportOfficeData {

    static final Integer IP_NUMBER_ID_OFFSET = 1000000;
    static final Integer HIVE_ID_OFFSET = 2000000;
    static final Integer PACKET_ID_OFFSET = 3000000;
    static final Integer UNIXUSER_ID_OFFSET = 4000000;
    static final Integer EMAILALIAS_ID_OFFSET = 5000000;
    static final Integer DBINSTANCE_ID_OFFSET = 6000000;
    static final Integer DBUSER_ID_OFFSET = 7000000;
    static final Integer DB_ID_OFFSET = 8000000;

    record Hive(int hive_id, String hive_name, int inet_addr_id, AtomicReference<HsHostingAssetRawEntity> serverRef) {}

    static Map<Integer, HsBookingProjectEntity> bookingProjects = new WriteOnceMap<>();
    static Map<Integer, HsBookingItemEntity> bookingItems = new WriteOnceMap<>();
    static Map<Integer, Hive> hives = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRawEntity> hostingAssets = new WriteOnceMap<>(); // TODO.impl: separate maps for each type?
    static Map<String, HsHostingAssetRawEntity> dbUsersByEngineAndName = new WriteOnceMap<>();

    @Test
    @Order(11010)
    void createBookingProjects() {
        debitors.forEach((id, debitor) -> {
            bookingProjects.put(id, HsBookingProjectEntity.builder()
                    .caption(debitor.getDefaultPrefix() + " default project")
                    .debitor(em.find(HsBookingDebitorEntity.class, debitor.getUuid()))
                    .build());
        });
    }

    @Test
    @Order(12010)
    void importIpNumbers() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/inet_addr.csv")) {
            final var lines = readAllLines(reader);
            importIpNumbers(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(12019)
    void verifyIpNumbers() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(5, IPV4_NUMBER)).isEqualToIgnoringWhitespace("""
                {
                   1000363=HsHostingAssetRawEntity(IPV4_NUMBER, 83.223.95.34),
                   1000381=HsHostingAssetRawEntity(IPV4_NUMBER, 83.223.95.52),
                   1000402=HsHostingAssetRawEntity(IPV4_NUMBER, 83.223.95.73),
                   1000433=HsHostingAssetRawEntity(IPV4_NUMBER, 83.223.95.104),
                   1000457=HsHostingAssetRawEntity(IPV4_NUMBER, 83.223.95.128)
                }
                """);
    }

    @Test
    @Order(12030)
    void importHives() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/hive.csv")) {
            final var lines = readAllLines(reader);
            importHives(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(12039)
    void verifyHives() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(first(5, hives))).isEqualToIgnoringWhitespace("""
                {
                   2000001=Hive[hive_id=1, hive_name=h00, inet_addr_id=358, serverRef=null],
                   2000002=Hive[hive_id=2, hive_name=h01, inet_addr_id=359, serverRef=null],
                   2000004=Hive[hive_id=4, hive_name=h02, inet_addr_id=360, serverRef=null],
                   2000007=Hive[hive_id=7, hive_name=h03, inet_addr_id=361, serverRef=null],
                   2000013=Hive[hive_id=13, hive_name=h04, inet_addr_id=430, serverRef=null]
                }
                """);
    }

    @Test
    @Order(13000)
    void importPackets() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/packet.csv")) {
            final var lines = readAllLines(reader);
            importPackets(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(13009)
    void verifyPackets() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(3, CLOUD_SERVER, MANAGED_SERVER, MANAGED_WEBSPACE)).isEqualToIgnoringWhitespace("""
                {
                   3000630=HsHostingAssetRawEntity(MANAGED_WEBSPACE,   hsh00, HA hsh00,  MANAGED_SERVER:vm1050,    D-1000000:hsh default project:BI hsh00),
                   3000968=HsHostingAssetRawEntity(MANAGED_SERVER,    vm1061, HA vm1061,                           D-1015200:rar default project:BI vm1061),
                   3000978=HsHostingAssetRawEntity(MANAGED_SERVER,    vm1050, HA vm1050,                           D-1000000:hsh default project:BI vm1050),
                   3001061=HsHostingAssetRawEntity(MANAGED_SERVER,    vm1068, HA vm1068,                           D-1000300:mim default project:BI vm1068),
                   3001094=HsHostingAssetRawEntity(MANAGED_WEBSPACE,   lug00, HA lug00,  MANAGED_SERVER:vm1068,    D-1000300:mim default project:BI lug00),
                   3001112=HsHostingAssetRawEntity(MANAGED_WEBSPACE,   mim00, HA mim00,  MANAGED_SERVER:vm1068,    D-1000300:mim default project:BI mim00),
                   3023611=HsHostingAssetRawEntity(CLOUD_SERVER,      vm2097, HA vm2097,                           D-1101800:wws default project:BI vm2097)
                }
                """);
        assertThat(firstOfEachType(
                3,
                HsBookingItemType.CLOUD_SERVER,
                HsBookingItemType.MANAGED_SERVER,
                HsBookingItemType.MANAGED_WEBSPACE)).isEqualToIgnoringWhitespace("""
                {
                   3000630=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_WEBSPACE, [2001-06-01,), BI hsh00),
                   3000968=HsBookingItemEntity(D-1015200:rar default project, MANAGED_SERVER, [2013-04-01,), BI vm1061),
                   3000978=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2013-04-01,), BI vm1050),
                   3001061=HsBookingItemEntity(D-1000300:mim default project, MANAGED_SERVER, [2013-08-19,), BI vm1068),
                   3001094=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-10,), BI lug00),
                   3001112=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-17,), BI mim00),
                   3023611=HsBookingItemEntity(D-1101800:wws default project, CLOUD_SERVER, [2022-08-10,), BI vm2097)
                }
                """);
    }

    @Test
    @Order(13010)
    void importPacketComponents() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/packet_component.csv")) {
            final var lines = readAllLines(reader);
            importPacketComponents(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(13019)
    void verifyPacketComponents() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(5, CLOUD_SERVER, MANAGED_SERVER, MANAGED_WEBSPACE))
                .isEqualToIgnoringWhitespace("""
                        {
                           3000630=HsHostingAssetRawEntity(MANAGED_WEBSPACE, hsh00, HA hsh00, MANAGED_SERVER:vm1050, D-1000000:hsh default project:BI hsh00),
                           3000968=HsHostingAssetRawEntity(MANAGED_SERVER, vm1061, HA vm1061, D-1015200:rar default project:BI vm1061),
                           3000978=HsHostingAssetRawEntity(MANAGED_SERVER, vm1050, HA vm1050, D-1000000:hsh default project:BI vm1050),
                           3001061=HsHostingAssetRawEntity(MANAGED_SERVER, vm1068, HA vm1068, D-1000300:mim default project:BI vm1068),
                           3001094=HsHostingAssetRawEntity(MANAGED_WEBSPACE, lug00, HA lug00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI lug00),
                           3001112=HsHostingAssetRawEntity(MANAGED_WEBSPACE, mim00, HA mim00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI mim00),
                           3001447=HsHostingAssetRawEntity(MANAGED_SERVER, vm1093, HA vm1093, D-1000000:hsh default project:BI vm1093),
                           3019959=HsHostingAssetRawEntity(MANAGED_WEBSPACE, dph00, HA dph00, MANAGED_SERVER:vm1093, D-1101900:dph default project:BI dph00),
                           3023611=HsHostingAssetRawEntity(CLOUD_SERVER, vm2097, HA vm2097, D-1101800:wws default project:BI vm2097)
                        }
                        """);
        assertThat(firstOfEachType(
                5,
                HsBookingItemType.CLOUD_SERVER,
                HsBookingItemType.MANAGED_SERVER,
                HsBookingItemType.MANAGED_WEBSPACE))
                .isEqualToIgnoringWhitespace("""
                        {
                           3000630=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_WEBSPACE, [2001-06-01,), BI hsh00, { "HDD": 10, "Multi": 25, "SLA-Platform": "EXT24H", "SSD": 16, "Traffic": 50}),
                           3000968=HsBookingItemEntity(D-1015200:rar default project, MANAGED_SERVER, [2013-04-01,), BI vm1061, { "CPU": 6, "HDD": 250, "RAM": 14, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 375, "Traffic": 250}),
                           3000978=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2013-04-01,), BI vm1050, { "CPU": 4, "HDD": 250, "RAM": 32, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 150, "Traffic": 250}),
                           3001061=HsBookingItemEntity(D-1000300:mim default project, MANAGED_SERVER, [2013-08-19,), BI vm1068, { "CPU": 2, "HDD": 250, "RAM": 4, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT2H", "SLA-Web": true, "Traffic": 250}),
                           3001094=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-10,), BI lug00, { "Multi": 5, "SLA-Platform": "EXT24H", "SSD": 1, "Traffic": 10}),
                           3001112=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-17,), BI mim00, { "Multi": 5, "SLA-Platform": "EXT24H", "SSD": 3, "Traffic": 20}),
                           3001447=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2014-11-28,), BI vm1093, { "CPU": 6, "HDD": 500, "RAM": 16, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 300, "Traffic": 250}),
                           3019959=HsBookingItemEntity(D-1101900:dph default project, MANAGED_WEBSPACE, [2021-06-02,), BI dph00, { "Multi": 1, "SLA-Platform": "EXT24H", "SSD": 25, "Traffic": 20}),
                           3023611=HsBookingItemEntity(D-1101800:wws default project, CLOUD_SERVER, [2022-08-10,), BI vm2097, { "CPU": 8, "RAM": 12, "SLA-Infrastructure": "EXT4H", "SSD": 25, "Traffic": 250})
                        }
                        """);
    }

    @Test
    @Order(14010)
    void importUnixUsers() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/unixuser.csv")) {
            final var lines = readAllLines(reader);
            importUnixUsers(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(14019)
    void verifyUnixUsers() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(15, UNIX_USER)).isEqualToIgnoringWhitespace("""
                {
                   4005803=HsHostingAssetRawEntity(UNIX_USER, lug00, LUGs, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102090}),
                   4005805=HsHostingAssetRawEntity(UNIX_USER, lug00-wla.1, Paul Klemm, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102091}),
                   4005809=HsHostingAssetRawEntity(UNIX_USER, lug00-wla.2, Walter Müller, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 8, "SSD soft quota": 4, "locked": false, "shell": "/bin/bash", "userid": 102093}),
                   4005811=HsHostingAssetRawEntity(UNIX_USER, lug00-ola.a, LUG OLA - POP a, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/usr/bin/passwd", "userid": 102094}),
                   4005813=HsHostingAssetRawEntity(UNIX_USER, lug00-ola.b, LUG OLA - POP b, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/usr/bin/passwd", "userid": 102095}),
                   4005835=HsHostingAssetRawEntity(UNIX_USER, lug00-test, Test, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 1024, "SSD soft quota": 1024, "locked": false, "shell": "/usr/bin/passwd", "userid": 102106}),
                   4005964=HsHostingAssetRawEntity(UNIX_USER, mim00, Michael Mellis, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102147}),
                   4005966=HsHostingAssetRawEntity(UNIX_USER, mim00-1981, Jahrgangstreffen 1981, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 256, "SSD soft quota": 128, "locked": false, "shell": "/bin/bash", "userid": 102148}),
                   4005990=HsHostingAssetRawEntity(UNIX_USER, mim00-mail, Mailbox, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102160}),
                   4100705=HsHostingAssetRawEntity(UNIX_USER, hsh00-mim, Michael Mellis, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/false", "userid": 10003}),
                   4100824=HsHostingAssetRawEntity(UNIX_USER, hsh00, Hostsharing Paket, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 10000}),
                   4167846=HsHostingAssetRawEntity(UNIX_USER, hsh00-dph, hsh00-uph, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/false", "userid": 110568}),
                   4169546=HsHostingAssetRawEntity(UNIX_USER, dph00, Reinhard Wiese, MANAGED_WEBSPACE:dph00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 110593}),
                   4169596=HsHostingAssetRawEntity(UNIX_USER, dph00-uph, Domain admin, MANAGED_WEBSPACE:dph00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 110594})
                }
                """);
    }

    @Test
    @Order(14020)
    void importEmailAliases() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/emailalias.csv")) {
            final var lines = readAllLines(reader);
            importEmailAliases(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(14029)
    void verifyEmailAliases() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(15, EMAIL_ALIAS)).isEqualToIgnoringWhitespace("""
                {
                   5002403=HsHostingAssetRawEntity(EMAIL_ALIAS, lug00, lug00, MANAGED_WEBSPACE:lug00, { "target": "[michael.mellis@example.com]"}),
                   5002405=HsHostingAssetRawEntity(EMAIL_ALIAS, lug00-wla-listar, lug00-wla-listar, MANAGED_WEBSPACE:lug00, { "target": "[|/home/pacs/lug00/users/in/mailinglist/listar]"}),
                   5002429=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00, mim00, MANAGED_WEBSPACE:mim00, { "target": "[mim12-mi@mim12.hostsharing.net]"}),
                   5002431=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-abruf, mim00-abruf, MANAGED_WEBSPACE:mim00, { "target": "[michael.mellis@hostsharing.net]"}),
                   5002449=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-hhfx, mim00-hhfx, MANAGED_WEBSPACE:mim00, { "target": "[mim00-hhfx, |/usr/bin/formail -I 'Reply-To: hamburger-fx@example.net' | /usr/lib/sendmail mim00-hhfx-l]"}),
                   5002451=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-hhfx-l, mim00-hhfx-l, MANAGED_WEBSPACE:mim00, { "target": "[:include:/home/pacs/mim00/etc/hhfx.list]"}),
                   5002452=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-empty, mim00-empty, MANAGED_WEBSPACE:mim00, { "target": "[]"}),
                   5002453=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-0_entries, mim00-0_entries, MANAGED_WEBSPACE:mim00, { "target": "[]"}),
                   5002454=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-dev.null, mim00-dev.null, MANAGED_WEBSPACE:mim00, { "target": "[/dev/null]"}),
                   5002455=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-1_with_space, mim00-1_with_space, MANAGED_WEBSPACE:mim00, { "target": "[|/home/pacs/mim00/install/corpslistar/listar]"}),
                   5002456=HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-1_with_single_quotes, mim00-1_with_single_quotes, MANAGED_WEBSPACE:mim00, { "target": "[|/home/pacs/rir00/mailinglist/ecartis -r kybs06-intern]"})
                }
                """);
    }

    @Test
    @Order(15000)
    void createDatabaseInstances() {
        createDatabaseInstances(hostingAssets.values().stream().filter(ha -> ha.getType()==MANAGED_SERVER).toList());
    }

    @Test
    @Order(15009)
    void verifyDatabaseInstances() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(5, PGSQL_INSTANCE, MARIADB_INSTANCE)).isEqualToIgnoringWhitespace("""
                {
                   6000000=HsHostingAssetRawEntity(PGSQL_INSTANCE, vm1061|PgSql.default, vm1061-PostgreSQL default instance, MANAGED_SERVER:vm1061),
                   6000001=HsHostingAssetRawEntity(MARIADB_INSTANCE, vm1061|MariaDB.default, vm1061-MariaDB default instance, MANAGED_SERVER:vm1061),
                   6000002=HsHostingAssetRawEntity(PGSQL_INSTANCE, vm1050|PgSql.default, vm1050-PostgreSQL default instance, MANAGED_SERVER:vm1050),
                   6000003=HsHostingAssetRawEntity(MARIADB_INSTANCE, vm1050|MariaDB.default, vm1050-MariaDB default instance, MANAGED_SERVER:vm1050),
                   6000004=HsHostingAssetRawEntity(PGSQL_INSTANCE, vm1068|PgSql.default, vm1068-PostgreSQL default instance, MANAGED_SERVER:vm1068),
                   6000005=HsHostingAssetRawEntity(MARIADB_INSTANCE, vm1068|MariaDB.default, vm1068-MariaDB default instance, MANAGED_SERVER:vm1068),
                   6000006=HsHostingAssetRawEntity(PGSQL_INSTANCE, vm1093|PgSql.default, vm1093-PostgreSQL default instance, MANAGED_SERVER:vm1093),
                   6000007=HsHostingAssetRawEntity(MARIADB_INSTANCE, vm1093|MariaDB.default, vm1093-MariaDB default instance, MANAGED_SERVER:vm1093)
                }
                """);
    }

    @Test
    @Order(15010)
    void importDatabaseUsers() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/database_user.csv")) {
            final var lines = readAllLines(reader);
            importDatabaseUsers(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(15019)
    void verifyDatabaseUsers() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(5, PGSQL_USER, MARIADB_USER)).isEqualToIgnoringWhitespace("""
                {
                   7001857=HsHostingAssetRawEntity(PGSQL_USER, PGU|hsh00, hsh00, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$JDiZmaxU+O+ByArLY/CkYZ8HbOk0r/I8LyABnno5gQs=:NI3T500/63dzI1B07Jh3UtQGlukS6JxuS0XoxM/QgAc="}),
                   7001858=HsHostingAssetRawEntity(MARIADB_USER, MAU|hsh00, hsh00, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*59067A36BA197AD0A47D74909296C5B002A0FB9F"}),
                   7001859=HsHostingAssetRawEntity(PGSQL_USER, PGU|hsh00_vorstand, hsh00_vorstand, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$54Wh+OGx/GaIvAia+I3k78jHGhqmYwe4+iLssmH5zhk=:D4Gq1z2Li2BVSaZrz1azDrs6pwsIzhq4+suK1Hh6ZIg="}),
                   7001860=HsHostingAssetRawEntity(PGSQL_USER, PGU|hsh00_hsadmin, hsh00_hsadmin, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$54Wh+OGx/GaIvAia+I3k78jHGhqmYwe4+iLssmH5zhk=:D4Gq1z2Li2BVSaZrz1azDrs6pwsIzhq4+suK1Hh6ZIg="}),
                   7001861=HsHostingAssetRawEntity(PGSQL_USER, PGU|hsh00_hsadmin_ro, hsh00_hsadmin_ro, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$UhJnJJhmKANbcaG+izWK3rz5bmhhluSuiCJFlUmDVI8=:6AC4mbLfJGiGlEOWhpz9BivvMODhLLHOnRnnktJPgn8="}),
                   7004908=HsHostingAssetRawEntity(MARIADB_USER, MAU|hsh00_mantis, hsh00_mantis, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*EA4C0889A22AAE66BBEBC88161E8CF862D73B44F"}),
                   7004909=HsHostingAssetRawEntity(MARIADB_USER, MAU|hsh00_mantis_ro, hsh00_mantis_ro, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*B3BB6D0DA2EC01958616E9B3BCD2926FE8C38383"}),
                   7004931=HsHostingAssetRawEntity(PGSQL_USER, PGU|hsh00_phpPgSqlAdmin, hsh00_phpPgSqlAdmin, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$UhJnJJhmKANbcaG+izWK3rz5bmhhluSuiCJFlUmDVI8=:6AC4mbLfJGiGlEOWhpz9BivvMODhLLHOnRnnktJPgn8="}),
                   7004932=HsHostingAssetRawEntity(MARIADB_USER, MAU|hsh00_phpMyAdmin, hsh00_phpMyAdmin, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*3188720B1889EF5447C722629765F296F40257C2"}),
                   7007520=HsHostingAssetRawEntity(MARIADB_USER, MAU|lug00_wla, lug00_wla, MANAGED_WEBSPACE:lug00, MARIADB_INSTANCE:vm1068|MariaDB.default, { "password": "*11667C0EAC42BF8B0295ABEDC7D2868A835E4DB5"})
                }
                """);
    }

    @Test
    @Order(15020)
    void importDatabases() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/database.csv")) {
            final var lines = readAllLines(reader);
            importDatabases(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(15029)
    void verifyDatabases() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(5, PGSQL_DATABASE, MARIADB_DATABASE)).isEqualToIgnoringWhitespace("""
                {
                   8000077=HsHostingAssetRawEntity(PGSQL_DATABASE, PGD|hsh00_vorstand, hsh00_vorstand, PGSQL_USER:PGU|hsh00_vorstand, { "encoding": "LATIN1"}),
                   8000786=HsHostingAssetRawEntity(MARIADB_DATABASE, MAD|hsh00_addr, hsh00_addr, MARIADB_USER:MAU|hsh00, { "encoding": "latin1"}),
                   8000805=HsHostingAssetRawEntity(MARIADB_DATABASE, MAD|hsh00_db2, hsh00_db2, MARIADB_USER:MAU|hsh00, { "encoding": "latin1"}),
                   8001858=HsHostingAssetRawEntity(PGSQL_DATABASE, PGD|hsh00, hsh00, PGSQL_USER:PGU|hsh00, { "encoding": "LATIN1"}),
                   8001860=HsHostingAssetRawEntity(PGSQL_DATABASE, PGD|hsh00_hsadmin, hsh00_hsadmin, PGSQL_USER:PGU|hsh00_hsadmin, { "encoding": "UTF8"}),
                   8004908=HsHostingAssetRawEntity(MARIADB_DATABASE, MAD|hsh00_mantis, hsh00_mantis, MARIADB_USER:MAU|hsh00_mantis, { "encoding": "utf8"}),
                   8004931=HsHostingAssetRawEntity(PGSQL_DATABASE, PGD|hsh00_phpPgSqlAdmin, hsh00_phpPgSqlAdmin, PGSQL_USER:PGU|hsh00_phpPgSqlAdmin, { "encoding": "UTF8"}),
                   8004932=HsHostingAssetRawEntity(PGSQL_DATABASE, PGD|hsh00_phpPgSqlAdmin_new, hsh00_phpPgSqlAdmin_new, PGSQL_USER:PGU|hsh00_phpPgSqlAdmin, { "encoding": "UTF8"}),
                   8004941=HsHostingAssetRawEntity(MARIADB_DATABASE, MAD|hsh00_phpMyAdmin, hsh00_phpMyAdmin, MARIADB_USER:MAU|hsh00_phpMyAdmin, { "encoding": "utf8"}),
                   8004942=HsHostingAssetRawEntity(MARIADB_DATABASE, MAD|hsh00_phpMyAdmin_old, hsh00_phpMyAdmin_old, MARIADB_USER:MAU|hsh00_phpMyAdmin, { "encoding": "utf8"})
                }
                """);
    }

    // --------------------------------------------------------------------------------------------

    @Test
    @Order(18010)
    void validateBookingItems() {
        bookingItems.forEach((id, bi) -> {
            try {
                HsBookingItemEntityValidatorRegistry.validated(bi);
            } catch (final Exception exc) {
                errors.add("validation failed for id:" + id + "( " + bi + "): " + exc.getMessage());
            }
        });
    }

    @Test
    @Order(18020)
    void validateHostingAssets() {
        hostingAssets.forEach((id, ha) -> {
            try {
                new HostingAssetEntitySaveProcessor(em, ha)
                        .preprocessEntity()
                        .validateEntity()
                        .prepareForSave();
            } catch (final Exception exc) {
                errors.add("validation failed for id:" + id + "( " + ha + "): " + exc.getMessage());
            }
        });
    }

    @Test
    @Order(18999)
    @ContinueOnFailure
    void logValidationErrors() {
        this.logErrors();
    }

    // --------------------------------------------------------------------------------------------

    @Test
    @Order(19000)
    @Commit
    void persistBookingProjects() {

        System.out.println("PERSISTING booking-projects to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            bookingProjects.forEach(this::persist);
        }).assertSuccessful();
    }

    @Test
    @Order(19010)
    @Commit
    void persistBookingItems() {

        System.out.println("PERSISTING booking-items to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            bookingItems.forEach(this::persistRecursively);
        }).assertSuccessful();
    }

    @Test
    @Order(19120)
    @Commit
    void persistCloudServers() {

        System.out.println("PERSISTING cloud-servers to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");

        persistHostingAssetsOfType(CLOUD_SERVER);
    }

    @Test
    @Order(19130)
    @Commit
    void persistManagedServers() {
        System.out.println("PERSISTING managed-servers to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(MANAGED_SERVER);
    }

    @Test
    @Order(19140)
    @Commit
    void persistManagedWebspaces() {
        System.out.println("PERSISTING managed-webspaces to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(MANAGED_WEBSPACE);
    }

    @Test
    @Order(19150)
    @Commit
    void persistIPNumbers() {
        System.out.println("PERSISTING ip-numbers to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(IPV4_NUMBER);
    }

    @Test
    @Order(19160)
    @Commit
    void persistUnixUsers() {
        System.out.println("PERSISTING unix-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(UNIX_USER);
    }

    @Test
    @Order(19170)
    @Commit
    void persistEmailAliases() {
        System.out.println("PERSISTING email-aliases to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(EMAIL_ALIAS);
    }

    @Test
    @Order(19200)
    @Commit
    void persistDatabaseInstances() {
        System.out.println("PERSISTING db-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(PGSQL_INSTANCE, MARIADB_INSTANCE);
    }

    @Test
    @Order(19210)
    @Commit
    void persistDatabaseUsers() {
        System.out.println("PERSISTING db-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(PGSQL_USER, MARIADB_USER);
    }

    @Test
    @Order(19220)
    @Commit
    void persistDatabases() {
        System.out.println("PERSISTING databases to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssetsOfType(PGSQL_DATABASE, MARIADB_DATABASE);
    }

    @Test
    @Order(19900)
    void verifyPersistedUnixUsersWithUserId() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEachType(15, UNIX_USER)).isEqualToIgnoringWhitespace("""
                {
                   4005803=HsHostingAssetRawEntity(UNIX_USER, lug00, LUGs, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102090}),
                   4005805=HsHostingAssetRawEntity(UNIX_USER, lug00-wla.1, Paul Klemm, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102091}),
                   4005809=HsHostingAssetRawEntity(UNIX_USER, lug00-wla.2, Walter Müller, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 8, "SSD soft quota": 4, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102093}),
                   4005811=HsHostingAssetRawEntity(UNIX_USER, lug00-ola.a, LUG OLA - POP a, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102094}),
                   4005813=HsHostingAssetRawEntity(UNIX_USER, lug00-ola.b, LUG OLA - POP b, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102095}),
                   4005835=HsHostingAssetRawEntity(UNIX_USER, lug00-test, Test, MANAGED_WEBSPACE:lug00, { "SSD hard quota": 1024, "SSD soft quota": 1024, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102106}),
                   4005964=HsHostingAssetRawEntity(UNIX_USER, mim00, Michael Mellis, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102147}),
                   4005966=HsHostingAssetRawEntity(UNIX_USER, mim00-1981, Jahrgangstreffen 1981, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 256, "SSD soft quota": 128, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102148}),
                   4005990=HsHostingAssetRawEntity(UNIX_USER, mim00-mail, Mailbox, MANAGED_WEBSPACE:mim00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102160}),
                   4100705=HsHostingAssetRawEntity(UNIX_USER, hsh00-mim, Michael Mellis, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/false", "userid": 10003}),
                   4100824=HsHostingAssetRawEntity(UNIX_USER, hsh00, Hostsharing Paket, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 10000}),
                   4167846=HsHostingAssetRawEntity(UNIX_USER, hsh00-dph, hsh00-uph, MANAGED_WEBSPACE:hsh00, { "HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/false", "userid": 110568}),
                   4169546=HsHostingAssetRawEntity(UNIX_USER, dph00, Reinhard Wiese, MANAGED_WEBSPACE:dph00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 110593}),
                   4169596=HsHostingAssetRawEntity(UNIX_USER, dph00-uph, Domain admin, MANAGED_WEBSPACE:dph00, { "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 110594})
                }
                """);
    }

    @Test
    @Order(19910)
    void verifyBookingItemsAreActuallyPersisted() {
        final var biCount = (Integer) em.createNativeQuery("SELECT count(*) FROM hs_booking_item", Integer.class).getSingleResult();
        assertThat(biCount).isGreaterThan(isImportingControlledTestData() ? 5 : 500);
    }

    @Test
    @Order(19920)
    void verifyHostingAssetsAreActuallyPersisted() {
        final var haCount = (Integer) em.createNativeQuery("SELECT count(*) FROM hs_hosting_asset", Integer.class).getSingleResult();
        assertThat(haCount).isGreaterThan(isImportingControlledTestData() ? 30 : 10000);
    }

    // ============================================================================================

    @Test
    @Order(99999)
    void logErrors() {
        if (isImportingControlledTestData()) {
            super.expectErrors("""
                        validation failed for id:5002452( HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-empty, mim00-empty, MANAGED_WEBSPACE:mim00, {
                            "target": "[]"
                        }
                        )): ['EMAIL_ALIAS:mim00-empty.config.target' length is expected to be at min 1 but length of [[]] is 0]""",
                    """
                    validation failed for id:5002453( HsHostingAssetRawEntity(EMAIL_ALIAS, mim00-0_entries, mim00-0_entries, MANAGED_WEBSPACE:mim00, {
                        "target": "[]"
                    }
                    )): ['EMAIL_ALIAS:mim00-0_entries.config.target' length is expected to be at min 1 but length of [[]] is 0]"""
                    );
        } else {
            super.logErrors();
        }
    }

    private void persistRecursively(final Integer key, final HsBookingItemEntity bi) {
        if (bi.getParentItem() != null) {
            persistRecursively(key, HsBookingItemEntityValidatorRegistry.validated(bi.getParentItem()));
        }
        persist(key, HsBookingItemEntityValidatorRegistry.validated(bi));
    }

    // ============================================================================================

    private void persistHostingAssetsOfType(final HsHostingAssetType... hsHostingAssetTypes) {
        final var hsHostingAssetTypeSet = stream(hsHostingAssetTypes).collect(toSet());
        jpaAttempt.transacted(() -> {
            hostingAssets.forEach((key, ha) -> {
                        context(rbacSuperuser);
                        if (hsHostingAssetTypeSet.contains(ha.getType())) {
                            new HostingAssetEntitySaveProcessor(em, ha)
                                    .preprocessEntity()
                                    .validateEntityIgnoring("'EMAIL_ALIAS:.*\\.config\\.target' .*")
                                    .prepareForSave()
                                    .saveUsing(entity -> persist(key, entity))
                                    .validateContext();
                        }
                    }
            );
        }).assertSuccessful();
    }

    private void importIpNumbers(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var ipNumber = HsHostingAssetRawEntity.builder()
                            .type(IPV4_NUMBER)
                            .identifier(rec.getString("inet_addr"))
                            .caption(rec.getString("description"))
                            .build();
                    hostingAssets.put(IP_NUMBER_ID_OFFSET + rec.getInteger("inet_addr_id"), ipNumber);
                });
    }

    private void importHives(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var hive_id = rec.getInteger("hive_id");
                    final var hive = new Hive(
                            hive_id,
                            rec.getString("hive_name"),
                            rec.getInteger("inet_addr_id"),
                            new AtomicReference<>());
                    hives.put(HIVE_ID_OFFSET + hive_id, hive);
                });
    }

    private void importPackets(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var packet_id = rec.getInteger("packet_id");
                    final var basepacket_code = rec.getString("basepacket_code");
                    final var packet_name = rec.getString("packet_name");
                    final var bp_id = rec.getInteger("bp_id");
                    final var hive_id = rec.getInteger("hive_id");
                    final var created = rec.getLocalDate("created");
                    final var cancelled = rec.getLocalDate("cancelled");
                    final var cur_inet_addr_id = rec.getInteger("cur_inet_addr_id");
                    final var old_inet_addr_id = rec.getInteger("old_inet_addr_id");
                    final var free = rec.getBoolean("free");

                    assertThat(old_inet_addr_id)
                            .as("packet.old_inet_addr_id not supported, but is not null for " + packet_name)
                            .isNull();

                    final var biType = determineBiType(basepacket_code);
                    final var bookingItem = HsBookingItemEntity.builder()
                            .type(biType)
                            .caption("BI " + packet_name)
                            .project(bookingProjects.get(bp_id))
                            .validity(toPostgresDateRange(created, cancelled))
                            .build();
                    bookingItems.put(PACKET_ID_OFFSET + packet_id, bookingItem);
                    final var haType = determineHaType(basepacket_code);

                    logError(() -> assertThat(!free || haType == MANAGED_WEBSPACE || bookingItem.getRelatedProject()
                            .getDebitor()
                            .getDefaultPrefix()
                            .equals("hsh"))
                            .as("packet.free only supported for Hostsharing-Assets and ManagedWebspace in customer-ManagedServer, but is set for "
                                    + packet_name)
                            .isTrue());

                    final var asset = HsHostingAssetRawEntity.builder()
                            // this turns off identifier validation to accept former default prefixes
                            .isLoaded(haType == MANAGED_WEBSPACE)
                            .type(haType)
                            .identifier(packet_name)
                            .bookingItem(bookingItem)
                            .caption("HA " + packet_name)
                            .build();
                    hostingAssets.put(PACKET_ID_OFFSET + packet_id, asset);
                    if (haType == MANAGED_SERVER) {
                        hive(hive_id).serverRef.set(asset);
                    }

                    if (cur_inet_addr_id != null) {
                        ipNumber(cur_inet_addr_id).setAssignedToAsset(asset);
                    }
                });

        // once we know all hosting assets, we can set the parentAsset for managed webspaces
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var packet_id = rec.getInteger("packet_id");
                    final var basepacket_code = rec.getString("basepacket_code");
                    final var hive_id = rec.getInteger("hive_id");

                    final var haType = determineHaType(basepacket_code);
                    if (haType == MANAGED_WEBSPACE) {
                        final var managedWebspace = pac(packet_id);
                        final var parentAsset = hive(hive_id).serverRef.get();
                        managedWebspace.setParentAsset(parentAsset);
                        managedWebspace.getBookingItem().setParentItem(parentAsset.getBookingItem());
                    }
                });
    }

    private void importPacketComponents(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    // final var packet_component_id = rec.getInteger("packet_component_id"); not needed
                    final var packet_id = rec.getInteger("packet_id");
                    final var quantity = rec.getInteger("quantity");
                    final var basecomponent_code = rec.getString("basecomponent_code");
                    // final var created = rec.getLocalDate("created"); TODO.spec: can we do without?
                    // final var cancelled = rec.getLocalDate("cancelled"); TODO.spec: can we do without?
                    Function<Integer, Object> convert = (v -> v);

                    final var asset = pac(packet_id);
                    final var name = switch (basecomponent_code) {
                        case "DAEMON" -> "Daemons";
                        case "MULTI" -> "Multi";
                        case "CPU" -> "CPU";
                        case "RAM" -> returning("RAM", convert = v -> v / 1024);
                        case "QUOTA" -> returning("SSD", convert = v -> v / 1024);
                        case "STORAGE" -> returning("HDD", convert = v -> v / 1024);
                        case "TRAFFIC" -> "Traffic";
                        case "OFFICE" -> returning("Online Office Server", convert = v -> v == 1);

                        case "SLABASIC" -> switch (asset.getType()) {
                            case CLOUD_SERVER -> "SLA-Infrastructure";
                            case MANAGED_SERVER -> "SLA-Platform";
                            case MANAGED_WEBSPACE -> "SLA-Platform";
                            default -> throw new IllegalArgumentException("SLABASIC not defined for " + asset.getType());
                        };

                        case "SLAINFR2H" -> "SLA-Infrastructure";
                        case "SLAINFR4H" -> "SLA-Infrastructure";
                        case "SLAINFR8H" -> "SLA-Infrastructure";

                        case "SLAEXT24H" -> "SLA-Platform";

                        case "SLAPLAT2H" -> "SLA-Platform";
                        case "SLAPLAT4H" -> "SLA-Platform";
                        case "SLAPLAT8H" -> "SLA-Platform";

                        case "SLAWEB2H" -> "SLA-Web";
                        case "SLAWEB4H" -> "SLA-Web";
                        case "SLAWEB8H" -> "SLA-Web";

                        case "SLAMAIL2H" -> "SLA-EMail";
                        case "SLAMAIL4H" -> "SLA-EMail";
                        case "SLAMAIL8H" -> "SLA-EMail";

                        case "SLAMARIA2H" -> "SLA-Maria";
                        case "SLAMARIA4H" -> "SLA-Maria";
                        case "SLAMARIA8H" -> "SLA-Maria";

                        case "SLAPGSQL2H" -> "SLA-PgSQL";
                        case "SLAPGSQL4H" -> "SLA-PgSQL";
                        case "SLAPGSQL8H" -> "SLA-PgSQL";

                        case "SLAOFFIC2H" -> "SLA-Office";
                        case "SLAOFFIC4H" -> "SLA-Office";
                        case "SLAOFFIC8H" -> "SLA-Office";

                        case "BANDWIDTH" -> "Bandwidth";
                        default -> throw new IllegalArgumentException("unknown basecomponent_code: " + basecomponent_code);
                    };

                    if (name.equals("SLA-Infrastructure")) {
                        final var slaValue = switch (basecomponent_code) {
                            case "SLABASIC" -> "BASIC";
                            case "SLAINFR2H" -> "EXT2H";
                            case "SLAINFR4H" -> "EXT4H";
                            case "SLAINFR8H" -> "EXT8H";
                            default -> throw new IllegalArgumentException("unknown basecomponent_code: " + basecomponent_code);
                        };
                        asset.getBookingItem().getResources().put(name, slaValue);
                    } else if (name.equals("SLA-Platform")) {
                        final var slaValue = switch (basecomponent_code) {
                            case "SLABASIC" -> "BASIC";
                            case "SLAEXT24H" -> "EXT24H";
                            case "SLAPLAT2H" -> "EXT2H";
                            case "SLAPLAT4H" -> "EXT4H";
                            case "SLAPLAT8H" -> "EXT8H";
                            default -> throw new IllegalArgumentException("unknown basecomponent_code: " + basecomponent_code);
                        };
                        if (ofNullable(asset.getBookingItem().getResources().get(name)).map("BASIC"::equals).orElse(true)) {
                            asset.getBookingItem().getResources().put(name, slaValue);
                        }
                    } else if (name.startsWith("SLA")) {
                        asset.getBookingItem().getResources().put(name, true);
                    } else if (quantity > 0) {
                        asset.getBookingItem().getResources().put(name, convert.apply(quantity));
                    }
                });
    }

    private void importUnixUsers(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var unixuser_id = rec.getInteger("unixuser_id");
                    final var packet_id = rec.getInteger("packet_id");
                    final var unixUserAsset = HsHostingAssetRawEntity.builder()
                            .type(UNIX_USER)
                            .parentAsset(hostingAssets.get(PACKET_ID_OFFSET + packet_id))
                            .identifier(rec.getString("name"))
                            .caption(rec.getString("comment"))
                            .isLoaded(true) // avoid overwriting imported userids with generated ids
                            .config(new HashMap<>(ofEntries(
                                    entry("shell", rec.getString("shell")),
                                    // entry("homedir", rec.getString("homedir")), do not import, it's calculated
                                    entry("locked", rec.getBoolean("locked")),
                                    entry("userid", rec.getInteger("userid")),
                                    entry("SSD soft quota", rec.getInteger("quota_softlimit")),
                                    entry("SSD hard quota", rec.getInteger("quota_hardlimit")),
                                    entry("HDD soft quota", rec.getInteger("storage_softlimit")),
                                    entry("HDD hard quota", rec.getInteger("storage_hardlimit"))
                            )))
                            .build();

                    // TODO.spec: crop SSD+HDD limits if > booked
                    if (unixUserAsset.getDirectValue("SSD hard quota", Integer.class, 0)
                            > 1024*unixUserAsset.getContextValue("SSD", Integer.class, 0)) {
                        unixUserAsset.getConfig().put("SSD hard quota", unixUserAsset.getContextValue("SSD", Integer.class, 0)*1024);
                    }
                    if (unixUserAsset.getDirectValue("HDD hard quota", Integer.class, 0)
                            > 1024*unixUserAsset.getContextValue("HDD", Integer.class, 0)) {
                        unixUserAsset.getConfig().put("HDD hard quota", unixUserAsset.getContextValue("HDD", Integer.class, 0)*1024);
                    }

                    // TODO.spec: does `softlimit<hardlimit?` even make sense? Fix it in this or the other direction?
                    if (unixUserAsset.getDirectValue("SSD soft quota", Integer.class, 0)
                            > unixUserAsset.getDirectValue("SSD hard quota", Integer.class, 0)) {
                        unixUserAsset.getConfig().put("SSD soft quota", unixUserAsset.getConfig().get("SSD hard quota"));
                    }
                    if (unixUserAsset.getDirectValue("HDD soft quota", Integer.class, 0)
                            > unixUserAsset.getDirectValue("HDD hard quota", Integer.class, 0)) {
                        unixUserAsset.getConfig().put("HDD soft quota", unixUserAsset.getConfig().get("HDD hard quota"));
                    }

                    // TODO.spec: remove HDD limits if no HDD storage is booked
                    if (unixUserAsset.getContextValue("HDD", Integer.class, 0) == 0) {
                        unixUserAsset.getConfig().remove("HDD hard quota");
                        unixUserAsset.getConfig().remove("HDD soft quota");
                    }

                    hostingAssets.put(UNIXUSER_ID_OFFSET + unixuser_id, unixUserAsset);
                });
    }

    private void importEmailAliases(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var unixuser_id = rec.getInteger("emailalias_id");
                    final var packet_id = rec.getInteger("pac_id");
                    final var targets = parseCsvLine(rec.getString("target"));
                    final var unixUserAsset = HsHostingAssetRawEntity.builder()
                            .type(EMAIL_ALIAS)
                            .parentAsset(hostingAssets.get(PACKET_ID_OFFSET + packet_id))
                            .identifier(rec.getString("name"))
                            .caption(rec.getString("name"))
                            .config(ofEntries(
                                    entry("target", targets)
                            ))
                            .build();
                    hostingAssets.put(EMAILALIAS_ID_OFFSET + unixuser_id, unixUserAsset);
                });
    }

    private void createDatabaseInstances(final List<HsHostingAssetRawEntity> parentAssets) {
        final var idRef = new AtomicInteger(0);
        parentAssets.forEach(pa -> {
            if (pa.getSubHostingAssets() == null) {
                pa.setSubHostingAssets(new ArrayList<>());
            }

            final var pgSqlInstanceAsset = HsHostingAssetRawEntity.builder()
                    .type(PGSQL_INSTANCE)
                    .parentAsset(pa)
                    .identifier(pa.getIdentifier() + "|PgSql.default")
                    .caption(pa.getIdentifier() + "-PostgreSQL default instance")
                    .build();
            pa.getSubHostingAssets().add(pgSqlInstanceAsset);
            hostingAssets.put(DBINSTANCE_ID_OFFSET + idRef.getAndIncrement(), pgSqlInstanceAsset);

            final var mariaDbInstanceAsset = HsHostingAssetRawEntity.builder()
                    .type(MARIADB_INSTANCE)
                    .parentAsset(pa)
                    .identifier(pa.getIdentifier() + "|MariaDB.default")
                    .caption(pa.getIdentifier() + "-MariaDB default instance")
                    .build();
            pa.getSubHostingAssets().add(mariaDbInstanceAsset);
            hostingAssets.put(DBINSTANCE_ID_OFFSET + idRef.getAndIncrement(), mariaDbInstanceAsset);
        });
    }

    private void importDatabaseUsers(final String[] header, final List<String[]> records) {
        HashGenerator.enableChouldBeHash(true);
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var dbuser_id = rec.getInteger("dbuser_id");
                    final var packet_id = rec.getInteger("packet_id");
                    final var engine = rec.getString("engine");
                    final HsHostingAssetType dbUserAssetType = "mysql".equals(engine) ? MARIADB_USER
                            : "pgsql".equals(engine) ? PGSQL_USER
                            : failWith("unknown DB engine " + engine);
                    final var hash = dbUserAssetType == MARIADB_USER ? Algorithm.MYSQL_NATIVE : Algorithm.SCRAM_SHA256;
                    final var name = rec.getString("name");
                    final var password_hash = rec.getString("password_hash", HashGenerator.using(hash).withRandomSalt().hash("fake pw " + name));

                    final HsHostingAssetType dbInstanceAssetType = "mysql".equals(engine) ? MARIADB_INSTANCE
                            : "pgsql".equals(engine) ? PGSQL_INSTANCE
                            : failWith("unknown DB engine " + engine);
                    final var relatedWebspaceHA = hostingAssets.get(PACKET_ID_OFFSET + packet_id).getParentAsset();
                    final var dbInstanceAsset = relatedWebspaceHA.getSubHostingAssets().stream()
                            .filter(ha -> ha.getType() == dbInstanceAssetType)
                            .findAny().orElseThrow(); // there is exactly one: the default instance for the given type

                    final var dbUserAsset = HsHostingAssetRawEntity.builder()
                            .type(dbUserAssetType)
                            .parentAsset(hostingAssets.get(PACKET_ID_OFFSET + packet_id))
                            .assignedToAsset(dbInstanceAsset)
                            .identifier(dbUserAssetType.name().substring(0, 2) + "U|" + name)
                            .caption(name)
                            .config(new HashMap<>(ofEntries(
                                    entry("password", password_hash)
                            )))
                            .build();
                    dbUsersByEngineAndName.put(engine + ":" + name, dbUserAsset);
                    hostingAssets.put(DBUSER_ID_OFFSET + dbuser_id, dbUserAsset);
                });
    }

    private void importDatabases(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var database_id = rec.getInteger("database_id");
                    final var engine = rec.getString("engine");
                    final var owner = rec.getString("owner");
                    final var owningDbUserHA =  dbUsersByEngineAndName.get(engine + ":" + owner);
                    assertThat(owningDbUserHA).as("owning user for " + (engine + ":" + owner) + " not found").isNotNull();
                    final HsHostingAssetType type = "mysql".equals(engine) ? MARIADB_DATABASE
                            : "pgsql".equals(engine) ? PGSQL_DATABASE
                            : failWith("unknown DB engine " + engine);
                    final var name = rec.getString("name");
                    final var encoding = rec.getString("encoding").replaceAll("[-_]+", "");
                    final var dbAsset = HsHostingAssetRawEntity.builder()
                            .type(type)
                            .parentAsset(owningDbUserHA)
                            .identifier(type.name().substring(0, 2) + "D|" + name)
                            .caption(name)
                            .config(ofEntries(
                                    entry("encoding", type == MARIADB_DATABASE ? encoding.toLowerCase() : encoding.toUpperCase())
                            ))
                            .build();
                    hostingAssets.put(DB_ID_OFFSET + database_id, dbAsset);
                });
    }

    // ============================================================================================

    <V> V returning(
            final V value,
            @SuppressWarnings("unused") final Object... assignments // DSL-hack: just used for side effects on caller-side
    ) {
        return value;
    }

    private static @NotNull HsBookingItemType determineBiType(final String basepacket_code) {
        return switch (basepacket_code) {
            case "SRV/CLD" -> HsBookingItemType.CLOUD_SERVER;
            case "SRV/MGD" -> HsBookingItemType.MANAGED_SERVER;
            case "PAC/WEB" -> HsBookingItemType.MANAGED_WEBSPACE;
            default -> throw new IllegalArgumentException(
                    "unknown basepacket_code: " + basepacket_code);
        };
    }

    private static @NotNull HsHostingAssetType determineHaType(final String basepacket_code) {
        return switch (basepacket_code) {
            case "SRV/CLD" -> CLOUD_SERVER;
            case "SRV/MGD" -> MANAGED_SERVER;
            case "PAC/WEB" -> MANAGED_WEBSPACE;
            default -> throw new IllegalArgumentException(
                    "unknown basepacket_code: " + basepacket_code);
        };
    }

    private static HsHostingAssetRawEntity ipNumber(final Integer inet_addr_id) {
        return inet_addr_id != null ? hostingAssets.get(IP_NUMBER_ID_OFFSET + inet_addr_id) : null;
    }

    private static Hive hive(final Integer hive_id) {
        return hive_id != null ? hives.get(HIVE_ID_OFFSET + hive_id) : null;
    }

    private static HsHostingAssetRawEntity pac(final Integer packet_id) {
        return packet_id != null ? hostingAssets.get(PACKET_ID_OFFSET + packet_id) : null;
    }

    private String firstOfEachType(
            final int maxCount,
            final HsHostingAssetType... types) {
        return toFormattedString(stream(types)
                .flatMap(t ->
                        hostingAssets.entrySet().stream()
                                .filter(hae -> hae.getValue().getType() == t)
                                .limit(maxCount)
                )
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, ImportHostingAssets::uniqueKeys, TreeMap::new)));
    }

    protected static <V> V uniqueKeys(final V v1, final V v2) {
        throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
    }

    private String firstOfEachType(
            final int maxCount,
            final HsBookingItemType... types) {
        return toFormattedString(stream(types)
                .flatMap(t ->
                        bookingItems.entrySet().stream()
                                .filter(bie -> bie.getValue().getType() == t)
                                .limit(maxCount)
                )
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private Map<Integer, Object> first(
            final int maxCount,
            final Map<Integer, ?> entities) {
        return entities.entrySet().stream()
                .limit(maxCount)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    protected void assumeThatWeAreExplicitlyImportingOfficeData() {
        assumeThat(false).isTrue();
    }

    protected static boolean isImportingControlledTestData() {
        return MIGRATION_DATA_PATH.equals(TEST_DATA_MIGRATION_DATA_PATH);
    }

    protected static void assumeThatWeAreImportingControlledTestData() {
        assumeThat(isImportingControlledTestData()).isTrue();
    }
}
