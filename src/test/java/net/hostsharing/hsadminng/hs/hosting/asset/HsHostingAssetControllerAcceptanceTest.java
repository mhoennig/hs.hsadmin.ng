package net.hostsharing.hsadminng.hs.hosting.asset;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.strictlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

@Transactional
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@TestClassOrder(ClassOrderer.OrderAnnotation.class) // fail early on fetching problems
class HsHostingAssetControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsHostingAssetRepository assetRepo;

    @Autowired
    HsBookingItemRepository bookingItemRepo;

    @Autowired
    HsBookingProjectRepository projectRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    @Order(2)
    class ListAssets {

        @Test
        void globalAdmin_canViewAllAssetsOfArbitraryDebitor() {

            // given
            context("superuser-alex@hostsharing.net");
            final var givenProject = projectRepo.findByCaption("D-1000111 default project").stream()
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/assets?projectUuid=" + givenProject.getUuid() + "&type=MANAGED_WEBSPACE")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "type": "MANAGED_WEBSPACE",
                            "identifier": "sec01",
                             "caption": "some Webspace",
                            "config": {}
                        },
                        {
                            "type": "MANAGED_WEBSPACE",
                            "identifier": "fir01",
                            "caption": "some Webspace",
                            "config": {}
                        },
                        {
                            "type": "MANAGED_WEBSPACE",
                            "identifier": "thi01",
                            "caption": "some Webspace",
                            "config": {}
                        }
                    ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_canViewAllAssetsByType() {

            // given
            context("superuser-alex@hostsharing.net");

            RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                    .   get("http://localhost/api/hs/hosting/assets?type=" + MANAGED_SERVER)
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("", lenientlyEquals("""
                        [
                            {
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1011",
                                "caption": "some ManagedServer",
                                "config": {
                                    "monit_max_cpu_usage": 90,
                                    "monit_max_ram_usage": 80,
                                    "monit_max_ssd_usage": 70
                                }
                            },
                            {
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1012",
                                "caption": "some ManagedServer",
                                "config": {
                                    "monit_max_cpu_usage": 90,
                                    "monit_max_ram_usage": 80,
                                    "monit_max_ssd_usage": 70
                                }
                            },
                            {
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1013",
                                "caption": "some ManagedServer",
                                "config": {
                                    "monit_max_cpu_usage": 90,
                                    "monit_max_ram_usage": 80,
                                    "monit_max_ssd_usage": 70
                                }
                            }
                        ]
                        """));
            // @formatter:on
        }
    }

    @Nested
    @Order(3)
    class AddAsset {

        @Test
        void globalAdmin_canAddBookedAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = newBookingItem("D-1000111 default project",
                    HsBookingItemType.MANAGED_WEBSPACE, "separate ManagedWebspace BI",
                    Map.ofEntries(
                            entry("SSD", 50),
                            entry("Traffic", 50)
                    )
            );
            final var givenParentAsset = givenParentAsset(MANAGED_SERVER, "vm1011");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "bookingItemUuid": "%s",
                                "type": "MANAGED_WEBSPACE",
                                "identifier": "fir10",
                                "parentAssetUuid": "%s",
                                "caption": "some separate ManagedWebspace HA",
                                "config": {}
                            }
                            """.formatted(givenBookingItem.getUuid(), givenParentAsset.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/hosting/assets")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "type": "MANAGED_WEBSPACE",
                                "identifier": "fir10",
                                "caption": "some separate ManagedWebspace HA",
                                "config": {}
                            }
                            """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/hosting/assets/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new asset can be accessed under the generated UUID
            final var newWebspace = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newWebspace).isNotNull();
            toCleanup(HsHostingAssetEntity.class, newWebspace);
        }

        @Test
        void parentAssetAgent_canAddSubAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenParentAsset = givenParentAsset(MANAGED_WEBSPACE, "fir01");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .header("assumed-roles", "hs_hosting_asset#vm1011:ADMIN")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "parentAssetUuid": "%s",
                                    "type": "UNIX_USER",
                                    "identifier": "fir01-temp",
                                    "caption": "some new UnixUser in client's ManagedWebspace",
                                    "config": {}
                                }
                                """.formatted(givenParentAsset.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/hosting/assets")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                                {
                                    "type": "UNIX_USER",
                                    "identifier": "fir01-temp",
                                    "caption": "some new UnixUser in client's ManagedWebspace",
                                    "config": {}
                                }
                                """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/hosting/assets/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new asset can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void propertyValidationsArePerformend_whenAddingAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenSomeNewBookingItem(
                    "D-1000111 default project",
                    HsBookingItemType.MANAGED_SERVER,
                    "some PrivateCloud");

            RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "bookingItemUuid": "%s",
                                    "type": "MANAGED_SERVER",
                                    "identifier": "vm1400",
                                    "caption": "some new ManagedServer",
                                    "config": {  "monit_max_ssd_usage": 0, "monit_max_cpu_usage": 101, "extra": 42 }
                                }
                                """.formatted(givenBookingItem.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/hosting/assets")
                    .then().log().all().assertThat()
                        .statusCode(400)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                                {
                                    "statusPhrase": "Bad Request",
                                    "message": "[
                                              <<<'MANAGED_SERVER:vm1400.config.extra' is not expected but is set to '42',
                                              <<<'MANAGED_SERVER:vm1400.config.monit_max_cpu_usage' is expected to be at most 100 but is 101,
                                              <<<'MANAGED_SERVER:vm1400.config.monit_max_ssd_usage' is expected to be at least 10 but is 0
                                              <<<]"
                                }
                                """.replaceAll(" +<<<", "")));  // @formatter:on
        }

        @Test
        void totalsLimitValidationsArePerformend_whenAddingAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenHostingAsset = givenHostingAsset(MANAGED_WEBSPACE, "fir01");
            assertThat(givenHostingAsset.getBookingItem().getResources().get("Multi"))
                    .as("precondition failed")
                    .isEqualTo(1);

            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                for (int n = 0; n < 25; ++n) {
                    toCleanup(assetRepo.save(
                            HsHostingAssetEntity.builder()
                                    .type(UNIX_USER)
                                    .parentAsset(givenHostingAsset)
                                    .identifier("fir01-%2d".formatted(n))
                                    .caption("Test UnixUser fir01-%2d".formatted(n))
                                    .build()));
                }
            }).assertSuccessful();

            RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                    {
                                        "parentAssetUuid": "%s",
                                        "type": "UNIX_USER",
                                        "identifier": "fir01-extra",
                                        "caption": "some extra UnixUser",
                                        "config": { }
                                    }
                                    """.formatted(givenHostingAsset.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/hosting/assets")
                    .then().log().all().assertThat()
                        .statusCode(400)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                                    {
                                        "statusPhrase": "Bad Request",
                                        "message": "['D-1000111:D-1000111 default project:separate ManagedWebspace.resources.Multi=1 allows at maximum 25 unix users, but 26 found]"
                                    }
                                    """.replaceAll(" +<<<", "")));  // @formatter:on
        }
    }

    @Nested
    @Order(1)
    class GetAsset {

        @Test
        void globalAdmin_canGetArbitraryAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAssetUuid = assetRepo.findByIdentifier("vm1011").stream()
                    .filter(bi -> bi.getBookingItem().getProject().getCaption().equals("D-1000111 default project"))
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/assets/" + givenAssetUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some ManagedServer",
                            "config": {}
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAssetUuid = assetRepo.findByIdentifier("vm1012").stream()
                    .filter(bi -> bi.getBookingItem().getProject().getCaption().equals("D-1000212 default project"))
                    .map(HsHostingAssetEntity::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/assets/" + givenAssetUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void debitorAgentUser_canGetRelatedAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAssetUuid = assetRepo.findByIdentifier("vm1013").stream()
                    .filter(bi -> bi.getBookingItem().getProject().getCaption().equals("D-1000313 default project"))
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "person-TuckerJack@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/assets/" + givenAssetUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "identifier": "vm1013",
                            "caption": "some ManagedServer",
                            "config": {}
                        }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Order(4)
    class PatchAsset {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfAsset() {

            final var givenAsset = givenSomeTemporaryHostingAsset(() ->
                    HsHostingAssetEntity.builder()
                            .uuid(UUID.randomUUID())
                            .bookingItem(givenSomeNewBookingItem(
                                    "D-1000111 default project",
                                    HsBookingItemType.MANAGED_SERVER,
                                    "temp ManagedServer"))
                            .type(MANAGED_SERVER)
                            .identifier("vm2001")
                            .caption("some test-asset")
                            .config(Map.ofEntries(
                                    Map.<String, Object>entry("monit_max_ssd_usage", 80),
                                    Map.<String, Object>entry("monit_max_hdd_usage", 90),
                                    Map.<String, Object>entry("monit_max_cpu_usage", 90),
                                    Map.<String, Object>entry("monit_max_ram_usage", 70)
                            ))
                            .build());
            final var alarmContactUuid = givenContact().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "alarmContactUuid": "%s",
                            "config": {
                                "monit_max_ssd_usage": 85,
                                "monit_max_hdd_usage": null,
                                "monit_min_free_ssd": 5
                            }
                        }
                        """.formatted(alarmContactUuid))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/hosting/assets/" + givenAsset.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                        {
                            "type": "MANAGED_SERVER",
                            "identifier": "vm2001",
                            "caption": "some test-asset",
                            "alarmContact": {
                                "caption": "second contact",
                                "emailAddresses": {
                                    "main": "contact-admin@secondcontact.example.com"
                                }
                            },
                            "config": {
                                "monit_max_cpu_usage": 90,
                                "monit_max_ram_usage": 70,
                                "monit_max_ssd_usage": 85,
                                "monit_min_free_ssd": 5
                            }
                         }
                    """));
                // @formatter:on

            // finally, the asset is actually updated
            em.clear();
            context.define("superuser-alex@hostsharing.net");
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isPresent().get()
                    .matches(asset -> {
                        assertThat(asset.getAlarmContact()).isNotNull()
                                .extracting(c -> c.getEmailAddresses().get("main"))
                                .isEqualTo("contact-admin@secondcontact.example.com");
                        assertThat(asset.getConfig().toString())
                                .isEqualToIgnoringWhitespace("""
                                    {
                                        "monit_max_cpu_usage": 90,
                                        "monit_max_ram_usage": 70,
                                        "monit_max_ssd_usage": 85,
                                        "monit_min_free_ssd": 5
                                    }
                                    """);
                        return true;
                    });
        }

        @Test
        void assetAdmin_canPatchAllUpdatablePropertiesOfAsset() {

            final var givenAsset = givenSomeTemporaryHostingAsset(() ->
                    HsHostingAssetEntity.builder()
                            .uuid(UUID.randomUUID())
                            .type(UNIX_USER)
                            .parentAsset(givenHostingAsset(MANAGED_WEBSPACE, "fir01"))
                            .identifier("fir01-temp")
                            .caption("some test-unix-user")
                            .build());

            RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        //.header("assumed-roles", "hs_hosting_asset#vm2001:ADMIN")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "caption": "some patched test-unix-user",
                                "config": {
                                    "shell": "/bin/bash",
                                    "totpKey": "0x1234567890abcdef0123456789abcdef",
                                    "password": "Ein Passwort mit 4 Zeichengruppen!"
                                }
                            }
                            """)
                        .port(port)
                    .when()
                        .patch("http://localhost/api/hs/hosting/assets/" + givenAsset.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "type": "UNIX_USER",
                                "identifier": "fir01-temp",
                                "caption": "some patched test-unix-user",
                                "config": {
                                    "homedir": "/home/pacs/fir01/users/temp",
                                    "shell": "/bin/bash"
                                }
                             }
                        """))
                    // the config separately but not-leniently to make sure that no write-only-properties are listed
                    .body("config", strictlyEquals("""
                        {
                            "homedir": "/home/pacs/fir01/users/temp",
                            "shell": "/bin/bash"
                        }
                        """))
            ;
            // @formatter:on

            // finally, the asset is actually updated
            assertThat(jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                return assetRepo.findByUuid(givenAsset.getUuid());
            }).returnedValue()).isPresent().get()
                    .matches(asset -> {
                        assertThat(asset.getCaption()).isEqualTo("some patched test-unix-user");
                        assertThat(asset.getConfig().toString()).isEqualTo("""
                               {
                                   "password": "Ein Passwort mit 4 Zeichengruppen!",
                                   "shell": "/bin/bash",
                                   "totpKey": "0x1234567890abcdef0123456789abcdef"
                               }
                               """);
                        return true;
                    });
        }
    }

    @Nested
    @Order(5)
    class DeleteAsset {

        @Test
        void globalAdmin_canDeleteArbitraryAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAsset = givenSomeTemporaryHostingAsset(() ->
                    HsHostingAssetEntity.builder()
                            .uuid(UUID.randomUUID())
                            .bookingItem(givenSomeNewBookingItem(
                                    "D-1000111 default project",
                                    HsBookingItemType.MANAGED_SERVER,
                                    "temp ManagedServer"))
                            .type(MANAGED_SERVER)
                            .identifier("vm1002")
                            .caption("some test-asset")
                            .config(Map.ofEntries(
                                    Map.<String, Object>entry("monit_max_ssd_usage", 80),
                                    Map.<String, Object>entry("monit_max_hdd_usage", 90),
                                    Map.<String, Object>entry("monit_max_cpu_usage", 90),
                                    Map.<String, Object>entry("monit_max_ram_usage", 70)
                            ))
                            .build());
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/hosting/assets/" + givenAsset.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given assets is gone
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAsset = givenSomeTemporaryHostingAsset(() ->
                    HsHostingAssetEntity.builder()
                            .uuid(UUID.randomUUID())
                            .bookingItem(givenSomeNewBookingItem(
                                    "D-1000111 default project",
                                    HsBookingItemType.MANAGED_SERVER,
                                    "temp ManagedServer"))
                            .type(MANAGED_SERVER)
                            .identifier("vm1003")
                            .caption("some test-asset")
                            .config(Map.ofEntries(
                                    Map.<String, Object>entry("monit_max_ssd_usage", 80),
                                    Map.<String, Object>entry("monit_max_hdd_usage", 90),
                                    Map.<String, Object>entry("monit_max_cpu_usage", 90),
                                    Map.<String, Object>entry("monit_max_ram_usage", 70)
                            ))
                            .build());
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/hosting/assets/" + givenAsset.getUuid())
                .then().log().all().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given asset is still there
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isNotEmpty();
        }
    }

    HsHostingAssetEntity givenHostingAsset(final HsHostingAssetType type, final String identifier) {
        return assetRepo.findByIdentifier(identifier).stream()
                .filter(ha -> ha.getType() == type)
                .findAny().orElseThrow();
    }

    HsBookingItemEntity newBookingItem(
            final String projectCaption,
            final HsBookingItemType type, final String bookingItemCaption, final Map<String, Object> resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var project = projectRepo.findByCaption(projectCaption).stream()
                    .findAny().orElseThrow();
            final var bookingItem = HsBookingItemEntity.builder()
                    .project(project)
                    .type(type)
                    .caption(bookingItemCaption)
                    .resources(resources)
                    .build();
            return toCleanup(bookingItemRepo.save(bookingItem));
        }).assertSuccessful().returnedValue();
    }

    HsBookingItemEntity givenSomeNewBookingItem(
            final String projectCaption,
            final HsBookingItemType bookingItemType,
            final String bookingItemCaption) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var project = projectRepo.findByCaption(projectCaption).getFirst();
            final var resources = switch (bookingItemType) {
                case MANAGED_SERVER -> Map.<String, Object>ofEntries(entry("CPUs", 1),
                        entry("RAM", 20),
                        entry("SSD", 25),
                        entry("Traffic", 250));
                default -> new HashMap<String, Object>();
            };
            final var newBookingItem = HsBookingItemEntity.builder()
                    .project(project)
                    .type(bookingItemType)
                    .caption(bookingItemCaption)
                    .resources(resources)
                    .build();
            return toCleanup(bookingItemRepo.save(newBookingItem));
        }).assertSuccessful().returnedValue();
    }

    HsHostingAssetEntity givenParentAsset(final HsHostingAssetType assetType, final String assetIdentifier) {
        final var givenAsset = assetRepo.findByIdentifier(assetIdentifier).stream()
                .filter(a -> a.getType() == assetType)
                .findAny().orElseThrow();
        return givenAsset;
    }

    private HsHostingAssetEntity givenSomeTemporaryHostingAsset(final Supplier<HsHostingAssetEntity> newAsset) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            return toCleanup(assetRepo.save(newAsset.get()));
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeContactEntity givenContact() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            return contactRepo.findContactByOptionalCaptionLike("second").stream().findFirst().orElseThrow();
        }).returnedValue();
    }

}
