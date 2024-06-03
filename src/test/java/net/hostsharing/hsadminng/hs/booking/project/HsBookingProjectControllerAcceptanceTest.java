package net.hostsharing.hsadminng.hs.booking.project;

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
class HsBookingProjectControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsBookingProjectRepository bookingProjectRepo;

    @Autowired
    HsBookingProjectRepository projectRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListBookingProjects {

        @Test
        void globalAdmin_canViewAllBookingProjectsOfArbitraryDebitor() {

            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(1000111).stream()
                            .findFirst()
                            .orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects?debitorUuid=" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "caption": "D-1000111 default project"
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    class AddBookingProject {

        @Test
        void globalAdmin_canAddBookingProject() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(1000111).stream()
                    .findFirst()
                    .orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "debitorUuid": "%s",
                                "caption": "some new project"
                            }
                            """.formatted(givenDebitor.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/projects")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "caption": "some new project"
                             }
                            """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/projects/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bookingProject can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    class GetBookingProject {

        @Test
        void globalAdmin_canGetArbitraryBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = bookingProjectRepo.findAll().stream()
                            .filter(project -> project.getDebitor().getDebitorNumber() == 1000111)
                            .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "D-1000111 default project"
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = bookingProjectRepo.findAll().stream()
                    .filter(project -> project.getDebitor().getDebitorNumber() == 1000212)
                    .map(HsBookingProjectEntity::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void debitorAgentUser_canGetRelatedBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = bookingProjectRepo.findAll().stream()
                    .filter(project -> project.getDebitor().getDebitorNumber() == 1000313)
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "person-TuckerJack@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "D-1000313 default project"
                        }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchBookingProject {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfBookingProject() {

            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "caption": "some project"
                        }
                        """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some project"
                         }
                    """)); // @formatter:on

            // finally, the bookingProject is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(bookingProjectRepo.findByUuid(givenBookingProject.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)");
                        return true;
                    });
        }
    }

    @Nested
    class DeleteBookingProject {

        @Test
        void globalAdmin_canDeleteArbitraryBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bookingProject is gone
            assertThat(bookingProjectRepo.findByUuid(givenBookingProject.getUuid())).isEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given bookingProject is still there
            assertThat(bookingProjectRepo.findByUuid(givenBookingProject.getUuid())).isNotEmpty();
        }
    }

    private HsBookingProjectEntity givenSomeBookingProject(final int debitorNumber, final String caption) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(debitorNumber).stream().findAny().orElseThrow();
            final var newBookingProject = HsBookingProjectEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitor(givenDebitor)
                    .caption(caption)
                    .build();

            return bookingProjectRepo.save(newBookingProject);
        }).assertSuccessful().returnedValue();
    }

    private Map.Entry<String, Object> resource(final String key, final Object value) {
        return entry(key, value);
    }
}
