package net.hostsharing.hsadminng.hs.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;

import java.io.Reader;
import java.net.IDN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_DNS_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_HTTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ADDRESS;
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

    private static final Set<String> NOBODY_SUBSTITUTES = Set.of("nomail", "bounce");

    static List<String> zonefileErrors = new ArrayList<>();

    record Hive(int hive_id, String hive_name, int inet_addr_id, AtomicReference<HsHostingAssetRealEntity> serverRef) {}

    static Map<Integer, HsBookingProjectEntity> bookingProjects = new WriteOnceMap<>();
    static Map<Integer, HsBookingItemEntity> bookingItems = new WriteOnceMap<>();
    static Map<Integer, Hive> hives = new WriteOnceMap<>();

    static Map<Integer, HsHostingAssetRealEntity> ipNumberAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> packetAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> unixUserAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> emailAliasAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> dbInstanceAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> dbUserAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> dbAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> domainSetupAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> domainDnsSetupAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> domainHttpSetupAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> domainMBoxSetupAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> domainSmtpSetupAssets = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetRealEntity> emailAddressAssets = new WriteOnceMap<>();

    static Map<String, HsHostingAssetRealEntity> dbUsersByEngineAndName = new WriteOnceMap<>();
    static Map<String, HsHostingAssetRealEntity> domainSetupsByName = new WriteOnceMap<>();

    final ObjectMapper jsonMapper = new ObjectMapper();

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

        assertThat(firstOfEach(5, ipNumberAssets)).isEqualToIgnoringWhitespace("""
                {
                   363=HsHostingAssetRealEntity(IPV4_NUMBER, 83.223.95.34),
                   381=HsHostingAssetRealEntity(IPV4_NUMBER, 83.223.95.52),
                   402=HsHostingAssetRealEntity(IPV4_NUMBER, 83.223.95.73),
                   433=HsHostingAssetRealEntity(IPV4_NUMBER, 83.223.95.104),
                   457=HsHostingAssetRealEntity(IPV4_NUMBER, 83.223.95.128)
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

        assertThat(toJsonFormattedString(first(5, hives))).isEqualToIgnoringWhitespace("""
                {
                   1001=Hive[hive_id=1001, hive_name=h00, inet_addr_id=358, serverRef=null],
                   1002=Hive[hive_id=1002, hive_name=h01, inet_addr_id=359, serverRef=null],
                   1004=Hive[hive_id=1004, hive_name=h02, inet_addr_id=360, serverRef=null],
                   1007=Hive[hive_id=1007, hive_name=h03, inet_addr_id=361, serverRef=null],
                   1013=Hive[hive_id=1013, hive_name=h04, inet_addr_id=430, serverRef=null]
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

        assertThat(firstOfEachType(
                3,
                HsBookingItemType.CLOUD_SERVER,
                HsBookingItemType.MANAGED_SERVER,
                HsBookingItemType.MANAGED_WEBSPACE)).isEqualToIgnoringWhitespace("""
                {
                   10630=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_WEBSPACE, [2001-06-01,), BI hsh00),
                   10968=HsBookingItemEntity(D-1015200:rar default project, MANAGED_SERVER, [2013-04-01,), BI vm1061),
                   10978=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2013-04-01,), BI vm1050),
                   11061=HsBookingItemEntity(D-1000300:mim default project, MANAGED_SERVER, [2013-08-19,), BI vm1068),
                   11094=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-10,), BI lug00),
                   11112=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-17,), BI mim00),
                   23611=HsBookingItemEntity(D-1101800:wws default project, CLOUD_SERVER, [2022-08-10,), BI vm2097)
                }
                """);
        assertThat(firstOfEach(9, packetAssets)).isEqualToIgnoringWhitespace("""
                {
                   10630=HsHostingAssetRealEntity(MANAGED_WEBSPACE, hsh00, HA hsh00, MANAGED_SERVER:vm1050, D-1000000:hsh default project:BI hsh00),
                   10968=HsHostingAssetRealEntity(MANAGED_SERVER, vm1061, HA vm1061, D-1015200:rar default project:BI vm1061),
                   10978=HsHostingAssetRealEntity(MANAGED_SERVER, vm1050, HA vm1050, D-1000000:hsh default project:BI vm1050),
                   11061=HsHostingAssetRealEntity(MANAGED_SERVER, vm1068, HA vm1068, D-1000300:mim default project:BI vm1068),
                   11094=HsHostingAssetRealEntity(MANAGED_WEBSPACE, lug00, HA lug00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI lug00),
                   11112=HsHostingAssetRealEntity(MANAGED_WEBSPACE, mim00, HA mim00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI mim00),
                   11447=HsHostingAssetRealEntity(MANAGED_SERVER, vm1093, HA vm1093, D-1000000:hsh default project:BI vm1093),
                   19959=HsHostingAssetRealEntity(MANAGED_WEBSPACE, dph00, HA dph00, MANAGED_SERVER:vm1093, D-1101900:dph default project:BI dph00),
                   23611=HsHostingAssetRealEntity(CLOUD_SERVER, vm2097, HA vm2097, D-1101800:wws default project:BI vm2097)
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

        assertThat(firstOfEach(7, packetAssets))
                .isEqualToIgnoringWhitespace("""
                        {
                           10630=HsHostingAssetRealEntity(MANAGED_WEBSPACE, hsh00, HA hsh00, MANAGED_SERVER:vm1050, D-1000000:hsh default project:BI hsh00),
                           10968=HsHostingAssetRealEntity(MANAGED_SERVER, vm1061, HA vm1061, D-1015200:rar default project:BI vm1061),
                           10978=HsHostingAssetRealEntity(MANAGED_SERVER, vm1050, HA vm1050, D-1000000:hsh default project:BI vm1050),
                           11061=HsHostingAssetRealEntity(MANAGED_SERVER, vm1068, HA vm1068, D-1000300:mim default project:BI vm1068),
                           11094=HsHostingAssetRealEntity(MANAGED_WEBSPACE, lug00, HA lug00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI lug00),
                           11112=HsHostingAssetRealEntity(MANAGED_WEBSPACE, mim00, HA mim00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI mim00),
                           11447=HsHostingAssetRealEntity(MANAGED_SERVER, vm1093, HA vm1093, D-1000000:hsh default project:BI vm1093)
                        }
                        """);
        assertThat(firstOfEachType(
                5,
                HsBookingItemType.CLOUD_SERVER,
                HsBookingItemType.MANAGED_SERVER,
                HsBookingItemType.MANAGED_WEBSPACE))
                .isEqualToIgnoringWhitespace("""
                        {
                           10630=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_WEBSPACE, [2001-06-01,), BI hsh00, {"HDD": 10, "Multi": 25, "SLA-Platform": "EXT24H", "SSD": 16, "Traffic": 50}),
                           10968=HsBookingItemEntity(D-1015200:rar default project, MANAGED_SERVER, [2013-04-01,), BI vm1061, {"CPU": 6, "HDD": 250, "RAM": 14, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 375, "Traffic": 250}),
                           10978=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2013-04-01,), BI vm1050, {"CPU": 4, "HDD": 250, "RAM": 32, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 150, "Traffic": 250}),
                           11061=HsBookingItemEntity(D-1000300:mim default project, MANAGED_SERVER, [2013-08-19,), BI vm1068, {"CPU": 2, "HDD": 250, "RAM": 4, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT2H", "SLA-Web": true, "Traffic": 250}),
                           11094=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-10,), BI lug00, {"Multi": 5, "SLA-Platform": "EXT24H", "SSD": 1, "Traffic": 10}),
                           11112=HsBookingItemEntity(D-1000300:mim default project, MANAGED_WEBSPACE, [2013-09-17,), BI mim00, {"Multi": 5, "SLA-Platform": "EXT24H", "SSD": 3, "Traffic": 20}),
                           11447=HsBookingItemEntity(D-1000000:hsh default project, MANAGED_SERVER, [2014-11-28,), BI vm1093, {"CPU": 6, "HDD": 500, "RAM": 16, "SLA-EMail": true, "SLA-Maria": true, "SLA-Office": true, "SLA-PgSQL": true, "SLA-Platform": "EXT4H", "SLA-Web": true, "SSD": 300, "Traffic": 250}),
                           19959=HsBookingItemEntity(D-1101900:dph default project, MANAGED_WEBSPACE, [2021-06-02,), BI dph00, {"Multi": 1, "SLA-Platform": "EXT24H", "SSD": 25, "Traffic": 20}),
                           23611=HsBookingItemEntity(D-1101800:wws default project, CLOUD_SERVER, [2022-08-10,), BI vm2097, {"CPU": 8, "RAM": 12, "SLA-Infrastructure": "EXT4H", "SSD": 25, "Traffic": 250})
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

        assertThat(firstOfEach(15, unixUserAssets)).isEqualToIgnoringWhitespace("""
                {
                   5803=HsHostingAssetRealEntity(UNIX_USER, lug00, LUGs, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102090}),
                   5805=HsHostingAssetRealEntity(UNIX_USER, lug00-wla.1, Paul Klemm, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102091}),
                   5809=HsHostingAssetRealEntity(UNIX_USER, lug00-wla.2, Walter Müller, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 8, "SSD soft quota": 4, "locked": false, "shell": "/bin/bash", "userid": 102093}),
                   5811=HsHostingAssetRealEntity(UNIX_USER, lug00-ola.a, LUG OLA - POP a, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/usr/bin/passwd", "userid": 102094}),
                   5813=HsHostingAssetRealEntity(UNIX_USER, lug00-ola.b, LUG OLA - POP b, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/usr/bin/passwd", "userid": 102095}),
                   5835=HsHostingAssetRealEntity(UNIX_USER, lug00-test, Test, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 1024, "SSD soft quota": 1024, "locked": false, "shell": "/usr/bin/passwd", "userid": 102106}),
                   5964=HsHostingAssetRealEntity(UNIX_USER, mim00, Michael Mellis, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102147}),
                   5966=HsHostingAssetRealEntity(UNIX_USER, mim00-1981, Jahrgangstreffen 1981, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 256, "SSD soft quota": 128, "locked": false, "shell": "/bin/bash", "userid": 102148}),
                   5990=HsHostingAssetRealEntity(UNIX_USER, mim00-mail, Mailbox, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 102160}),
                   6705=HsHostingAssetRealEntity(UNIX_USER, hsh00-mim, Michael Mellis, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/false", "userid": 10003}),
                   6824=HsHostingAssetRealEntity(UNIX_USER, hsh00, Hostsharing Paket, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 10000}),
                   7846=HsHostingAssetRealEntity(UNIX_USER, hsh00-dph, hsh00-uph, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/false", "userid": 110568}),
                   9546=HsHostingAssetRealEntity(UNIX_USER, dph00, Reinhard Wiese, MANAGED_WEBSPACE:dph00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 110593}),
                   9596=HsHostingAssetRealEntity(UNIX_USER, dph00-dph, Domain admin, MANAGED_WEBSPACE:dph00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "shell": "/bin/bash", "userid": 110594})
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

        assertThat(firstOfEach(15, emailAliasAssets)).isEqualToIgnoringWhitespace("""
                {
                   2403=HsHostingAssetRealEntity(EMAIL_ALIAS, lug00, lug00, MANAGED_WEBSPACE:lug00, {"target": [ "michael.mellis@example.com" ]}),
                   2405=HsHostingAssetRealEntity(EMAIL_ALIAS, lug00-wla-listar, lug00-wla-listar, MANAGED_WEBSPACE:lug00, {"target": [ "|/home/pacs/lug00/users/in/mailinglist/listar" ]}),
                   2429=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00, mim00, MANAGED_WEBSPACE:mim00, {"target": [ "mim12-mi@mim12.hostsharing.net" ]}),
                   2431=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-abruf, mim00-abruf, MANAGED_WEBSPACE:mim00, {"target": [ "michael.mellis@hostsharing.net" ]}),
                   2449=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-hhfx, mim00-hhfx, MANAGED_WEBSPACE:mim00, {"target": [ "mim00-hhfx", "|/usr/bin/formail -I 'Reply-To: hamburger-fx@example.net' | /usr/lib/sendmail mim00-hhfx-l" ]}),
                   2451=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-hhfx-l, mim00-hhfx-l, MANAGED_WEBSPACE:mim00, {"target": [ ":include:/home/pacs/mim00/etc/hhfx.list" ]}),
                   2454=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-dev.null, mim00-dev.null, MANAGED_WEBSPACE:mim00, {"target": [ "/dev/null" ]}),
                   2455=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-1_with_space, mim00-1_with_space, MANAGED_WEBSPACE:mim00, {"target": [ "|/home/pacs/mim00/install/corpslistar/listar" ]}),
                   2456=HsHostingAssetRealEntity(EMAIL_ALIAS, mim00-1_with_single_quotes, mim00-1_with_single_quotes, MANAGED_WEBSPACE:mim00, {"target": [ "|/home/pacs/rir00/mailinglist/ecartis -r kybs06-intern" ]})
                }
                """);
    }

    @Test
    @Order(15000)
    void createDatabaseInstances() {
        createDatabaseInstances(packetAssets.values().stream().filter(ha -> ha.getType() == MANAGED_SERVER).toList());
    }

    @Test
    @Order(15009)
    void verifyDatabaseInstances() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEach(8, dbInstanceAssets)).isEqualToIgnoringWhitespace("""
                {
                   0=HsHostingAssetRealEntity(PGSQL_INSTANCE, vm1061|PgSql.default, vm1061-PostgreSQL default instance, MANAGED_SERVER:vm1061),
                   1=HsHostingAssetRealEntity(MARIADB_INSTANCE, vm1061|MariaDB.default, vm1061-MariaDB default instance, MANAGED_SERVER:vm1061),
                   2=HsHostingAssetRealEntity(PGSQL_INSTANCE, vm1050|PgSql.default, vm1050-PostgreSQL default instance, MANAGED_SERVER:vm1050),
                   3=HsHostingAssetRealEntity(MARIADB_INSTANCE, vm1050|MariaDB.default, vm1050-MariaDB default instance, MANAGED_SERVER:vm1050),
                   4=HsHostingAssetRealEntity(PGSQL_INSTANCE, vm1068|PgSql.default, vm1068-PostgreSQL default instance, MANAGED_SERVER:vm1068),
                   5=HsHostingAssetRealEntity(MARIADB_INSTANCE, vm1068|MariaDB.default, vm1068-MariaDB default instance, MANAGED_SERVER:vm1068),
                   6=HsHostingAssetRealEntity(PGSQL_INSTANCE, vm1093|PgSql.default, vm1093-PostgreSQL default instance, MANAGED_SERVER:vm1093),
                   7=HsHostingAssetRealEntity(MARIADB_INSTANCE, vm1093|MariaDB.default, vm1093-MariaDB default instance, MANAGED_SERVER:vm1093)
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

        assertThat(firstOfEach(10, dbUserAssets)).isEqualToIgnoringWhitespace("""
                {
                   1857=HsHostingAssetRealEntity(PGSQL_USER, PGU|hsh00, hsh00, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$JDiZmaxU+O+ByArLY/CkYZ8HbOk0r/I8LyABnno5gQs=:NI3T500/63dzI1B07Jh3UtQGlukS6JxuS0XoxM/QgAc="}),
                   1858=HsHostingAssetRealEntity(MARIADB_USER, MAU|hsh00, hsh00, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*59067A36BA197AD0A47D74909296C5B002A0FB9F"}),
                   1859=HsHostingAssetRealEntity(PGSQL_USER, PGU|hsh00_vorstand, hsh00_vorstand, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$54Wh+OGx/GaIvAia+I3k78jHGhqmYwe4+iLssmH5zhk=:D4Gq1z2Li2BVSaZrz1azDrs6pwsIzhq4+suK1Hh6ZIg="}),
                   1860=HsHostingAssetRealEntity(PGSQL_USER, PGU|hsh00_hsadmin, hsh00_hsadmin, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$54Wh+OGx/GaIvAia+I3k78jHGhqmYwe4+iLssmH5zhk=:D4Gq1z2Li2BVSaZrz1azDrs6pwsIzhq4+suK1Hh6ZIg="}),
                   1861=HsHostingAssetRealEntity(PGSQL_USER, PGU|hsh00_hsadmin_ro, hsh00_hsadmin_ro, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$UhJnJJhmKANbcaG+izWK3rz5bmhhluSuiCJFlUmDVI8=:6AC4mbLfJGiGlEOWhpz9BivvMODhLLHOnRnnktJPgn8="}),
                   4908=HsHostingAssetRealEntity(MARIADB_USER, MAU|hsh00_mantis, hsh00_mantis, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*EA4C0889A22AAE66BBEBC88161E8CF862D73B44F"}),
                   4909=HsHostingAssetRealEntity(MARIADB_USER, MAU|hsh00_mantis_ro, hsh00_mantis_ro, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*B3BB6D0DA2EC01958616E9B3BCD2926FE8C38383"}),
                   4931=HsHostingAssetRealEntity(PGSQL_USER, PGU|hsh00_phpPgSqlAdmin, hsh00_phpPgSqlAdmin, MANAGED_WEBSPACE:hsh00, PGSQL_INSTANCE:vm1050|PgSql.default, { "password": "SCRAM-SHA-256$4096:Zml4ZWQgc2FsdA==$UhJnJJhmKANbcaG+izWK3rz5bmhhluSuiCJFlUmDVI8=:6AC4mbLfJGiGlEOWhpz9BivvMODhLLHOnRnnktJPgn8="}),
                   4932=HsHostingAssetRealEntity(MARIADB_USER, MAU|hsh00_phpMyAdmin, hsh00_phpMyAdmin, MANAGED_WEBSPACE:hsh00, MARIADB_INSTANCE:vm1050|MariaDB.default, { "password": "*3188720B1889EF5447C722629765F296F40257C2"}),
                   7520=HsHostingAssetRealEntity(MARIADB_USER, MAU|lug00_wla, lug00_wla, MANAGED_WEBSPACE:lug00, MARIADB_INSTANCE:vm1068|MariaDB.default, { "password": "*11667C0EAC42BF8B0295ABEDC7D2868A835E4DB5"})
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

        assertThat(firstOfEach(10, dbAssets)).isEqualToIgnoringWhitespace("""
                {
                   1077=HsHostingAssetRealEntity(PGSQL_DATABASE, PGD|hsh00_vorstand, hsh00_vorstand, PGSQL_USER:PGU|hsh00_vorstand, {"encoding": "LATIN1"}),
                   1786=HsHostingAssetRealEntity(MARIADB_DATABASE, MAD|hsh00_addr, hsh00_addr, MARIADB_USER:MAU|hsh00, {"encoding": "latin1"}),
                   1805=HsHostingAssetRealEntity(MARIADB_DATABASE, MAD|hsh00_dba, hsh00_dba, MARIADB_USER:MAU|hsh00, {"encoding": "latin1"}),
                   1858=HsHostingAssetRealEntity(PGSQL_DATABASE, PGD|hsh00, hsh00, PGSQL_USER:PGU|hsh00, {"encoding": "LATIN1"}),
                   1860=HsHostingAssetRealEntity(PGSQL_DATABASE, PGD|hsh00_hsadmin, hsh00_hsadmin, PGSQL_USER:PGU|hsh00_hsadmin, {"encoding": "UTF8"}),
                   4908=HsHostingAssetRealEntity(MARIADB_DATABASE, MAD|hsh00_mantis, hsh00_mantis, MARIADB_USER:MAU|hsh00_mantis, {"encoding": "utf8"}),
                   4931=HsHostingAssetRealEntity(PGSQL_DATABASE, PGD|hsh00_phpPgSqlAdmin, hsh00_phpPgSqlAdmin, PGSQL_USER:PGU|hsh00_phpPgSqlAdmin, {"encoding": "UTF8"}),
                   4932=HsHostingAssetRealEntity(PGSQL_DATABASE, PGD|hsh00_phpPgSqlAdmin_new, hsh00_phpPgSqlAdmin_new, PGSQL_USER:PGU|hsh00_phpPgSqlAdmin, {"encoding": "UTF8"}),
                   4941=HsHostingAssetRealEntity(MARIADB_DATABASE, MAD|hsh00_phpMyAdmin, hsh00_phpMyAdmin, MARIADB_USER:MAU|hsh00_phpMyAdmin, {"encoding": "utf8"}),
                   4942=HsHostingAssetRealEntity(MARIADB_DATABASE, MAD|hsh00_phpMyAdmin_old, hsh00_phpMyAdmin_old, MARIADB_USER:MAU|hsh00_phpMyAdmin, {"encoding": "utf8"})
                }
                """);
    }

    @Test
    @Order(16010)
    void importDomains() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/domain.csv")) {
            final var lines = readAllLines(reader);
            importDomains(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(16020)
    void importZonenfiles() {
        final var reflections = new Reflections(MIGRATION_DATA_PATH + "/hosting/zonefiles", new ResourcesScanner());
        final var zonefileFiles = reflections.getResources(Pattern.compile(".*\\.json")).stream().sorted().toList();
        zonefileFiles.forEach(zonenfileName -> {
            System.out.println("Processing zonenfile: " + zonenfileName);
            importZonefiles(vmName(zonenfileName), resourceAsString(zonenfileName));
        });
    }

    @Test
    @Order(16029)
    void verifyDomains() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEach(12, domainSetupAssets)).isEqualToIgnoringWhitespace("""
                {
                    4531=HsHostingAssetRealEntity(DOMAIN_SETUP, l-u-g.org, l-u-g.org),
                    4532=HsHostingAssetRealEntity(DOMAIN_SETUP, linuxfanboysngirls.de, linuxfanboysngirls.de),
                    4534=HsHostingAssetRealEntity(DOMAIN_SETUP, lug-mars.de, lug-mars.de),
                    4581=HsHostingAssetRealEntity(DOMAIN_SETUP, 1981.ist-im-netz.de, 1981.ist-im-netz.de, DOMAIN_SETUP:ist-im-netz.de),
                    4587=HsHostingAssetRealEntity(DOMAIN_SETUP, mellis.de, mellis.de),
                    4589=HsHostingAssetRealEntity(DOMAIN_SETUP, ist-im-netz.de, ist-im-netz.de),
                    4600=HsHostingAssetRealEntity(DOMAIN_SETUP, waera.de, waera.de),
                    4604=HsHostingAssetRealEntity(DOMAIN_SETUP, xn--wra-qla.de, wära.de),
                    7662=HsHostingAssetRealEntity(DOMAIN_SETUP, dph-netzwerk.de, dph-netzwerk.de)
                }
                """);

        assertThat(firstOfEach(12, domainDnsSetupAssets)).isEqualToIgnoringWhitespace("""
                {
                   4531=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, l-u-g.org|DNS, DNS-Setup für l-u-g.org, DOMAIN_SETUP:l-u-g.org, MANAGED_WEBSPACE:lug00, {"TTL": 21600, "auto-A-RR": true, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": true, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   4532=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, linuxfanboysngirls.de|DNS, DNS-Setup für linuxfanboysngirls.de, DOMAIN_SETUP:linuxfanboysngirls.de, MANAGED_WEBSPACE:lug00, {"TTL": 21600, "auto-A-RR": true, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": true, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   4534=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, lug-mars.de|DNS, DNS-Setup für lug-mars.de, DOMAIN_SETUP:lug-mars.de, MANAGED_WEBSPACE:lug00, {"TTL": 14400, "auto-A-RR": true, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": false, "auto-NS-RR": true, "auto-SOA": false, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": false, "auto-WILDCARD-SPF-RR": false, "user-RR": [ "lug-mars.de. 14400 IN SOA dns1.hostsharing.net. hostmaster.hostsharing.net. 1611590905 10800 3600 604800 3600", "lug-mars.de. 14400 IN MX 10 mailin1.hostsharing.net.", "lug-mars.de. 14400 IN MX 20 mailin2.hostsharing.net.", "lug-mars.de. 14400 IN MX 30 mailin3.hostsharing.net.", "bbb.lug-mars.de. 14400 IN A 83.223.79.72", "ftp.lug-mars.de. 14400 IN A 83.223.79.72", "www.lug-mars.de. 14400 IN A 83.223.79.72" ]}),
                   4581=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, 1981.ist-im-netz.de|DNS, DNS-Setup für 1981.ist-im-netz.de, DOMAIN_SETUP:1981.ist-im-netz.de, MANAGED_WEBSPACE:mim00, {"TTL": 21600, "auto-A-RR": true, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": true, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   4587=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, mellis.de|DNS, DNS-Setup für mellis.de, DOMAIN_SETUP:mellis.de, MANAGED_WEBSPACE:mim00, {"TTL": 21600, "auto-A-RR": true, "auto-AAAA-RR": true, "auto-AUTOCONFIG-RR": true, "auto-AUTODISCOVER-RR": true, "auto-DKIM-RR": true, "auto-MAILSERVICES-RR": true, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": true, "auto-WILDCARD-MX-RR": true, "auto-WILDCARD-SPF-RR": true, "user-RR": [ "dump.hoennig.de. 21600 IN CNAME mih12.hostsharing.net.", "fotos.hoennig.de. 21600 IN CNAME mih12.hostsharing.net.", "maven.hoennig.de. 21600 IN NS dns1.hostsharing.net." ]}),
                   4589=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, ist-im-netz.de|DNS, DNS-Setup für ist-im-netz.de, DOMAIN_SETUP:ist-im-netz.de, MANAGED_WEBSPACE:mim00, {"TTL": 700, "auto-A-RR": true, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": false, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   4600=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, waera.de|DNS, DNS-Setup für waera.de, DOMAIN_SETUP:waera.de, MANAGED_WEBSPACE:mim00, {"TTL": 21600, "auto-A-RR": false, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": false, "auto-NS-RR": false, "auto-SOA": false, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": false, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": false, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   4604=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, xn--wra-qla.de|DNS, DNS-Setup für wära.de, DOMAIN_SETUP:xn--wra-qla.de, MANAGED_WEBSPACE:mim00, {"TTL": 21600, "auto-A-RR": false, "auto-AAAA-RR": false, "auto-AUTOCONFIG-RR": false, "auto-AUTODISCOVER-RR": false, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": false, "auto-MX-RR": false, "auto-NS-RR": false, "auto-SOA": false, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": false, "auto-WILDCARD-AAAA-RR": false, "auto-WILDCARD-MX-RR": false, "auto-WILDCARD-SPF-RR": false, "user-RR": [ ]}),
                   7662=HsHostingAssetRealEntity(DOMAIN_DNS_SETUP, dph-netzwerk.de|DNS, DNS-Setup für dph-netzwerk.de, DOMAIN_SETUP:dph-netzwerk.de, MANAGED_WEBSPACE:dph00, {"TTL": 21600, "auto-A-RR": true, "auto-AAAA-RR": true, "auto-AUTOCONFIG-RR": true, "auto-AUTODISCOVER-RR": true, "auto-DKIM-RR": false, "auto-MAILSERVICES-RR": true, "auto-MX-RR": true, "auto-NS-RR": true, "auto-SOA": true, "auto-SPF-RR": false, "auto-WILDCARD-A-RR": true, "auto-WILDCARD-AAAA-RR": true, "auto-WILDCARD-MX-RR": true, "auto-WILDCARD-SPF-RR": false, "user-RR": [ "dph-netzwerk.de. 21600 IN TXT \\"v=spf1 include:spf.hostsharing.net ?all\\"", "*.dph-netzwerk.de. 21600 IN TXT \\"v=spf1 include:spf.hostsharing.net ?all\\"" ]})
                }
                """);

        assertThat(firstOfEach(12, domainHttpSetupAssets)).isEqualToIgnoringWhitespace("""
                {
                   4531=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, l-u-g.org|HTTP, HTTP-Setup für l-u-g.org, DOMAIN_SETUP:l-u-g.org, UNIX_USER:lug00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": false, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   4532=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, linuxfanboysngirls.de|HTTP, HTTP-Setup für linuxfanboysngirls.de, DOMAIN_SETUP:linuxfanboysngirls.de, UNIX_USER:lug00-wla.2, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": false, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   4534=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, lug-mars.de|HTTP, HTTP-Setup für lug-mars.de, DOMAIN_SETUP:lug-mars.de, UNIX_USER:lug00-wla.2, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": true, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "www" ]}),
                   4581=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, 1981.ist-im-netz.de|HTTP, HTTP-Setup für 1981.ist-im-netz.de, DOMAIN_SETUP:1981.ist-im-netz.de, UNIX_USER:mim00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": false, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   4587=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, mellis.de|HTTP, HTTP-Setup für mellis.de, DOMAIN_SETUP:mellis.de, UNIX_USER:mim00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": false, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": true, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "www", "michael", "test", "photos", "static", "input" ]}),
                   4589=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, ist-im-netz.de|HTTP, HTTP-Setup für ist-im-netz.de, DOMAIN_SETUP:ist-im-netz.de, UNIX_USER:mim00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": false, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": true, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   4600=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, waera.de|HTTP, HTTP-Setup für waera.de, DOMAIN_SETUP:waera.de, UNIX_USER:mim00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": false, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   4604=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, xn--wra-qla.de|HTTP, HTTP-Setup für wära.de, DOMAIN_SETUP:xn--wra-qla.de, UNIX_USER:mim00, {"autoconfig": false, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": false, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]}),
                   7662=HsHostingAssetRealEntity(DOMAIN_HTTP_SETUP, dph-netzwerk.de|HTTP, HTTP-Setup für dph-netzwerk.de, DOMAIN_SETUP:dph-netzwerk.de, UNIX_USER:dph00-dph, {"autoconfig": true, "cgi": true, "fastcgi": true, "fcgi-php-bin": "/usr/lib/cgi-bin/php", "greylisting": true, "htdocsfallback": true, "includes": true, "indexes": true, "letsencrypt": true, "multiviews": true, "passenger": true, "passenger-errorpage": false, "passenger-nodejs": "/usr/bin/node", "passenger-python": "/usr/bin/python3", "passenger-ruby": "/usr/bin/ruby", "subdomains": [ "*" ]})
                }
                """);

        assertThat(firstOfEach(12, domainMBoxSetupAssets)).isEqualToIgnoringWhitespace("""
                {
                   4531=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, l-u-g.org|MBOX, E-Mail-Empfang-Setup für l-u-g.org, DOMAIN_SETUP:l-u-g.org, MANAGED_WEBSPACE:lug00),
                   4532=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, linuxfanboysngirls.de|MBOX, E-Mail-Empfang-Setup für linuxfanboysngirls.de, DOMAIN_SETUP:linuxfanboysngirls.de, MANAGED_WEBSPACE:lug00),
                   4534=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, lug-mars.de|MBOX, E-Mail-Empfang-Setup für lug-mars.de, DOMAIN_SETUP:lug-mars.de, MANAGED_WEBSPACE:lug00),
                   4581=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, 1981.ist-im-netz.de|MBOX, E-Mail-Empfang-Setup für 1981.ist-im-netz.de, DOMAIN_SETUP:1981.ist-im-netz.de, MANAGED_WEBSPACE:mim00),
                   4587=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, mellis.de|MBOX, E-Mail-Empfang-Setup für mellis.de, DOMAIN_SETUP:mellis.de, MANAGED_WEBSPACE:mim00),
                   4589=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, ist-im-netz.de|MBOX, E-Mail-Empfang-Setup für ist-im-netz.de, DOMAIN_SETUP:ist-im-netz.de, MANAGED_WEBSPACE:mim00),
                   4600=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, waera.de|MBOX, E-Mail-Empfang-Setup für waera.de, DOMAIN_SETUP:waera.de, MANAGED_WEBSPACE:mim00),
                   4604=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, xn--wra-qla.de|MBOX, E-Mail-Empfang-Setup für wära.de, DOMAIN_SETUP:xn--wra-qla.de, MANAGED_WEBSPACE:mim00),
                   7662=HsHostingAssetRealEntity(DOMAIN_MBOX_SETUP, dph-netzwerk.de|MBOX, E-Mail-Empfang-Setup für dph-netzwerk.de, DOMAIN_SETUP:dph-netzwerk.de, MANAGED_WEBSPACE:dph00)
                }
                """);

        assertThat(firstOfEach(12, domainSmtpSetupAssets)).isEqualToIgnoringWhitespace("""
                {
                   4531=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, l-u-g.org|SMTP, E-Mail-Versand-Setup für l-u-g.org, DOMAIN_SETUP:l-u-g.org, MANAGED_WEBSPACE:lug00),
                   4532=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, linuxfanboysngirls.de|SMTP, E-Mail-Versand-Setup für linuxfanboysngirls.de, DOMAIN_SETUP:linuxfanboysngirls.de, MANAGED_WEBSPACE:lug00),
                   4534=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, lug-mars.de|SMTP, E-Mail-Versand-Setup für lug-mars.de, DOMAIN_SETUP:lug-mars.de, MANAGED_WEBSPACE:lug00),
                   4581=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, 1981.ist-im-netz.de|SMTP, E-Mail-Versand-Setup für 1981.ist-im-netz.de, DOMAIN_SETUP:1981.ist-im-netz.de, MANAGED_WEBSPACE:mim00),
                   4587=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, mellis.de|SMTP, E-Mail-Versand-Setup für mellis.de, DOMAIN_SETUP:mellis.de, MANAGED_WEBSPACE:mim00),
                   4589=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, ist-im-netz.de|SMTP, E-Mail-Versand-Setup für ist-im-netz.de, DOMAIN_SETUP:ist-im-netz.de, MANAGED_WEBSPACE:mim00),
                   4600=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, waera.de|SMTP, E-Mail-Versand-Setup für waera.de, DOMAIN_SETUP:waera.de, MANAGED_WEBSPACE:mim00),
                   4604=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, xn--wra-qla.de|SMTP, E-Mail-Versand-Setup für wära.de, DOMAIN_SETUP:xn--wra-qla.de, MANAGED_WEBSPACE:mim00),
                   7662=HsHostingAssetRealEntity(DOMAIN_SMTP_SETUP, dph-netzwerk.de|SMTP, E-Mail-Versand-Setup für dph-netzwerk.de, DOMAIN_SETUP:dph-netzwerk.de, MANAGED_WEBSPACE:dph00)
                }
                """);
    }

    @Test
    @Order(17010)
    void importEmailAddresses() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/hosting/emailaddr.csv")) {
            final var lines = readAllLines(reader);
            importEmailAddresses(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(17029)
    void verifyEmailAddresses() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEach(12, emailAddressAssets)).isEqualToIgnoringWhitespace("""
                {
                   54745=HsHostingAssetRealEntity(EMAIL_ADDRESS, lugmaster@l-u-g.org, lugmaster@l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "lugmaster", "target": [ "nobody" ]}),
                   54746=HsHostingAssetRealEntity(EMAIL_ADDRESS, abuse@l-u-g.org, abuse@l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "abuse", "target": [ "lug00" ]}),
                   54747=HsHostingAssetRealEntity(EMAIL_ADDRESS, postmaster@l-u-g.org, postmaster@l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "postmaster", "target": [ "nobody" ]}),
                   54748=HsHostingAssetRealEntity(EMAIL_ADDRESS, webmaster@l-u-g.org, webmaster@l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "webmaster", "target": [ "nobody" ]}),
                   54749=HsHostingAssetRealEntity(EMAIL_ADDRESS, abuse@linuxfanboysngirls.de, abuse@linuxfanboysngirls.de, DOMAIN_MBOX_SETUP:linuxfanboysngirls.de|MBOX, {"local-part": "abuse", "target": [ "lug00-mars" ]}),
                   54750=HsHostingAssetRealEntity(EMAIL_ADDRESS, postmaster@linuxfanboysngirls.de, postmaster@linuxfanboysngirls.de, DOMAIN_MBOX_SETUP:linuxfanboysngirls.de|MBOX, {"local-part": "postmaster", "target": [ "m.hinsel@example.org" ]}),
                   54751=HsHostingAssetRealEntity(EMAIL_ADDRESS, webmaster@linuxfanboysngirls.de, webmaster@linuxfanboysngirls.de, DOMAIN_MBOX_SETUP:linuxfanboysngirls.de|MBOX, {"local-part": "webmaster", "target": [ "m.hinsel@example.org" ]}),
                   54755=HsHostingAssetRealEntity(EMAIL_ADDRESS, abuse@lug-mars.de, abuse@lug-mars.de, DOMAIN_MBOX_SETUP:lug-mars.de|MBOX, {"local-part": "abuse", "target": [ "lug00-marl" ]}),
                   54756=HsHostingAssetRealEntity(EMAIL_ADDRESS, postmaster@lug-mars.de, postmaster@lug-mars.de, DOMAIN_MBOX_SETUP:lug-mars.de|MBOX, {"local-part": "postmaster", "target": [ "m.hinsel@example.org" ]}),
                   54757=HsHostingAssetRealEntity(EMAIL_ADDRESS, webmaster@lug-mars.de, webmaster@lug-mars.de, DOMAIN_MBOX_SETUP:lug-mars.de|MBOX, {"local-part": "webmaster", "target": [ "m.hinsel@example.org" ]}),
                   54760=HsHostingAssetRealEntity(EMAIL_ADDRESS, info@hamburg-west.l-u-g.org, info@hamburg-west.l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "info", "sub-domain": "hamburg-west", "target": [ "peter.lottmann@example.com" ]}),
                   54761=HsHostingAssetRealEntity(EMAIL_ADDRESS, lugmaster@hamburg-west.l-u-g.org, lugmaster@hamburg-west.l-u-g.org, DOMAIN_MBOX_SETUP:l-u-g.org|MBOX, {"local-part": "lugmaster", "sub-domain": "hamburg-west", "target": [ "raoul.lottmann@example.com" ]})
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
    void validateIpNumberAssets() {
        validateHostingAssets(ipNumberAssets);
    }

    @Test
    @Order(18021)
    void validateServerAndWebspaceAssets() {
        validateHostingAssets(packetAssets);
    }

    @Test
    @Order(18022)
    void validateUnixUserAssets() {
        validateHostingAssets(unixUserAssets);
    }

    @Test
    @Order(18023)
    void validateEmailAliasAssets() {
        validateHostingAssets(emailAliasAssets);
    }

    @Test
    @Order(18030)
    void validateDbInstanceAssets() {
        validateHostingAssets(dbInstanceAssets);
    }

    @Test
    @Order(18031)
    void validateDbUserAssets() {
        validateHostingAssets(dbUserAssets);
    }
    
    @Test
    @Order(18032)
    void validateDbAssets() {
        validateHostingAssets(dbAssets);
    }

    @Test
    @Order(18040)
    void validateDomainSetupAssets() {
        validateHostingAssets(domainSetupAssets);
    }

    @Test
    @Order(18041)
    void validateDomainDnsSetupAssets() {
        validateHostingAssets(domainDnsSetupAssets);
    }

    @Test
    @Order(18042)
    void validateDomainHttpSetupAssets() {
        validateHostingAssets(domainHttpSetupAssets);
    }

    @Test
    @Order(18043)
    void validateDomainSmtpSetupAssets() {
        validateHostingAssets(domainSmtpSetupAssets);
    }

    @Test
    @Order(18044)
    void validateDomainMBoxSetupAssets() {
        validateHostingAssets(domainMBoxSetupAssets);
    }

    @Test
    @Order(18050)
    void validateEmailAddressAssets() {
        validateHostingAssets(emailAddressAssets);
    }

    void validateHostingAssets(final Map<Integer, HsHostingAssetRealEntity> assets) {
        assets.forEach((id, ha) -> {
            logError(() ->
                new HostingAssetEntitySaveProcessor(em, ha)
                        .preprocessEntity()
                        .validateEntity()
                        .prepareForSave()
            );
        });
    }

    @Test
    @Order(18999)
    @ContinueOnFailure
    void logValidationErrors() {
        if (isImportingControlledTestData()) {
            expectError("zonedata dom_owner of mellis.de is old00 but expected to be mim00");
            expectError("\nexpected: \"vm1068\"\n but was: \"vm1093\"");
            expectError("['EMAIL_ADDRESS:webmaster@hamburg-west.l-u-g.org.config.target' is expected to match any of [^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\.+_-]*)?$, ^([a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+)?@[a-zA-Z0-9.-]+$, ^nobody$, ^/dev/null$] but 'raoul.lottmann@example.com peter.lottmann@example.com' does not match any]");
            expectError("['EMAIL_ADDRESS:abuse@mellis.de.config.target' length is expected to be at min 1 but length of [[]] is 0]");
            expectError("['EMAIL_ADDRESS:abuse@ist-im-netz.de.config.target' length is expected to be at min 1 but length of [[]] is 0]");
        }
        this.assertNoErrors();
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

        persistHostingAssets(packetAssets, CLOUD_SERVER);
    }

    @Test
    @Order(19130)
    @Commit
    void persistManagedServers() {
        System.out.println("PERSISTING managed-servers to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(packetAssets, MANAGED_SERVER);
    }

    @Test
    @Order(19140)
    @Commit
    void persistManagedWebspaces() {
        System.out.println("PERSISTING managed-webspaces to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(packetAssets, MANAGED_WEBSPACE);
    }

    @Test
    @Order(19150)
    @Commit
    void persistIPNumbers() {
        System.out.println("PERSISTING ip-numbers to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(ipNumberAssets);
    }

    @Test
    @Order(19160)
    @Commit
    void persistUnixUsers() {
        System.out.println("PERSISTING unix-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(unixUserAssets);
    }

    @Test
    @Order(19170)
    @Commit
    void persistEmailAliases() {
        System.out.println("PERSISTING email-aliases to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(emailAliasAssets);
    }

    @Test
    @Order(19200)
    @Commit
    void persistDatabaseInstances() {
        System.out.println("PERSISTING db-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(dbInstanceAssets);
    }

    @Test
    @Order(19210)
    @Commit
    void persistDatabaseUsers() {
        System.out.println("PERSISTING db-users to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(dbUserAssets);
    }

    @Test
    @Order(19220)
    @Commit
    void persistDatabases() {
        System.out.println("PERSISTING databases to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(dbAssets);
    }

    @Test
    @Order(19300)
    @Commit
    void persistDomainSetups() {
        System.out.println("PERSISTING domain setups to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(domainSetupAssets);
    }

    @Test
    @Order(19301)
    @Commit
    void persistDomainDnsSetups() {
        System.out.println("PERSISTING domain DNS setups to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        HsDomainDnsSetupHostingAssetValidator.addZonefileErrorsTo(zonefileErrors);
        persistHostingAssets(domainDnsSetupAssets);
    }

    @Test
    @Order(19302)
    @Commit
    void persistDomainHttpSetups() {
        System.out.println("PERSISTING domain HTTP setups to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(domainHttpSetupAssets);
    }

    @Test
    @Order(19303)
    @Commit
    void persistDomainMboxSetups() {
        System.out.println("PERSISTING domain MBOX setups to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(domainMBoxSetupAssets);
    }

    @Test
    @Order(19304)
    @Commit
    void persistDomainSmtpSetups() {
        System.out.println("PERSISTING domain SMTP setups to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(domainSmtpSetupAssets);
    }

    @Test
    @Order(19400)
    @Commit
    void persistEmailAddresses() {
        System.out.println("PERSISTING email-aliases to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        persistHostingAssets(emailAddressAssets);
    }

    @Test
    @Order(19900)
    void verifyPersistedUnixUsersWithUserId() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(firstOfEach(15, unixUserAssets)).isEqualToIgnoringWhitespace("""
                {
                   5803=HsHostingAssetRealEntity(UNIX_USER, lug00, LUGs, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102090}),
                   5805=HsHostingAssetRealEntity(UNIX_USER, lug00-wla.1, Paul Klemm, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102091}),
                   5809=HsHostingAssetRealEntity(UNIX_USER, lug00-wla.2, Walter Müller, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 8, "SSD soft quota": 4, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102093}),
                   5811=HsHostingAssetRealEntity(UNIX_USER, lug00-ola.a, LUG OLA - POP a, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102094}),
                   5813=HsHostingAssetRealEntity(UNIX_USER, lug00-ola.b, LUG OLA - POP b, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102095}),
                   5835=HsHostingAssetRealEntity(UNIX_USER, lug00-test, Test, MANAGED_WEBSPACE:lug00, {"SSD hard quota": 1024, "SSD soft quota": 1024, "locked": false, "password": null, "shell": "/usr/bin/passwd", "userid": 102106}),
                   5964=HsHostingAssetRealEntity(UNIX_USER, mim00, Michael Mellis, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102147}),
                   5966=HsHostingAssetRealEntity(UNIX_USER, mim00-1981, Jahrgangstreffen 1981, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 256, "SSD soft quota": 128, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102148}),
                   5990=HsHostingAssetRealEntity(UNIX_USER, mim00-mail, Mailbox, MANAGED_WEBSPACE:mim00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 102160}),
                   6705=HsHostingAssetRealEntity(UNIX_USER, hsh00-mim, Michael Mellis, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/false", "userid": 10003}),
                   6824=HsHostingAssetRealEntity(UNIX_USER, hsh00, Hostsharing Paket, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 10000}),
                   7846=HsHostingAssetRealEntity(UNIX_USER, hsh00-dph, hsh00-uph, MANAGED_WEBSPACE:hsh00, {"HDD hard quota": 0, "HDD soft quota": 0, "SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/false", "userid": 110568}),
                   9546=HsHostingAssetRealEntity(UNIX_USER, dph00, Reinhard Wiese, MANAGED_WEBSPACE:dph00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 110593}),
                   9596=HsHostingAssetRealEntity(UNIX_USER, dph00-dph, Domain admin, MANAGED_WEBSPACE:dph00, {"SSD hard quota": 0, "SSD soft quota": 0, "locked": false, "password": null, "shell": "/bin/bash", "userid": 110594})
                }
                """);
    }

    @Test
    @Order(19910)
    void verifyBookingItemsAreActuallyPersisted() {
        final var biCount = (Integer) em.createNativeQuery("select count(*) from hs_booking_item", Integer.class)
                .getSingleResult();
        assertThat(biCount).isGreaterThan(isImportingControlledTestData() ? 5 : 500);
    }

    @Test
    @Order(19920)
    void verifyHostingAssetsAreActuallyPersisted() {
        final var haCount = (Integer) em.createNativeQuery("select count(*) from hs_hosting_asset", Integer.class)
                .getSingleResult();
        assertThat(haCount).isGreaterThan(isImportingControlledTestData() ? 40 : 15000);

        verifyActuallyPersistedHostingAssetCount(CLOUD_SERVER, 1, 50);
        verifyActuallyPersistedHostingAssetCount(MANAGED_SERVER, 4, 100);
        verifyActuallyPersistedHostingAssetCount(MANAGED_WEBSPACE, 4, 100);
        verifyActuallyPersistedHostingAssetCount(UNIX_USER, 14, 100);
        verifyActuallyPersistedHostingAssetCount(EMAIL_ALIAS, 9, 1400);
        verifyActuallyPersistedHostingAssetCount(PGSQL_DATABASE, 8, 100);
        verifyActuallyPersistedHostingAssetCount(MARIADB_DATABASE, 8, 100);
        verifyActuallyPersistedHostingAssetCount(DOMAIN_SETUP, 9, 100);
        verifyActuallyPersistedHostingAssetCount(EMAIL_ADDRESS, 71, 30000);
    }

    // ============================================================================================

    @Test
    @Order(19999)
    void logErrorsAfterPersistingHostingAssets() {
        errors.addAll(zonefileErrors);
        if (isImportingControlledTestData()) {
            expectError("[waera.de|DNS] zone waera.de/IN: has 0 SOA records");
            expectError("[waera.de|DNS] zone waera.de/IN: has no NS records");
            expectError("[waera.de|DNS] zone waera.de/IN: not loaded due to errors.");
            expectError("[xn--wra-qla.de|DNS] zone xn--wra-qla.de/IN: has 0 SOA records");
            expectError("[xn--wra-qla.de|DNS] zone xn--wra-qla.de/IN: has no NS records");
            expectError("[xn--wra-qla.de|DNS] zone xn--wra-qla.de/IN: not loaded due to errors.");
        }
        assertNoErrors();
    }

    // ============================================================================================

    private String vmName(final String zonenfileName) {
        return zonenfileName.substring(zonenfileName.length() - "vm0000.json".length()).substring(0, 6);
    }

    private void persistRecursively(final Integer key, final HsBookingItemEntity bi) {
        if (bi.getParentItem() != null) {
            persistRecursively(key, HsBookingItemEntityValidatorRegistry.validated(bi.getParentItem()));
        }
        persist(key, HsBookingItemEntityValidatorRegistry.validated(bi));
    }

    private void persistHostingAssets(final Map<Integer, HsHostingAssetRealEntity> assets) {
        persistHostingAssets(assets, null);
    }

    private void persistHostingAssets(final Map<Integer, HsHostingAssetRealEntity> assets, final HsHostingAssetType type) {
        final var assetsOfType = assets.entrySet().stream()
                .filter(entry -> type == null || type == entry.getValue().getType())
                .toList();
        final var chunkSize = isImportingControlledTestData() ? 10 : 500;
        ListUtils.partition(assetsOfType, chunkSize).forEach(chunk ->
                jpaAttempt.transacted(() -> {
                            context(rbacSuperuser);
                            chunk.forEach(entry ->
                                    logError(() ->
                                            new HostingAssetEntitySaveProcessor(em, entry.getValue())
                                                    .preprocessEntity()
                                                    .validateEntityIgnoring(
                                                            "'EMAIL_ALIAS:.*\\.config\\.target' .*",
                                                            "'EMAIL_ADDRESS:.*\\.config\\.target' .*"
                                                    )
                                                    .prepareForSave()
                                                    .saveUsing(entity -> persist(entry.getKey(), entity))
                                                    .validateContext()
                                    ));
                        }
                ).assertSuccessful()
        );
    }

    private void verifyActuallyPersistedHostingAssetCount(
            final HsHostingAssetType assetType,
            final int expectedCountInTestDataCount,
            final int minCountExpectedInProdData) {
        final var q = em.createNativeQuery(
                "select count(*) from hs_hosting_asset where type = cast(:type as HsHostingAssetType)",
                Integer.class);
        q.setParameter("type", assetType.name());
        final var count = (Integer) q.getSingleResult();
        if (isImportingControlledTestData()) {
            assertThat(count).isEqualTo(expectedCountInTestDataCount);
        } else {
            assertThat(count).isGreaterThanOrEqualTo(minCountExpectedInProdData);
        }

    }

    private void importIpNumbers(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var ipNumber = HsHostingAssetRealEntity.builder()
                            .type(IPV4_NUMBER)
                            .identifier(rec.getString("inet_addr"))
                            .caption(rec.getString("description"))
                            .build();
                    ipNumberAssets.put(rec.getInteger("inet_addr_id"), ipNumber);
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
                    hives.put(hive_id, hive);
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
                    bookingItems.put(packet_id, bookingItem);
                    final var haType = determineHaType(basepacket_code);

                    logError(() -> assertThat(!free || haType == MANAGED_WEBSPACE || bookingItem.getRelatedProject()
                            .getDebitor()
                            .getDefaultPrefix()
                            .equals("hsh"))
                            .as("packet.free only supported for Hostsharing-Assets and ManagedWebspace in customer-ManagedServer, but is set for "
                                    + packet_name)
                            .isTrue());

                    final var asset = HsHostingAssetRealEntity.builder()
                            // this turns off identifier validation to accept former default prefixes
                            .isLoaded(haType == MANAGED_WEBSPACE)
                            .type(haType)
                            .identifier(packet_name)
                            .bookingItem(bookingItem)
                            .caption("HA " + packet_name)
                            .build();
                    packetAssets.put(packet_id, asset);
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
                    final var unixUserAsset = HsHostingAssetRealEntity.builder()
                            .type(UNIX_USER)
                            .parentAsset(packetAssets.get(packet_id))
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
                            > 1024 * unixUserAsset.getContextValue("SSD", Integer.class, 0)) {
                        unixUserAsset.getConfig()
                                .put("SSD hard quota", unixUserAsset.getContextValue("SSD", Integer.class, 0) * 1024);
                    }
                    if (unixUserAsset.getDirectValue("HDD hard quota", Integer.class, 0)
                            > 1024 * unixUserAsset.getContextValue("HDD", Integer.class, 0)) {
                        unixUserAsset.getConfig()
                                .put("HDD hard quota", unixUserAsset.getContextValue("HDD", Integer.class, 0) * 1024);
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

                    unixUserAssets.put(unixuser_id, unixUserAsset);
                });
    }

    private void importEmailAliases(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var emailalias_id = rec.getInteger("emailalias_id");
                    final var packet_id = rec.getInteger("pac_id");
                    final var targets = parseCsvLine(rec.getString("target"));
                    final var emailAliasAsset = HsHostingAssetRealEntity.builder()
                            .type(EMAIL_ALIAS)
                            .parentAsset(packetAssets.get(packet_id))
                            .identifier(rec.getString("name"))
                            .caption(rec.getString("name"))
                            .config(ofEntries(
                                    entry("target", targets)
                            ))
                            .build();
                    emailAliasAssets.put(emailalias_id, emailAliasAsset);
                });
    }

    private void createDatabaseInstances(final List<HsHostingAssetRealEntity> parentAssets) {
        final var idRef = new AtomicInteger(0);
        parentAssets.forEach(pa -> {
            if (pa.getSubHostingAssets() == null) {
                pa.setSubHostingAssets(new ArrayList<>());
            }

            final var pgSqlInstanceAsset = HsHostingAssetRealEntity.builder()
                    .type(PGSQL_INSTANCE)
                    .parentAsset(pa)
                    .identifier(pa.getIdentifier() + "|PgSql.default")
                    .caption(pa.getIdentifier() + "-PostgreSQL default instance")
                    .build();
            pa.getSubHostingAssets().add(pgSqlInstanceAsset);
            dbInstanceAssets.put(idRef.getAndIncrement(), pgSqlInstanceAsset);

            final var mariaDbInstanceAsset = HsHostingAssetRealEntity.builder()
                    .type(MARIADB_INSTANCE)
                    .parentAsset(pa)
                    .identifier(pa.getIdentifier() + "|MariaDB.default")
                    .caption(pa.getIdentifier() + "-MariaDB default instance")
                    .build();
            pa.getSubHostingAssets().add(mariaDbInstanceAsset);
            dbInstanceAssets.put(idRef.getAndIncrement(), mariaDbInstanceAsset);
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
                    final var password_hash = rec.getString(
                            "password_hash",
                            HashGenerator.using(hash).withRandomSalt().hash("fake pw " + name));

                    final HsHostingAssetType dbInstanceAssetType = "mysql".equals(engine) ? MARIADB_INSTANCE
                            : "pgsql".equals(engine) ? PGSQL_INSTANCE
                            : failWith("unknown DB engine " + engine);
                    final var relatedWebspaceHA = packetAssets.get(packet_id).getParentAsset();
                    final var dbInstanceAsset = relatedWebspaceHA.getSubHostingAssets().stream()
                            .filter(ha -> ha.getType() == dbInstanceAssetType)
                            .findAny().orElseThrow(); // there is exactly one: the default instance for the given type

                    final var dbUserAsset = HsHostingAssetRealEntity.builder()
                            .type(dbUserAssetType)
                            .parentAsset(packetAssets.get(packet_id))
                            .assignedToAsset(dbInstanceAsset)
                            .identifier(dbUserAssetType.name().substring(0, 2) + "U|" + name)
                            .caption(name)
                            .config(new HashMap<>(ofEntries(
                                    entry("password", password_hash)
                            )))
                            .build();
                    dbUsersByEngineAndName.put(engine + ":" + name, dbUserAsset);
                    dbUserAssets.put(dbuser_id, dbUserAsset);
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
                    final var owningDbUserHA = dbUsersByEngineAndName.get(engine + ":" + owner);
                    assertThat(owningDbUserHA).as("owning user for " + (engine + ":" + owner) + " not found").isNotNull();
                    final HsHostingAssetType type = "mysql".equals(engine) ? MARIADB_DATABASE
                            : "pgsql".equals(engine) ? PGSQL_DATABASE
                            : failWith("unknown DB engine " + engine);
                    final var name = rec.getString("name");
                    final var encoding = rec.getString("encoding").replaceAll("[-_]+", "");
                    final var dbAsset = HsHostingAssetRealEntity.builder()
                            .type(type)
                            .parentAsset(owningDbUserHA)
                            .identifier(type.name().substring(0, 2) + "D|" + name)
                            .caption(name)
                            .config(ofEntries(
                                    entry(
                                            "encoding",
                                            type == MARIADB_DATABASE ? encoding.toLowerCase() : encoding.toUpperCase())
                            ))
                            .build();
                    dbAssets.put(database_id, dbAsset);
                });
    }

    private void importDomains(final String[] header, final List<String[]> records) {
        final var httpDomainSetupValidator = HostingAssetEntityValidatorRegistry.forType(DOMAIN_HTTP_SETUP);

        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var domain_id = rec.getInteger("domain_id");
                    final var domain_name = rec.getString("domain_name");
                    // final var domain_since = rec.getString("domain_since"); TODO.spec: to related BookingItem?
                    // final var domain_dns_master = rec.getString("domain_dns_master"); TODO.spec: do we need this and where?
                    final var owner_id = rec.getInteger("domain_owner");
                    final var domainoptions = rec.getString("domainoptions");

                    // Domain Setup
                    final var domainSetupAsset = HsHostingAssetRealEntity.builder()
                            .type(DOMAIN_SETUP)
                            // .parentAsset(parentDomainSetupAsset) are set once we've collected all of them
                            .identifier(domain_name)
                            .caption(IDN.toUnicode(domain_name))
                            .config(ofEntries(
                                    // nothing here
                            ))
                            .build();
                    domainSetupsByName.put(domain_name, domainSetupAsset);
                    domainSetupAssets.put(domain_id, domainSetupAsset);
                    domainSetupAsset.setSubHostingAssets(new ArrayList<>());

                    // Domain DNS Setup
                    final var ownerAsset = unixUserAssets.get(owner_id);
                    final var webspaceAsset = ownerAsset.getParentAsset();
                    assertThat(webspaceAsset.getType()).isEqualTo(MANAGED_WEBSPACE);
                    final var domainDnsSetupAsset = HsHostingAssetRealEntity.builder()
                            .type(DOMAIN_DNS_SETUP)
                            .parentAsset(domainSetupAsset)
                            .assignedToAsset(webspaceAsset)
                            .identifier(domain_name + "|DNS")
                            .caption("DNS-Setup für " + IDN.toUnicode(domain_name))
                            .config(new HashMap<>()) // is read from separate files
                            .build();
                    domainDnsSetupAssets.put(domain_id, domainDnsSetupAsset);
                    domainSetupAsset.getSubHostingAssets().add(domainDnsSetupAsset);

                    // Domain HTTP Setup
                    final var options = stream(domainoptions.split(",")).collect(toSet());
                    final var domainHttpSetupAsset = HsHostingAssetRealEntity.builder()
                            .type(DOMAIN_HTTP_SETUP)
                            .parentAsset(domainSetupAsset)
                            .assignedToAsset(ownerAsset)
                            .identifier(domain_name + "|HTTP")
                            .caption("HTTP-Setup für " + IDN.toUnicode(domain_name))
                            .config(ofEntries(
                                    entry("htdocsfallback", options.contains("htdocsfallback")),
                                    entry("indexes", options.contains("indexes")),
                                    entry("cgi", options.contains("cgi")),
                                    entry("passenger", options.contains("passenger")),
                                    entry("passenger-errorpage", options.contains("passenger-errorpage")),
                                    entry("fastcgi", options.contains("fastcgi")),
                                    entry("autoconfig", options.contains("autoconfig")),
                                    entry("greylisting", options.contains("greylisting")),
                                    entry("includes", options.contains("includes")),
                                    entry("letsencrypt", options.contains("letsencrypt")),
                                    entry("multiviews", options.contains("multiviews")),
                                    entry("subdomains", withDefault(rec.getString("valid_subdomain_names"), "*")
                                            .split(",")),
                                    entry("fcgi-php-bin", withDefault(
                                            rec.getString("fcgi_php_bin"),
                                            httpDomainSetupValidator.getProperty("fcgi-php-bin").defaultValue())),
                                    entry("passenger-nodejs", withDefault(
                                            rec.getString("passenger_nodejs"),
                                            httpDomainSetupValidator.getProperty("passenger-nodejs").defaultValue())),
                                    entry("passenger-python", withDefault(
                                            rec.getString("passenger_python"),
                                            httpDomainSetupValidator.getProperty("passenger-python").defaultValue())),
                                    entry("passenger-ruby", withDefault(
                                            rec.getString("passenger_ruby"),
                                            httpDomainSetupValidator.getProperty("passenger-ruby").defaultValue()))
                            ))
                            .build();
                    domainHttpSetupAssets.put(domain_id, domainHttpSetupAsset);
                    domainSetupAsset.getSubHostingAssets().add(domainHttpSetupAsset);

                    // Domain MBOX Setup
                    final var domainMboxSetupAsset = HsHostingAssetRealEntity.builder()
                            .type(DOMAIN_MBOX_SETUP)
                            .parentAsset(domainSetupAsset)
                            .assignedToAsset(webspaceAsset)
                            .identifier(domain_name + "|MBOX")
                            .caption("E-Mail-Empfang-Setup für " + IDN.toUnicode(domain_name))
                            .config(ofEntries(
                                    // no properties available
                            ))
                            .subHostingAssets(new ArrayList<>())
                            .build();
                    domainMBoxSetupAssets.put(domain_id, domainMboxSetupAsset);
                    domainSetupAsset.getSubHostingAssets().add(domainMboxSetupAsset);

                    // Domain SMTP Setup
                    final var domainSmtpSetupAsset = HsHostingAssetRealEntity.builder()
                            .type(DOMAIN_SMTP_SETUP)
                            .parentAsset(domainSetupAsset)
                            .assignedToAsset(webspaceAsset)
                            .identifier(domain_name + "|SMTP")
                            .caption("E-Mail-Versand-Setup für " + IDN.toUnicode(domain_name))
                            .config(ofEntries(
                                    // no properties available
                            ))
                            .build();
                    domainSmtpSetupAssets.put(domain_id, domainSmtpSetupAsset);
                    domainSetupAsset.getSubHostingAssets().add(domainSmtpSetupAsset);
                });

        domainSetupsByName.values().forEach(domainSetup -> {
            final var parentDomainName = domainSetup.getIdentifier().split("\\.", 2)[1];
            final var parentDomainSetup = domainSetupsByName.get(parentDomainName);
            if (parentDomainSetup != null) {
                domainSetup.setParentAsset(parentDomainSetup);
            }
        });
    }

    private String withDefault(final String givenValue, final Object defaultValue) {
        if (defaultValue instanceof String defaultStringValue) {
            return givenValue != null && !givenValue.isBlank() ? givenValue : defaultStringValue;
        }
        throw new RuntimeException(
                "property default value expected to be of type string, but is of type " + defaultValue.getClass()
                        .getSimpleName());
    }

    private void importZonefiles(final String vmName, final String zonenfilesJson) {
        if (zonenfilesJson == null || zonenfilesJson.isEmpty() || zonenfilesJson.isBlank()) {
            return;
        }

        try {
            //noinspection unchecked
            final Map<String, Map<String, Object>> zoneData = jsonMapper.readValue(zonenfilesJson, Map.class);
            importZonenfile(vmName, zoneData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("cannot read zonefile JSON: '" + zonenfilesJson + "'", e);
        }
    }

    private void importZonenfile(final String vmName, final Map<String, Map<String, Object>> zoneDataForVM) {
        zoneDataForVM.forEach((domainName, zoneData) -> {
            final var domainAsset = domainSetupsByName.get(domainName);
            if (domainAsset != null) {
                final var domainDnsSetupAsset = domainAsset.getSubHostingAssets().stream()
                        .filter(subAsset -> subAsset.getType() == DOMAIN_DNS_SETUP)
                        .findAny().orElse(null);
                assertThat(domainDnsSetupAsset).as(domainAsset.getIdentifier() + " has no DOMAIN_DNS_SETUP").isNotNull();

                final var domUser = domainAsset.getSubHostingAssets().stream()
                        .filter(ha -> ha.getType() == DOMAIN_HTTP_SETUP)
                        .findAny().orElseThrow()
                        .getAssignedToAsset();
                final var domOwner = zoneData.remove("DOM_OWNER");
                final var expectedDomOwner = domUser.getIdentifier();
                if (domOwner.equals(expectedDomOwner)) {
                    logError(() -> assertThat(vmName).isEqualTo(domUser.getParentAsset().getParentAsset().getIdentifier()));

                    //noinspection unchecked
                    zoneData.put("user-RR", ((ArrayList<ArrayList<Object>>) zoneData.get("user-RR")).stream()
                            .map(userRR -> userRR.stream().map(Object::toString).collect(Collectors.joining(" ")))
                            .toArray(String[]::new)
                    );
                    domainDnsSetupAsset.getConfig().putAll(zoneData);
                } else {
                    logError("zonedata dom_owner of " + domainAsset.getIdentifier() + " is " + domOwner + " but expected to be "
                            + expectedDomOwner);
                }
            }
        });
    }

    private void importEmailAddresses(final String[] header, final List<String[]> records) {
        final var columns = new Columns(header);
        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    // emailaddr_id;domain_id;localpart;subdomain;target
                    final var emailaddr_id = rec.getInteger("emailaddr_id");
                    final var domain_id = rec.getInteger("domain_id");
                    final var localpart = rec.getString("localpart");
                    final var subdomain = rec.getString("subdomain");
                    final var targets = stream(parseCsvLine(rec.getString("target")))
                            .map(t -> NOBODY_SUBSTITUTES.contains(t) ? "nobody" : t)
                            .toArray(String[]::new);
                    final var domainMboxSetup = domainMBoxSetupAssets.get(domain_id);
                    final var domainSetup = domainMboxSetup.getParentAsset();
                    final var emailAddress = localpart + "@" +
                            (subdomain != null && !subdomain.isBlank() ? subdomain + "." : "") + domainSetup.getIdentifier();
                    final var emailAddressAsset = HsHostingAssetRealEntity.builder()
                            .type(EMAIL_ADDRESS)
                            .parentAsset(domainMboxSetup)
                            .identifier(emailAddress)
                            .caption(emailAddress)
                            .config(ofNonNullEntries(
                                    entryIfNotNull("local-part", localpart),
                                    entryIfNotNull("sub-domain", subdomain),
                                    entry("target", targets)
                            ))
                            .build();
                    emailAddressAssets.put(emailaddr_id, emailAddressAsset);
                    domainMboxSetup.getSubHostingAssets().add(emailAddressAsset);
                });
    }

    @SafeVarargs
    private static Map<String, Object> ofNonNullEntries(final Map.Entry<String, Object>... entries) {
        //noinspection unchecked
        return ofEntries(stream(entries).filter(Objects::nonNull).toArray(Map.Entry[]::new));
    }

    private static Map.Entry<String, Object> entryIfNotNull(final String key, final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return entry(key, value);
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

    private static HsHostingAssetRealEntity ipNumber(final Integer inet_addr_id) {
        return inet_addr_id != null ? ipNumberAssets.get(inet_addr_id) : null;
    }

    private static Hive hive(final Integer hive_id) {
        return hive_id != null ? hives.get(hive_id) : null;
    }

    private static HsHostingAssetRealEntity pac(final Integer packet_id) {
        return packet_id != null ? packetAssets.get(packet_id) : null;
    }

    private String firstOfEach(
            final int maxCount,
            final Map<Integer, HsHostingAssetRealEntity> assets) {
        return toJsonFormattedString(assets.entrySet().stream().limit(maxCount)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, ImportHostingAssets::uniqueKeys, TreeMap::new)));
    }

    private String firstOfEach(
            final int maxCount,
            final Map<Integer, HsHostingAssetRealEntity> assets,
            final HsHostingAssetType type) {
        return toJsonFormattedString(assets.entrySet().stream()
                .filter(hae -> hae.getValue().getType() == type)
                .limit(maxCount)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, ImportHostingAssets::uniqueKeys, TreeMap::new)));
    }

    protected static <V> V uniqueKeys(final V v1, final V v2) {
        throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
    }

    private String firstOfEachType(
            final int maxCount,
            final HsBookingItemType... types) {
        return toJsonFormattedString(stream(types)
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

    protected static boolean isImportingControlledTestData() {
        return MIGRATION_DATA_PATH.equals(TEST_DATA_MIGRATION_DATA_PATH);
    }

    protected static void assumeThatWeAreImportingControlledTestData() {
        assumeThat(isImportingControlledTestData()).isTrue();
    }
}
