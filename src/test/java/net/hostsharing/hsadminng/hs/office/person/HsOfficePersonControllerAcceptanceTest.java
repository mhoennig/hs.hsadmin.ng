package net.hostsharing.hsadminng.hs.office.person;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficePersonControllerAcceptanceTest {

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

    Set<UUID> tempPersonUuids = new HashSet<>();

    @Nested
    @Accepts({ "Person:F(Find)" })
    class ListPersons {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllPersons_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/persons")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                             {
                                 "personType": "JOINT_REPRESENTATION",
                                 "tradeName": "Erben Bessler",
                                 "givenName": "Bessler",
                                 "familyName": "Mel"
                             },
                             {
                                 "personType": "LEGAL",
                                 "tradeName": "First Impressions GmbH",
                                 "givenName": null,
                                 "familyName": null
                             },
                             {
                                 "personType": "SOLE_REPRESENTATION",
                                 "tradeName": "Ostfriesische Kuhhandel OHG",
                                 "givenName": null,
                                 "familyName": null
                             },
                             {
                                 "personType": "NATURAL",
                                 "tradeName": null,
                                 "givenName": "Smith",
                                 "familyName": "Peter"
                             },
                             {
                                 "personType": "LEGAL",
                                 "tradeName": "Rockshop e.K.",
                                 "givenName": "Miller",
                                 "familyName": "Sandra"
                             }
                         ]
                        """
                            ));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Person:C(Create)" })
    class AddPerson {

        @Test
        void globalAdmin_withoutAssumedRole_canAddPerson() {

            context.define("superuser-alex@hostsharing.net");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "personType": "NATURAL",
                                   "familyName": "Tester",
                                   "givenName": "Testi"
                                 }
                            """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/persons")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("personType", is("NATURAL"))
                        .body("familyName", is("Tester"))
                        .body("givenName", is("Testi"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new person can be accessed under the generated UUID
            final var newUserUuid = toCleanup(UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    @Accepts({ "Person:R(Read)" })
    class GetPerson {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryPerson() {
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
        @Accepts({ "Person:X(Access Control)" })
        void normalUser_canNotGetUnrelatedPerson() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPersonUuid = personRepo.findPersonByOptionalNameLike("Erben").get(0).getUuid();

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
        @Accepts({ "Person:X(Access Control)" })
        void personOwnerUser_canGetRelatedPerson() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPersonUuid = personRepo.findPersonByOptionalNameLike("Erben").get(0).getUuid();

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
                        "personType": "JOINT_REPRESENTATION",
                        "tradeName": "Erben Bessler",
                        "givenName": "Bessler",
                        "familyName": "Mel"
                     }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Person:U(Update)" })
    class PatchPerson {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryPerson() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "personType": "JOINT_REPRESENTATION",
                           "tradeName": "Patched Trade Name",
                           "familyName": "Patched Family Name",
                           "givenName": "Patched Given Name"
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("personType", is("JOINT_REPRESENTATION"))
                    .body("tradeName", is("Patched Trade Name"))
                    .body("familyName", is("Patched Family Name"))
                    .body("givenName", is("Patched Given Name"));
                // @formatter:on

            // finally, the person is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPersonType()).isEqualTo(HsOfficePersonType.JOINT_REPRESENTATION);
                        assertThat(person.getTradeName()).isEqualTo("Patched Trade Name");
                        assertThat(person.getFamilyName()).isEqualTo("Patched Family Name");
                        assertThat(person.getGivenName()).isEqualTo("Patched Given Name");
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryPerson() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = givenSomeTemporaryPersonCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "familyName": "Patched Family Name",
                            "givenName": "Patched Given Name"
                        }
                        """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/persons/" + givenPerson.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("personType", is(givenPerson.getPersonType().toString()))
                    .body("tradeName", is(givenPerson.getTradeName()))
                    .body("familyName", is("Patched Family Name"))
                    .body("givenName", is("Patched Given Name"));
            // @formatter:on

            // finally, the person is actually updated
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPersonType()).isEqualTo(givenPerson.getPersonType());
                        assertThat(person.getTradeName()).isEqualTo(givenPerson.getTradeName());
                        assertThat(person.getFamilyName()).isEqualTo("Patched Family Name");
                        assertThat(person.getGivenName()).isEqualTo("Patched Given Name");
                        return true;
                    });
        }
    }

    @Nested
    @Accepts({ "Person:D(Delete)" })
    class DeletePerson {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryPerson() {
            context.define("superuser-alex@hostsharing.net");
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
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Person:X(Access Control)" })
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
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Person:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedPerson() {
            context.define("superuser-alex@hostsharing.net");
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
            assertThat(personRepo.findByUuid(givenPerson.getUuid())).isNotEmpty();
        }
    }

    private HsOfficePersonEntity givenSomeTemporaryPersonCreatedBy(final String creatingUser) {
        return jpaAttempt.transacted(() -> {
            context.define(creatingUser);
            final var newPerson = HsOfficePersonEntity.builder()
                    .uuid(UUID.randomUUID())
                    .personType(HsOfficePersonType.LEGAL)
                    .tradeName("Temp " + Context.getCallerMethodNameFromStackFrame(2))
                    .familyName(RandomStringUtils.randomAlphabetic(10) + "@example.org")
                    .givenName("Given Name " + RandomStringUtils.randomAlphabetic(10))
                    .build();

            toCleanup(newPerson.getUuid());

            return personRepo.save(newPerson);
        }).assertSuccessful().returnedValue();
    }

    private UUID toCleanup(final UUID tempPersonUuid) {
        tempPersonUuids.add(tempPersonUuid);
        return tempPersonUuid;
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        tempPersonUuids.forEach(uuid -> {
            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net", null);
                System.out.println("DELETING temporary person: " + uuid);
                final var entity = personRepo.findByUuid(uuid);
                final var count = personRepo.deleteByUuid(uuid);
                System.out.println("DELETED temporary person: " + uuid + (count > 0 ? " successful" : " failed") +
                        (" (" + entity.map(HsOfficePersonEntity::getDisplayName).orElse("null") + ")"));
            }).assertSuccessful();
        });
    }
}
