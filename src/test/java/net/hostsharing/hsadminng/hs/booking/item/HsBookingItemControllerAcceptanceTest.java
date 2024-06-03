package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
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
    HsBookingProjectRepository projectRepo;

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
            final var givenProject = debitorRepo.findDebitorByDebitorNumber(1000111).stream()
                            .map(d -> projectRepo.findAllByDebitorUuid(d.getUuid()))
                            .flatMap(List::stream)
                            .findFirst()
                            .orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
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
                            "caption": "some ManagedWebspace",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "SDD": 512,
                                "Multi": 4,
                                "Daemons": 2,
                                "Traffic": 12
                            }
                        },
                        {
                            "type": "MANAGED_SERVER",
                            "caption": "separate ManagedServer",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "RAM": 8,
                                "SDD": 512,
                                "CPUs": 2,
                                "Traffic": 42
                            }
                        },
                        {
                            "type": "PRIVATE_CLOUD",
                            "caption": "some PrivateCloud",
                            "validFrom": "2024-04-01",
                            "validTo": null,
                            "resources": {
                                "HDD": 10240,
                                "SDD": 10240,
                                "CPUs": 10,
                                "Traffic": 42
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
            final var givenProject = debitorRepo.findDebitorByDebitorNumber(1000111).stream()
                    .map(d -> projectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(List::stream)
                    .findFirst()
                    .orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "projectUuid": "%s",
                                "type": "MANAGED_SERVER",
                                "caption": "some new booking",
                                "resources": { "CPUs": 12, "RAM": 4, "SSD": 100, "Traffic": 250 },
                                "validFrom": "2022-10-13"
                            }
                            """.formatted(givenProject.getUuid()))
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
                                "validFrom": "2022-10-13",
                                "validTo": null,
                                "resources": { "CPUs": 12, "SSD": 100, "Traffic": 250 }
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
                            .filter(bi -> belongsToDebitorNumber(bi, 1000111))
                            .filter(item -> item.getCaption().equals("some ManagedWebspace"))
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
                            "type": "MANAGED_WEBSPACE",
                             "caption": "some ManagedWebspace",
                             "validFrom": "2022-10-01",
                             "validTo": null,
                             "resources": {
                                 "SDD": 512,
                                 "Multi": 4,
                                 "Daemons": 2,
                                 "Traffic": 12
                            }
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedBookingItem() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingItemUuid = bookingItemRepo.findAll().stream()
                    .filter(bi -> belongsToDebitorNumber(bi, 1000212))
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
                    .filter(bi -> belongsToDebitorNumber(bi, 1000313))
                    .filter(item -> item.getCaption().equals("separate ManagedServer"))
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
                            "type": "MANAGED_SERVER",
                            "caption": "separate ManagedServer",
                            "validFrom": "2022-10-01",
                            "validTo": null,
                            "resources": {
                                "RAM": 8,
                                "SDD": 512,
                                "CPUs": 2,
                                "Traffic": 42
                            }
                        }
                    """)); // @formatter:on
        }

        private static boolean belongsToDebitorNumber(final HsBookingItemEntity bi, final int i) {
            return ofNullable(bi)
                    .map(HsBookingItemEntity::getProject)
                    .map(HsBookingProjectEntity::getDebitor)
                    .map(HsOfficeDebitorEntity::getDebitorNumber)
                    .filter(debitorNumber -> debitorNumber == i)
                    .isPresent();
        }
    }

    @Nested
    class PatchBookingItem {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfBookingItem() {

            final var givenBookingItem = givenSomeBookingItem(1000111, MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
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
            assertThat(bookingItemRepo.findByUuid(givenBookingItem.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getProject().getDebitor().toString()).isEqualTo("debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)");
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
            final var givenBookingItem = givenSomeBookingItem(1000111, MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

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
            final var givenBookingItem = givenSomeBookingItem(1000111, MANAGED_WEBSPACE,
                    resource("HDD", 100), resource("SSD", 50), resource("Traffic", 250));

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

    @SafeVarargs
    private HsBookingItemEntity givenSomeBookingItem(final int debitorNumber,
            final HsBookingItemType hsBookingItemType, final Map.Entry<String, Object>... resources) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenProject = debitorRepo.findDebitorByDebitorNumber(debitorNumber).stream()
                    .map(d -> projectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(java.util.List::stream)
                    .findAny().orElseThrow();
            final var newBookingItem = HsBookingItemEntity.builder()
                    .uuid(UUID.randomUUID())
                    .project(givenProject)
                    .type(hsBookingItemType)
                    .caption("some test-booking")
                    .resources(Map.ofEntries(resources))
                    .validity(Range.closedOpen(
                            LocalDate.parse("2022-11-01"), LocalDate.parse("2023-03-31")))
                    .build();

            return bookingItemRepo.save(newBookingItem);
        }).assertSuccessful().returnedValue();
    }

    private Map.Entry<String, Object> resource(final String key, final Object value) {
        return entry(key, value);
    }
}
