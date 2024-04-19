package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
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
class HsBookingItemControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsBookingItemRepository bookingItemRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListBookingItems {

        @Test
        void globalAdmin_canViewAllBookingItemsOfArbitraryDebitor() {

            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(1000111).get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items?debitorUuid=" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                             "caption": "some ManagedServer",
                             "validFrom": "2022-10-01",
                             "validTo": null,
                             "resources": {
                                 "CPU": 2,
                                 "SDD": 512,
                                 "extra": 42
                             }
                         },
                         {
                             "caption": "some CloudServer",
                             "validFrom": "2023-01-15",
                             "validTo": "2024-04-14",
                             "resources": {
                                 "CPU": 2,
                                 "HDD": 1024,
                                 "extra": 42
                             }
                         },
                         {
                             "caption": "some PrivateCloud",
                             "validFrom": "2024-04-01",
                             "validTo": null,
                             "resources": {
                                 "CPU": 10,
                                 "HDD": 10240,
                                 "SDD": 10240,
                                 "extra": 42
                             }
                         }
                     ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    class AddBookingItem {

        @Test
        void globalAdmin_canAddBookingItem() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(1000111).get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "debitorUuid": "%s",
                                "caption": "some new booking",
                                "resources": { "CPU": 12, "extra": 42 },
                                "validFrom": "2022-10-13"
                            }
                            """.formatted(givenDebitor.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/items")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "caption": "some new booking",
                                "validFrom": "2022-10-13",
                                "validTo": null,
                                "resources": { "CPU": 12 }
                             }
                            """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/items/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bookingItem can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    class GetBookingItem {

        @Test
        void globalAdmin_canGetArbitraryBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = bookingItemRepo.findAll().stream()
                            .filter(bi -> bi.getDebitor().getDebitorNumber() == 1000111)
                            .filter(item -> item.getCaption().equals("some CloudServer"))
                            .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItemUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some CloudServer",
                            "validFrom": "2023-01-15",
                            "validTo": "2024-04-14",
                            "resources": { CPU: 2, HDD: 1024 }
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = bookingItemRepo.findAll().stream()
                    .filter(bi -> bi.getDebitor().getDebitorNumber() == 1000212)
                    .map(HsBookingItemEntity::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItemUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void debitorAgentUser_canGetRelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = bookingItemRepo.findAll().stream()
                    .filter(bi -> bi.getDebitor().getDebitorNumber() == 1000313)
                    .filter(item -> item.getCaption().equals("some CloudServer"))
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "person-TuckerJack@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/items/" + givenBookingItemUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some CloudServer",
                            "validFrom": "2023-01-15",
                            "validTo": "2024-04-14",
                            "resources": { CPU: 2, HDD: 1024 }
                        }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchBookingItem {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfBookingItem() {

            final var givenBookingItem = givenSomeTemporaryBookingItemForDebitorNumber(1000111, entry("something", 1));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "validFrom": "2020-06-05",
                            "validTo": "2022-12-31",
                            "resources": {
                                "CPU": "4",
                                "HDD": null,
                                "SSD": "4096"
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
                                "CPU": "4",
                                "SSD": "4096",
                                "something": 1
                            }
                         }
                    """)); // @formatter:on

            // finally, the bookingItem is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(bookingItemRepo.findByUuid(givenBookingItem.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)");
                        assertThat(mandate.getValidFrom()).isEqualTo("2022-11-01");
                        assertThat(mandate.getValidTo()).isEqualTo("2022-12-31");
                        return true;
                    });
        }
    }

    @Nested
    class DeleteBookingItem {

        @Test
        void globalAdmin_canDeleteArbitraryBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenSomeTemporaryBookingItemForDebitorNumber(1000111, entry("something", 1));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bookingItem is gone
            assertThat(bookingItemRepo.findByUuid(givenBookingItem.getUuid())).isEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenSomeTemporaryBookingItemForDebitorNumber(1000111, entry("something", 1));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/items/" + givenBookingItem.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given bookingItem is still there
            assertThat(bookingItemRepo.findByUuid(givenBookingItem.getUuid())).isNotEmpty();
        }
    }

    private HsBookingItemEntity givenSomeTemporaryBookingItemForDebitorNumber(final int debitorNumber,
            final Map.Entry<String, Integer> resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(debitorNumber).get(0);
            final var newBookingItem = HsBookingItemEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitor(givenDebitor)
                    .caption("some test-booking")
                    .resources(Map.ofEntries(resources))
                    .validity(Range.closedOpen(
                            LocalDate.parse("2022-11-01"), LocalDate.parse("2023-03-31")))
                    .build();

            return bookingItemRepo.save(newBookingItem);
        }).assertSuccessful().returnedValue();
    }
}
