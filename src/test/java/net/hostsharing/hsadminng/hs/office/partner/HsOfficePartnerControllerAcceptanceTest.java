package net.hostsharing.hsadminng.hs.office.partner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
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

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
class HsOfficePartnerControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    @Accepts({ "Partner:F(Find)" })
    @Transactional
    class ListPartners {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllPartners_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "person": { "familyName": "Smith" },
                            "contact": { "label": "fifth contact" },
                            "details": { "birthday": "1987-10-31" }
                        },
                        {
                            "person": { "tradeName": "First GmbH" },
                            "contact": { "label": "first contact" },
                            "details": { "registrationOffice": "Hamburg" }
                        },
                        {
                            "person": { "tradeName": "Third OHG" },
                            "contact": { "label": "third contact" },
                            "details": { "registrationOffice": "Hamburg" }
                        },
                        {
                            "person": { "tradeName": "Second e.K." },
                            "contact": { "label": "second contact" },
                            "details": { "registrationOffice": "Hamburg" }
                        },
                        {
                            "person": { "personType": "INCORPORATED_FIRM" },
                            "contact": { "label": "forth contact" },
                            "details": { "registrationOffice": "Hamburg" }
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:C(Create)" })
    @Transactional
    class AddPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canAddPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "partnerNumber": "12345",
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "details": {
                                       "registrationOffice": "Temp Registergericht Aurich",
                                       "registrationNumber": "111111"
                                   }
                                 }
                            """.formatted(givenContact.getUuid(), givenPerson.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/partners")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("details.registrationOffice", is("Temp Registergericht Aurich"))
                        .body("details.registrationNumber", is("111111"))
                        .body("contact.label", is(givenContact.getLabel()))
                        .body("person.tradeName", is(givenPerson.getTradeName()))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new partner can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddPartner_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContactUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "partnerNumber": "12345",
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "details": {}
                                 }
                            """.formatted(givenContactUuid, givenPerson.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Contact with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddPartner_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPersonUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "partnerNumber": "12345",
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "details": {}
                                 }
                            """.formatted(givenContact.getUuid(), givenPersonUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Person with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:R(Read)" })
    @Transactional
    class GetPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "person": { "tradeName": "First GmbH" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void normalUser_canNotGetUnrelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void contactAdminUser_canGetRelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("first contact").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "person": { "tradeName": "First GmbH" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:U(Update)" })
    @Transactional
    class PatchPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler();
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitorNumerPrefix": "12345",
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "details": {
                                       "registrationOffice": "Temp Registergericht Aurich",
                                       "registrationNumber": "222222",
                                       "birthName": "Maja Schmidt",
                                       "birthday": "1938-04-08",
                                       "dateOfDeath": "2022-01-12"
                                   }
                                 }
                            """.formatted(givenContact.getUuid(), givenPerson.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("details.registrationNumber", is("222222"))
                    .body("contact.label", is(givenContact.getLabel()))
                    .body("person.tradeName", is(givenPerson.getTradeName()));
                // @formatter:on

            // finally, the partner is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPerson().getTradeName()).isEqualTo("Third OHG");
                        assertThat(person.getContact().getLabel()).isEqualTo("forth contact");
                        assertThat(person.getDetails().getRegistrationOffice()).isEqualTo("Temp Registergericht Aurich");
                        assertThat(person.getDetails().getRegistrationNumber()).isEqualTo("222222");
                        assertThat(person.getDetails().getBirthName()).isEqualTo("Maja Schmidt");
                        assertThat(person.getDetails().getBirthday()).isEqualTo("1938-04-08");
                        assertThat(person.getDetails().getDateOfDeath()).isEqualTo("2022-01-12");
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                    "details": {
                                       "birthName": "Maja Schmidt",
                                       "birthday": "1938-04-08",
                                       "dateOfDeath": "2022-01-12"
                                    }
                                 }
                            """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("details.birthName", is("Maja Schmidt"))
                    .body("contact.label", is(givenPartner.getContact().getLabel()))
                    .body("person.tradeName", is(givenPartner.getPerson().getTradeName()));
            // @formatter:on

            // finally, the partner is actually updated
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getPerson().getTradeName()).isEqualTo(givenPartner.getPerson().getTradeName());
                        assertThat(person.getContact().getLabel()).isEqualTo(givenPartner.getContact().getLabel());
                        assertThat(person.getDetails().getRegistrationOffice()).isEqualTo("Temp Registergericht Leer");
                        assertThat(person.getDetails().getRegistrationNumber()).isEqualTo("333333");
                        assertThat(person.getDetails().getBirthName()).isEqualTo("Maja Schmidt");
                        assertThat(person.getDetails().getBirthday()).isEqualTo("1938-04-08");
                        assertThat(person.getDetails().getDateOfDeath()).isEqualTo("2022-01-12");
                        return true;
                    });
        }

    }

    @Nested
    @Accepts({ "Partner:D(Delete)" })
    @Transactional
    class DeletePartner {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given partner is gone
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void contactAdminUser_canNotDeleteRelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler();
            assertThat(givenPartner.getContact().getLabel()).isEqualTo("forth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@forthcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given partner is still there
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler();
            assertThat(givenPartner.getContact().getLabel()).isEqualTo("forth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given partner is still there
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isNotEmpty();
        }
    }

    private HsOfficePartnerEntity givenSomeTemporaryPartnerBessler() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
            final var newPartner = HsOfficePartnerEntity.builder()
                    .person(givenPerson)
                    .contact(givenContact)
                    .details(HsOfficePartnerDetailsEntity.builder()
                            .registrationOffice("Temp Registergericht Leer")
                            .registrationNumber("333333")
                            .build())
                    .build();

            return partnerRepo.save(newPartner);
        }).assertSuccessful().returnedValue();
    }

    @AfterEach
    void cleanup() {
        final var deleted = jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createNativeQuery("""
                            delete from hs_office_partner p
                                where p.detailsuuid in (
                                    select d.uuid from hs_office_partner_details d
                                        where d.registrationoffice like 'Temp %')
                            """)
                    .executeUpdate();
        }).assertSuccessful().returnedValue();

        final var remaining = jpaAttempt.transacted(() -> {
            em.createNativeQuery("""
                            select count(p) from hs_office_partner p
                                where p.detailsuuid in (
                                    select d.uuid from hs_office_partner_details d
                                        where d.registrationoffice like 'Temp %')
                            """)
                    .getSingleResult();
        }).assertSuccessful().returnedValue();
        System.err.println("@AfterEach" + ": " + deleted + " records deleted, " + remaining + " remaining");
    }

}
