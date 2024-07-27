package net.hostsharing.hsadminng.hs.office.person;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
class HsOfficePersonControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListPersons {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllPersons_ifNoCriteriaGiven() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/persons")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasSize(13));
                // @formatter:on
        }
    }

    @Nested
    class AddPerson {

        @Test
        void globalAdmin_canAddPerson() {

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "personType": "NATURAL_PERSON",
                                   "familyName": "Tester",
                                   "givenName": "Temp Testi"
                                 }
                            """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/persons")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("personType", is("NATURAL_PERSON"))
                        .body("familyName", is("Tester"))
                        .body("givenName", is("Temp Testi"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new person can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    @Transactional
    class GetPerson {

        @Test
        void globalAdmin_canGetArbitraryPerson() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPersonUuid = personRepo.findPersonByOptionalNameLike("Erben").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/persons/" + givenPersonUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "tradeName": "Erben Bessler"
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedPerson() {
            final var givenPersonUuid = jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                return personRepo.findPersonByOptionalNameLike("Erben").get(0).getUuid();
            }).returnedValue();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/persons/" + givenPersonUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void personOwnerUser_canGetRelatedPerson() {
            final var givenPersonUuid = jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                return personRepo.findPersonByOptionalNameLike("Erben").get(0).getUuid();
            }).returnedValue();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "person-ErbenBesslerMelBessler@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/persons/" + givenPersonUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "personType": "UNINCORPORATED_FIRM",
                        "tradeName": "Erben Bessler",
                        "givenName": "Bessler",
                        "familyName": "Mel"
                     }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Transactional
    class PatchPerson {

        @Test
        void globalAdmin_canPatchAllPropertiesOfArbitraryPerson() {

            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "personType": "UNINCORPORATED_FIRM",
                           "tradeName": "Temp Trade Name - patched",
                           "familyName": "Temp Family Name - patched",
                           "givenName": "Temp Given Name - patched"
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("personType", is("UNINCORPORATED_FIRM"))
                    .body("tradeName", is("Temp Trade Name - patched"))
                    .body("familyName", is("Temp Family Name - patched"))
                    .body("givenName", is("Temp Given Name - patched"));
                // @formatter:on

            // finally, the person is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPersonType()).isEqualTo(HsOfficePersonType.UNINCORPORATED_FIRM);
                        assertThat(person.getTradeName()).isEqualTo("Temp Trade Name - patched");
                        assertThat(person.getFamilyName()).isEqualTo("Temp Family Name - patched");
                        assertThat(person.getGivenName()).isEqualTo("Temp Given Name - patched");
                        return true;
                    });
        }

        @Test
        void globalAdmin_canPatchPartialPropertiesOfArbitraryPerson() {

            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "familyName": "Temp Family Name - patched",
                            "givenName": "Temp Given Name - patched"
                        }
                        """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("personType", is(givenPerson.getPersonType().name()))
                    .body("tradeName", is(givenPerson.getTradeName()))
                    .body("familyName", is("Temp Family Name - patched"))
                    .body("givenName", is("Temp Given Name - patched"));
            // @formatter:on

            // finally, the person is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPersonType()).isEqualTo(givenPerson.getPersonType());
                        assertThat(person.getTradeName()).isEqualTo(givenPerson.getTradeName());
                        assertThat(person.getFamilyName()).isEqualTo("Temp Family Name - patched");
                        assertThat(person.getGivenName()).isEqualTo("Temp Given Name - patched");
                        return true;
                    });
        }
    }

    @Nested
    @Transactional
    class DeletePerson {

        @Test
        void globalAdmin_canDeleteArbitraryPerson() {
            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given person is gone

            context.define("superuser-alex@hostsharing.net");
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isEmpty();
        }

        @Test
        void personOwner_canDeleteRelatedPerson() {
            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-test-user@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given person is still there
            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                assertThat(personRepo.findByUuid(givenPerson.getUuid())).isEmpty();
            }).assertSuccessful();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedPerson() {
            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // unrelated user cannot even view the person
            // @formatter:on

            // then the given person is still there
            context.define("superuser-alex@hostsharing.net");
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isNotEmpty();
        }
    }

    private HsOfficePersonEntity givenSomeTemporaryPersonCreatedBy(final String creatingUser) {
        return jpaAttempt.transacted(() -> {
            context.define(creatingUser);
            final var newPerson = HsOfficePersonEntity.builder()
                    .uuid(UUID.randomUUID())
                    .personType(HsOfficePersonType.LEGAL_PERSON)
                    .tradeName("Temp " + Context.getCallerMethodNameFromStackFrame(2))
                    .familyName(RandomStringUtils.randomAlphabetic(10) + "@example.org")
                    .givenName("Temp Given Name " + RandomStringUtils.randomAlphabetic(10))
                    .build();

            return personRepo.save(newPerson).load();
        }).assertSuccessful().returnedValue();
    }

    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createQuery("""
                    DELETE FROM HsOfficePersonEntity p 
                        WHERE p.tradeName LIKE 'Temp %' OR p.givenName LIKE 'Temp %'
                    """).executeUpdate();
        }).assertSuccessful();
    }
}
