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
    }

    @Nested
    class AddServer {

        @Test
        void globalAdmin_canAddAsset() {

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
                                "caption": "some new CloudServer",
                                "config": { "CPU": 3, "extra": 42 }
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
                                "caption": "some new CloudServer",
                                "config": { "CPU": 3, "extra": 42 }
                            }
                            """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/hosting/assets/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new asset can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    class GetASset {

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

            final var givenAsset = givenSomeTemporaryAssetForDebitorNumber("2001", entry("something", 1));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "config": {
                                "CPU": "4",
                                "HDD": null,
                                "SSD": "4096"
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
                                "CPU": "4",
                                "SSD": "4096",
                                "something": 1
                            }
                         }
                    """)); // @formatter:on

            // finally, the asset is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isPresent().get()
                    .matches(asset -> {
                        assertThat(asset.toString()).isEqualTo("HsHostingAssetEntity(D-1000111:some CloudServer, CLOUD_SERVER, vm2001, some test-asset, { CPU: 4, SSD: 4096, something: 1 })");
                        return true;
                    });
        }
    }

    @Nested
    class DeleteAsset {

        @Test
        void globalAdmin_canDeleteArbitraryAsset() {
            context.define("superuser-alex@hostsharing.net");
            final var givenAsset = givenSomeTemporaryAssetForDebitorNumber("2002", entry("something", 1));

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
            final var givenAsset = givenSomeTemporaryAssetForDebitorNumber("2003", entry("something", 1));

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

    private HsHostingAssetEntity givenSomeTemporaryAssetForDebitorNumber(final String identifierSuffix,
            final Map.Entry<String, Integer> resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var newAsset = HsHostingAssetEntity.builder()
                    .uuid(UUID.randomUUID())
                    .bookingItem(givenBookingItem("First", "some CloudServer"))
                    .type(HsHostingAssetType.CLOUD_SERVER)
                    .identifier("vm" + identifierSuffix)
                    .caption("some test-asset")
                    .config(Map.ofEntries(resources))
                    .build();

            return assetRepo.save(newAsset);
        }).assertSuccessful().returnedValue();
    }
}
