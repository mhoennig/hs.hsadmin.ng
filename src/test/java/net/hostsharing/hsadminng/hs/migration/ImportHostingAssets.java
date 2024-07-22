package net.hostsharing.hsadminng.hs.migration;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV4_NUMBER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
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

    record Hive(int hive_id, String hive_name, int inet_addr_id, AtomicReference<HsHostingAssetEntity> serverRef) {}

    static Map<Integer, HsBookingProjectEntity> bookingProjects = new WriteOnceMap<>();
    static Map<Integer, HsBookingItemEntity> bookingItems = new WriteOnceMap<>();
    static Map<Integer, Hive> hives = new WriteOnceMap<>();
    static Map<Integer, HsHostingAssetEntity> hostingAssets = new WriteOnceMap<>(); // TODO.impl: separate maps for each type?

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

        // no contacts yet => mostly null values
        assertThat(firstOfEachType(5, IPV4_NUMBER)).isEqualToIgnoringWhitespace("""
                {
                   1000363=HsHostingAssetEntity(IPV4_NUMBER, 83.223.95.34),
                   1000381=HsHostingAssetEntity(IPV4_NUMBER, 83.223.95.52),
                   1000402=HsHostingAssetEntity(IPV4_NUMBER, 83.223.95.73),
                   1000433=HsHostingAssetEntity(IPV4_NUMBER, 83.223.95.104),
                   1000457=HsHostingAssetEntity(IPV4_NUMBER, 83.223.95.128)
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

        // no contacts yet => mostly null values
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
                   3000630=HsHostingAssetEntity(MANAGED_WEBSPACE,   hsh00, HA hsh00,  MANAGED_SERVER:vm1050,    D-1000000:hsh default project:BI hsh00),
                   3000968=HsHostingAssetEntity(MANAGED_SERVER,    vm1061, HA vm1061,                           D-1015200:rar default project:BI vm1061),
                   3000978=HsHostingAssetEntity(MANAGED_SERVER,    vm1050, HA vm1050,                           D-1000000:hsh default project:BI vm1050),
                   3001061=HsHostingAssetEntity(MANAGED_SERVER,    vm1068, HA vm1068,                           D-1000300:mim default project:BI vm1068),
                   3001094=HsHostingAssetEntity(MANAGED_WEBSPACE,   lug00, HA lug00,  MANAGED_SERVER:vm1068,    D-1000300:mim default project:BI lug00),
                   3001112=HsHostingAssetEntity(MANAGED_WEBSPACE,   mim00, HA mim00,  MANAGED_SERVER:vm1068,    D-1000300:mim default project:BI mim00),
                   3023611=HsHostingAssetEntity(CLOUD_SERVER,      vm2097, HA vm2097,                           D-1101800:wws default project:BI vm2097)
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

        // no contacts yet => mostly null values
        assertThat(firstOfEachType(5, CLOUD_SERVER, MANAGED_SERVER, MANAGED_WEBSPACE))
                .isEqualToIgnoringWhitespace("""
                        {
                           3000630=HsHostingAssetEntity(MANAGED_WEBSPACE, hsh00, HA hsh00, MANAGED_SERVER:vm1050, D-1000000:hsh default project:BI hsh00),
                           3000968=HsHostingAssetEntity(MANAGED_SERVER, vm1061, HA vm1061, D-1015200:rar default project:BI vm1061),
                           3000978=HsHostingAssetEntity(MANAGED_SERVER, vm1050, HA vm1050, D-1000000:hsh default project:BI vm1050),
                           3001061=HsHostingAssetEntity(MANAGED_SERVER, vm1068, HA vm1068, D-1000300:mim default project:BI vm1068),
                           3001094=HsHostingAssetEntity(MANAGED_WEBSPACE, lug00, HA lug00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI lug00),
                           3001112=HsHostingAssetEntity(MANAGED_WEBSPACE, mim00, HA mim00, MANAGED_SERVER:vm1068, D-1000300:mim default project:BI mim00),
                           3001447=HsHostingAssetEntity(MANAGED_SERVER, vm1093, HA vm1093, D-1000000:hsh default project:BI vm1093),
                           3019959=HsHostingAssetEntity(MANAGED_WEBSPACE, dph00, HA dph00, MANAGED_SERVER:vm1093, D-1101900:dph default project:BI dph00),
                           3023611=HsHostingAssetEntity(CLOUD_SERVER, vm2097, HA vm2097, D-1101800:wws default project:BI vm2097)
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
    @Order(11400)
    void validateBookingItems() {
        bookingItems.forEach((id, bi) -> {
            try {
                HsBookingItemEntityValidatorRegistry.validated(bi);
            } catch (final Exception exc) {
                System.err.println("validation failed for id:" + id + "( " + bi + "): " + exc.getMessage());
            }
        });
    }

    @Test
    @Order(11410)
    void validateHostingAssets() {
        hostingAssets.forEach((id, ha) -> {
            try {
                new HostingAssetEntitySaveProcessor(ha)
                        .preprocessEntity()
                        .validateEntity();
            } catch (final Exception exc) {
                System.err.println("validation failed for id:" + id + "( " + ha + "): " + exc.getMessage());
            }
        });
    }

    @Test
    @Order(19000)
    @Commit
    void persistHostingAssetEntities() {

        System.out.println("PERSISTING hosting-assets to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            bookingProjects.forEach(this::persist);
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            bookingItems.forEach(this::persistRecursively);
        }).assertSuccessful();

        persistHostingAssetsOfType(CLOUD_SERVER);
        persistHostingAssetsOfType(MANAGED_SERVER);
        persistHostingAssetsOfType(MANAGED_WEBSPACE);
        persistHostingAssetsOfType(IPV4_NUMBER);
    }

    @Test
    @Order(99999)
    void logErrors() {
        super.logErrors();
    }

    private void persistRecursively(final Integer key, final HsBookingItemEntity bi) {
        if (bi.getParentItem() != null) {
            persistRecursively(key, HsBookingItemEntityValidatorRegistry.validated(bi.getParentItem()));
        }
        persist(key, HsBookingItemEntityValidatorRegistry.validated(bi));
    }

    private void persistHostingAssetsOfType(final HsHostingAssetType hsHostingAssetType) {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            hostingAssets.forEach((key, ha) -> {
                    if (ha.getType() == hsHostingAssetType) {
                        new HostingAssetEntitySaveProcessor(ha)
                                .preprocessEntity()
                                .validateEntity()
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
                    final var ipNumber = HsHostingAssetEntity.builder()
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

                    logError(() -> assertThat(!free || haType == MANAGED_WEBSPACE || bookingItem.getRelatedProject().getDebitor().getDefaultPrefix().equals("hsh"))
                            .as("packet.free only supported for Hostsharing-Assets and ManagedWebspace in customer-ManagedServer, but is set for " + packet_name)
                            .isTrue());

                    final var asset = HsHostingAssetEntity.builder()
                            .isLoaded(haType == MANAGED_WEBSPACE) // this turns off identifier validation to accept former default prefixes
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
                        case "RAM" -> returning("RAM", convert = v -> v/1024);
                        case "QUOTA" -> returning("SSD", convert = v -> v/1024);
                        case "STORAGE" -> returning("HDD", convert = v -> v/1024);
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
                        if ( ofNullable(asset.getBookingItem().getResources().get(name)).map("BASIC"::equals).orElse(true) ) {
                            asset.getBookingItem().getResources().put(name, slaValue);
                        }
                    } else if (name.startsWith("SLA")) {
                        asset.getBookingItem().getResources().put(name, true);
                    } else if (quantity > 0) {
                        asset.getBookingItem().getResources().put(name, convert.apply(quantity));
                    }
                });
    }

    <V> V returning(final V value, final Object... assignments) {
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

    private static HsHostingAssetEntity ipNumber(final Integer inet_addr_id) {
        return inet_addr_id != null ? hostingAssets.get(IP_NUMBER_ID_OFFSET + inet_addr_id) : null;
    }

    private static Hive hive(final Integer hive_id) {
        return hive_id != null ? hives.get(HIVE_ID_OFFSET + hive_id) : null;
    }

    private static HsHostingAssetEntity pac(final Integer packet_id) {
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
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
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
