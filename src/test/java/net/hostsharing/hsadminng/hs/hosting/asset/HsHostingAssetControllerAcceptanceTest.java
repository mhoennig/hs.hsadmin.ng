package net.hostsharing.hsadminng.hs.hosting.asset;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsHostingAssetControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsHostingAssetRepository assetRepo;

    @Autowired
    HsBookingItemRepository bookingItemRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class ListAssets {

        @Test
        void globalAdmin_canViewAllAssetsOfArbitraryDebitor() {

            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(1000111).get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/assets?debitorUuid=" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "type": "MANAGED_WEBSPACE",
                            "identifier": "aaa01",
                            "caption": "some Webspace",
                            "config": {
                                "HDD": 2048,
                                "RAM": 1,
                                "SDD": 512,
                                "extra": 42
                            }
                        },
                        {
                            "type": "MANAGED_SERVER",
                            "identifier": "vm1011",
                            "caption": "some ManagedServer",
                            "config": {
                                "CPU": 2,
                                "SDD": 512,
                                "extra": 42
                            }
                        },
                        {
                            "type": "CLOUD_SERVER",
                            "identifier": "vm2011",
                            "caption": "another CloudServer",
                            "config": {
                                "CPU": 2,
                                "HDD": 1024,
                                "extra": 42
                            }
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
                    .get("http://localhost/api/hs/hosting/assets?type=" + MANAGED_SERVER)
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
                                "CPU": 2,
                                "SDD": 512,
                                "extra": 42
                            }
                        },
                        {
                            "type": "MANAGED_SERVER",
                            "identifier": "vm1013",
                            "caption": "some ManagedServer",
                            "config": {
                                "CPU": 2,
                                "SDD": 512,
                                "extra": 42
                            }
                        },
                        {
                            "type": "MANAGED_SERVER",
                            "identifier": "vm1012",
                            "caption": "some ManagedServer",
                            "config": {
                                "CPU": 2,
                                "SDD": 512,
                                "extra": 42
                            }
                        }
                    ]
                    """));
            // @formatter:on
        }
    }

    @Nested
    class AddServer {

        @Test
        void globalAdmin_canAddBookedAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenBookingItem("First", "some PrivateCloud");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "bookingItemUuid": "%s",
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1400",
                                "caption": "some new ManagedServer",
                                "config": { "CPUs": 2, "RAM": 100, "SSD": 300, "Traffic": 250 }
                            }
                            """.formatted(givenBookingItem.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/hosting/assets")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1400",
                                "caption": "some new ManagedServer",
                                "config": { "CPUs": 2, "RAM": 100, "SSD": 300, "Traffic": 250 }
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
        void parentAssetAgent_canAddSubAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenParentAsset = givenParentAsset("First", MANAGED_SERVER);

            final var location = RestAssured // @formatter:off
                    .given()
                    .header("current-user", "person-FirbySusan@example.com")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "parentAssetUuid": "%s",
                                "type": "MANAGED_WEBSPACE",
                                "identifier": "fir90",
                                "caption": "some new ManagedWebspace in client's ManagedServer",
                                "config": { "SSD": 100, "Traffic": 250 }
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
                                "type": "MANAGED_WEBSPACE",
                                "identifier": "fir90",
                                "caption": "some new ManagedWebspace in client's ManagedServer",
                                "config": { "SSD": 100, "Traffic": 250 }
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
        void additionalValidationsArePerformend_whenAddingAsset() {

            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenBookingItem("First", "some PrivateCloud");

            final var location = RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "bookingItemUuid": "%s",
                                "type": "MANAGED_SERVER",
                                "identifier": "vm1400",
                                "caption": "some new ManagedServer",
                                "config": { "CPUs": 0, "extra": 42 }
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
                                "message": "['config.extra' is not expected but is set to '42', 'config.CPUs' is expected to be >= 1 but is 0, 'config.RAM' is required but missing, 'config.SSD' is required but missing, 'config.Traffic' is required but missing]"
                            }
                            """));  // @formatter:on
        }
    }

    @Nested
    class GetAsset {

        @Test
        void globalAdmin_canGetArbitraryAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAssetUuid = assetRepo.findAll().stream()
                            .filter(bi -> bi.getBookingItem().getDebitor().getDebitorNumber() == 1000111)
                            .filter(item -> item.getCaption().equals("some ManagedServer"))
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
                            "config": {
                                 "CPU": 2,
                                 "SDD": 512,
                                 "extra": 42
                             }
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAssetUuid = assetRepo.findAll().stream()
                    .filter(bi -> bi.getBookingItem().getDebitor().getDebitorNumber() == 1000212)
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
            final var givenAssetUuid = assetRepo.findAll().stream()
                    .filter(bi -> bi.getBookingItem().getDebitor().getDebitorNumber() == 1000313)
                    .filter(bi -> bi.getCaption().equals("some ManagedServer"))
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
                            "config": {
                                "CPU": 2,
                                "SDD": 512,
                                "extra": 42
                            }
                        }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchAsset {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfAsset() {

            final var givenAsset = givenSomeTemporaryHostingAsset("2001", CLOUD_SERVER,
                    config("CPUs", 4), config("RAM", 100), config("HDD", 100), config("Traffic", 2000));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "config": {
                                "CPUs": 2,
                                "HDD": null,
                                "SSD": 250
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
                            "type": "CLOUD_SERVER",
                            "identifier": "vm2001",
                            "caption": "some test-asset",
                            "config": {
                                "CPUs": 2,
                                "RAM": 100,
                                "SSD": 250
                            }
                         }
                    """)); // @formatter:on

            // finally, the asset is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isPresent().get()
                    .matches(asset -> {
                        assertThat(asset.toString()).isEqualTo("HsHostingAssetEntity(CLOUD_SERVER, vm2001, some test-asset, D-1000111:some CloudServer, { CPUs: 2, RAM: 100, SSD: 250, Traffic: 2000 })");
                        return true;
                    });
        }
    }

    @Nested
    class DeleteAsset {

        @Test
        void globalAdmin_canDeleteArbitraryAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAsset = givenSomeTemporaryHostingAsset("2002", CLOUD_SERVER,
                    config("CPUs", 4), config("RAM", 100), config("HDD", 100), config("Traffic", 2000));

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
            final var givenAsset = givenSomeTemporaryHostingAsset("2003", CLOUD_SERVER,
                    config("CPUs", 4), config("RAM", 100), config("HDD", 100), config("Traffic", 2000));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/hosting/assets/" + givenAsset.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given asset is still there
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isNotEmpty();
        }
    }

    HsBookingItemEntity givenBookingItem(final String debitorName, final String bookingItemCaption) {
        final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike(debitorName).stream().findAny().orElseThrow();
        return bookingItemRepo.findAllByDebitorUuid(givenDebitor.getUuid()).stream()
                .filter(i -> i.getCaption().equals(bookingItemCaption))
                .findAny().orElseThrow();
    }

    HsHostingAssetEntity givenParentAsset(final String debitorName, final HsHostingAssetType assetType) {
        final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike(debitorName).stream().findAny().orElseThrow();
        final var givenAsset = assetRepo.findAllByCriteria(givenDebitor.getUuid(), null, assetType).stream().findAny().orElseThrow();
        return givenAsset;
    }

    @SafeVarargs
    private HsHostingAssetEntity givenSomeTemporaryHostingAsset(final String identifierSuffix,
            final HsHostingAssetType hostingAssetType,
            final Map.Entry<String, Object>... resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var newAsset = HsHostingAssetEntity.builder()
                    .uuid(UUID.randomUUID())
                    .bookingItem(givenBookingItem("First", "some CloudServer"))
                    .type(hostingAssetType)
                    .identifier("vm" + identifierSuffix)
                    .caption("some test-asset")
                    .config(Map.ofEntries(resources))
                    .build();

            return assetRepo.save(newAsset);
        }).assertSuccessful().returnedValue();
    }

    private Map.Entry<String, Object> config(final String key, final Object value) {
        return entry(key, value);
    }
}
