package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.test.DisableSecurityConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
@TestClassOrder(ClassOrderer.OrderAnnotation.class) // fail early on fetching problems
class HsBookingItemControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsBookingItemRealRepository realBookingItemRepo;

    @Autowired
    HsBookingProjectRealRepository realProjectRepo;

    @Autowired
    HsBookingDebitorRepository debitorRepo;

    @Autowired
    HsHostingAssetRealRepository realHostingAssetRepo;

    @Autowired
    BookingItemCreatedEventRepository bookingItemCreationEventRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    @Order(2)
    class ListBookingItems {

        @Test
        void globalAdmin_canViewAllBookingItemsOfArbitraryDebitor() {

            // given
            context("superuser-alex@hostsharing.net");
            final var givenProject = findDefaultProjectOfDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items?projectUuid=" + givenProject.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "type": "MANAGED_WEBSPACE",
                            "caption": "separate ManagedWebspace",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "SSD": 100,
                                "Multi": 1,
                                "Daemons": 0,
                                "Traffic": 50
                            }
                        },
                        {
                            "type": "MANAGED_SERVER",
                            "caption": "separate ManagedServer",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "RAM": 8,
                                "SSD": 500,
                                "CPU": 2,
                                "Traffic": 500
                            }
                        },
                        {
                            "type": "PRIVATE_CLOUD",
                            "caption": "some PrivateCloud",
                            "validFrom": "2024-04-01",
                            "validTo": null,
                            "resources": {
                                "HDD": 10000,
                                "RAM": 32,
                                "SSD": 4000,
                                "CPU": 10,
                                "Traffic": 2000
                            }
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Order(3)
    class PostNewBookingItem {

        @Test
        void globalAdmin_canPostNewBookingItem() {

            context.define("superuser-alex@hostsharing.net");
            final var givenProject = findDefaultProjectOfDebitorNumber(1000111);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "project.uuid": "{projectUuid}",
                                "type": "MANAGED_SERVER",
                                "caption": "some new booking",
                                "validTo": "{validTo}",
                                "resources": { "CPU": 12, "RAM": 4, "SSD": 100, "Traffic": 250 }
                            }
                            """
                                .replace("{projectUuid}", givenProject.getUuid().toString())
                                .replace("{validTo}", LocalDate.now().plusMonths(1).toString())
                        )
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/items")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "type": "MANAGED_SERVER",
                                "caption": "some new booking",
                                "validFrom": "{today}",
                                "validTo": "{todayPlus1Month}",
                                "resources": { "CPU": 12, "SSD": 100, "Traffic": 250 }
                             }
                            """
                                .replace("{today}", LocalDate.now().toString())
                                .replace("{todayPlus1Month}", LocalDate.now().plusMonths(1).toString()))
                        )
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/items/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bookingItem can be accessed under the generated UUID
            assertThat(fetchRealBookingItemFromURI(location)).isNotNull();
        }

        @Test
        void projectAgent_canAddManagedWebspaceBookingItemWithHostingAsset() {

            context.define("superuser-alex@hostsharing.net", "hs_booking.project#D-1000111-D-1000111defaultproject:AGENT");
            final var givenProject = findDefaultProjectOfDebitorNumber(1000111);
            final var givenManagedServer = realHostingAssetRepo.findByTypeAndIdentifier(MANAGED_SERVER, "vm1011").stream()
                    .map(HsHostingAsset::getBookingItem)
                    .findFirst().orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                                {
                                    "project.uuid": "{projectUuid}",
                                    "parentItem.uuid": "{managedServerUuid}",
                                    "type": "MANAGED_WEBSPACE",
                                    "caption": "some managed webspace",
                                    "resources": {
                                        "SSD": 25,
                                        "Traffic": 250
                                    },
                                    "hostingAsset": {
                                        "type": "MANAGED_WEBSPACE",
                                        "identifier": "fir00"
                                    }
                                }
                                """
                            .replace("{projectUuid}", givenProject.getUuid().toString())
                            .replace("{managedServerUuid}", givenManagedServer.getUuid().toString())
                    )
                    .port(port)
                    .when()
                    .post("http://localhost/api/hs/booking/items")
                    .then().log().all().assertThat()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                                {
                                    "type": "MANAGED_WEBSPACE",
                                    "caption": "some managed webspace",
                                    "validFrom": "{today}",
                                    "validTo": null
                                 }
                                """
                            .replace("{today}", LocalDate.now().toString())
                            .replace("{todayPlus1Month}", LocalDate.now().plusMonths(1).toString()))
                    )
                    .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/items/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // then, the new BookingItem can be accessed under the generated UUID
            final var newBookingItem = fetchRealBookingItemFromURI(location);
            assertThat(newBookingItem)
                    .extracting(HsBookingItem::getCaption)
                    .isEqualTo("some managed webspace");

            // and the related HostingAssets are also got created
            final var domainSetupHostingAsset = realHostingAssetRepo.findByIdentifier("fir00");
            assertThat(domainSetupHostingAsset).isNotEmpty()
                    .map(HsHostingAsset::getBookingItem)
                    .contains(newBookingItem);
            final var event = bookingItemCreationEventRepo.findByBookingItem(newBookingItem);
            assertThat(event).isNull();
        }

        @Test
        void projectAgent_canAddDomainSetupBookingItemWithHostingAsset() {

            context.define("superuser-alex@hostsharing.net", "hs_booking.project#D-1000111-D-1000111defaultproject:AGENT");
            final var givenProject = findDefaultProjectOfDebitorNumber(1000111);
            // TODO.impl: "sec01-web" should not work, but does
            final var givenUnixUser = realHostingAssetRepo.findByTypeAndIdentifier(UNIX_USER, "fir01-web").stream()
                    .findFirst().orElseThrow();

            Dns.fakeResultForDomain("example.org",
                    Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=just-a-fake-verification-code"));

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "project.uuid": "{projectUuid}",
                                    "type": "DOMAIN_SETUP",
                                    "caption": "Domain-Setup for example.org",
                                    "resources": {
                                        "domainName": "example.org",
                                        "verificationCode": "just-a-fake-verification-code"
                                    },
                                    "hostingAsset": {
                                        "identifier": "example.org", // also as default for all subAssets
                                        "subHostingAssets": [
                                            {
                                                "type": "DOMAIN_DNS_SETUP"
                                            },
                                            {
                                                "type": "DOMAIN_HTTP_SETUP",
                                                "assignedToAsset.uuid": "{unixUserUuid}"
                                            },
                                            {
                                                "type": "DOMAIN_MBOX_SETUP"
                                            },
                                            {
                                                "type": "DOMAIN_SMTP_SETUP"
                                            }
                                        ]
                                    }
                                }
                                """
                                .replace("{projectUuid}", givenProject.getUuid().toString())
                                .replace("{unixUserUuid}", givenUnixUser.getUuid().toString())
                        )
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/items")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                                {
                                    "type": "DOMAIN_SETUP",
                                    "caption": "Domain-Setup for example.org",
                                    "validFrom": "{today}",
                                    "validTo": null
                                 }
                                """
                                .replace("{today}", LocalDate.now().toString())
                                .replace("{todayPlus1Month}", LocalDate.now().plusMonths(1).toString()))
                        )
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/items/[^/]*"))
                        .extract().header("Location");  // @formatter:on

            // then, the new BookingItem can be accessed under the generated UUID
            final var newBookingItem = fetchRealBookingItemFromURI(location);
            assertThat(newBookingItem)
                    .extracting(HsBookingItem::getCaption)
                    .isEqualTo("Domain-Setup for example.org");

            // and the related HostingAssets are also got created
            final var domainSetupHostingAsset = realHostingAssetRepo.findByIdentifier("example.org");
            assertThat(domainSetupHostingAsset).isNotEmpty()
                    .map(HsHostingAsset::getBookingItem)
                    .contains(newBookingItem);
            // TODO.legacy: add check for example.org|DNS
            assertThat(realHostingAssetRepo.findByIdentifier("example.org|HTTP")).isNotEmpty()
                    .map(HsHostingAsset::getParentAsset)
                    .isEqualTo(domainSetupHostingAsset);
            assertThat(realHostingAssetRepo.findByIdentifier("example.org|MBOX")).isNotEmpty()
                    .map(HsHostingAsset::getParentAsset)
                    .isEqualTo(domainSetupHostingAsset);
            assertThat(realHostingAssetRepo.findByIdentifier("example.org|SMTP")).isNotEmpty()
                    .map(HsHostingAsset::getParentAsset)
                    .isEqualTo(domainSetupHostingAsset);
            final var event = bookingItemCreationEventRepo.findByBookingItem(newBookingItem);
            assertThat(event).isNull();
        }

        @Test
        void projectAgent_canPostNewBookingItemEvenIfHostingAssetCreationFails() {

            context.define("superuser-alex@hostsharing.net", "hs_booking.project#D-1000111-D-1000111defaultproject:AGENT");
            final var givenProject = findDefaultProjectOfDebitorNumber(1000111);
            final var givenUnixUser = realHostingAssetRepo.findByIdentifier("fir01-web").stream().findFirst().orElseThrow();

            Dns.fakeResultForDomain("example.org", Dns.Result.fromRecords()); // without valid verificationCode

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "project.uuid": "{projectUuid}",
                                    "type": "DOMAIN_SETUP",
                                    "caption": "some new domain-setup booking",
                                    "resources": {
                                        "domainName": "example.org",
                                        "verificationCode": "just-a-fake-verification-code"
                                    },
                                    "hostingAsset": {
                                        "identifier": "example.org", // also as default for all subAssets
                                        "subHostingAssets": [
                                            {
                                                "type": "DOMAIN_DNS_SETUP"
                                            },
                                            {
                                                "type": "DOMAIN_HTTP_SETUP",
                                                "assignedToAsset.uuid": "{unixUserUuid}"
                                            },
                                            {
                                                "type": "DOMAIN_MBOX_SETUP"
                                            },
                                            {
                                                "type": "DOMAIN_SMTP_SETUP"
                                            }
                                        ]
                                    }
                                }
                                """
                                .replace("{projectUuid}", givenProject.getUuid().toString())
                                .replace("{unixUserUuid}", givenUnixUser.getUuid().toString())
                        )
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/items")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                                {
                                    "type": "DOMAIN_SETUP",
                                    "caption": "some new domain-setup booking",
                                    "validFrom": "{today}",
                                    "validTo": null,
                                    "resources": { "domainName": "example.org" }
                                 }
                                """
                                .replace("{today}", LocalDate.now().toString())
                                .replace("{todayPlus1Month}", LocalDate.now().plusMonths(1).toString()))
                        )
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/items/[^/]*"))
                        .extract().header("Location");  // @formatter:on

            // then, the new BookingItem can be accessed under the generated UUID
            final var newBookingItem = fetchRealBookingItemFromURI(location);
            assertThat(newBookingItem)
                    .extracting(bi -> bi.getDirectValue("domainName", String.class))
                    .isEqualTo("example.org");
            final var event = bookingItemCreationEventRepo.findByBookingItem(newBookingItem);
            assertThat(event.getStatusMessage())
                    .isEqualTo("[[DNS] no TXT record 'Hostsharing-domain-setup-verification-code=just-a-fake-verification-code' found for domain name 'example.org' (nor in its super-domain)]");

            // but the related HostingAsset did not get created
            assertThat(realHostingAssetRepo.findByIdentifier("example.org")).isEmpty();
        }
    }

    private @NotNull HsBookingProjectRealEntity findDefaultProjectOfDebitorNumber(final int debitorNumber) {
        return debitorRepo.findByDebitorNumber(debitorNumber).stream()
                .map(d -> realProjectRepo.findAllByDebitorUuid(d.getUuid()))
                .flatMap(List::stream)
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @Order(1)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GetBookingItem {

        @Test
        @Order(1)
        void globalAdmin_canGetArbitraryBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = realBookingItemRepo.findByCaption("separate ManagedWebspace").stream()
                            .filter(bi -> belongsToProject(bi, "D-1000111 default project"))
                            .map(HsBookingItem::getUuid)
                            .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItemUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "type": "MANAGED_WEBSPACE",
                             "caption": "separate ManagedWebspace",
                             "validFrom": "2022-10-01",
                             "validTo": null,
                             "resources": {
                                 "SSD": 100,
                                 "Multi": 1,
                                 "Daemons": 0,
                                 "Traffic": 50
                            }
                        }
                    """)); // @formatter:on
        }

        @Test
        @Order(2)
        void normalUser_canNotGetUnrelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = realBookingItemRepo.findByCaption("separate ManagedServer").stream()
                    .filter(bi -> belongsToProject(bi, "D-1000212 default project"))
                    .map(HsBookingItem::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItemUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Order(3)
        void projectAdmin_canGetRelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = realBookingItemRepo.findByCaption("separate ManagedServer").stream()
                    .filter(bi -> belongsToProject(bi, "D-1000313 default project"))
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_booking.project#D-1000313-D-1000313defaultproject:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "type": "MANAGED_SERVER",
                            "caption": "separate ManagedServer",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "RAM": 8,
                                "SSD": 500,
                                "CPU": 2,
                                "Traffic": 500
                            }
                        }
                    """)); // @formatter:on
        }

        private static boolean belongsToProject(final HsBookingItem bi, final String projectCaption) {
            return ofNullable(bi)
                    .map(HsBookingItem::getProject)
                    .filter(bp -> bp.getCaption().equals(projectCaption))
                    .isPresent();
        }
    }

    @Nested
    @Order(4)
    class PatchBookingItem {

        @Test
        void projectAgent_canPatchAllUpdatablePropertiesOfBookingItem() {

            final var givenBookingItem = givenSomeNewBookingItem("D-1000111 default project", MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_booking.project#D-1000111-D-1000111defaultproject:AGENT")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "validFrom": "2020-06-05",
                            "validTo": "2022-12-31",
                            "resources": {
                                "Traffic": 500,
                                "HDD": null,
                                "SSD": 100
                            }
                        }
                        """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some test-booking",
                            "validFrom": "2022-11-01",
                            "validTo": "2022-12-31",
                            "resources": {
                                "Traffic": 500,
                                "SSD": 100
                            }
                         }
                    """)); // @formatter:on

            // finally, the bookingItem is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(realBookingItemRepo.findByUuid(givenBookingItem.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getProject().getDebitor().toString()).isEqualTo("booking-debitor(D-1000111: fir)");
                        assertThat(mandate.getValidFrom()).isEqualTo("2022-11-01");
                        assertThat(mandate.getValidTo()).isEqualTo("2022-12-31");
                        return true;
                    });
        }
    }

    @Nested
    @Order(5)
    class DeleteBookingItem {

        @Test
        void globalAdmin_canDeleteArbitraryBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenSomeNewBookingItem("D-1000111 default project", MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bookingItem is gone
            assertThat(realBookingItemRepo.findByUuid(givenBookingItem.getUuid())).isEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenSomeNewBookingItem("D-1000111 default project", MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given bookingItem is still there
            assertThat(realBookingItemRepo.findByUuid(givenBookingItem.getUuid())).isNotEmpty();
        }
    }

    @SafeVarargs
    private HsBookingItem givenSomeNewBookingItem(final String projectCaption,
            final HsBookingItemType hsBookingItemType, final Map.Entry<String, Object>... resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenProject = realProjectRepo.findByCaption(projectCaption).stream()
                    .findAny().orElseThrow();
            final var newBookingItem = HsBookingItemRealEntity.builder()
                    .uuid(UUID.randomUUID())
                    .project(givenProject)
                    .type(hsBookingItemType)
                    .caption("some test-booking")
                    .resources(Map.ofEntries(resources))
                    .validity(Range.closedOpen(
                            LocalDate.parse("2022-11-01"), LocalDate.parse("2023-03-31")))
                    .build();

            return realBookingItemRepo.save(newBookingItem);
        }).assertSuccessful().returnedValue();
    }

    @AfterEach
    void cleanupEventEntities() {
        jpaAttempt.transacted(() -> {
            em.createQuery("delete from BookingItemCreatedEventEntity").executeUpdate();
        }).assertSuccessful();
    }

    private Map.Entry<String, Object> resource(final String key, final Object value) {
        return entry(key, value);
    }

    private HsBookingItemRealEntity fetchRealBookingItemFromURI(final String location) {
        final var newBookingItemUuid = UUID.fromString(
                location.substring(location.lastIndexOf('/') + 1));
        assertThat(newBookingItemUuid).isNotNull();
        final var optional = realBookingItemRepo.findByUuid(newBookingItemUuid);
        assertThat(optional).isNotEmpty();
        return optional.get();
    }
}
