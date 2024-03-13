package net.hostsharing.hsadminng.hs.office.partner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
class HsOfficePartnerControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    private static final UUID GIVEN_NON_EXISTING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @LocalServerPort
    private Integer port;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeRelationRepository relationRepository;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    @Accepts({ "Partner:F(Find)" })
    @Transactional
    class ListPartners {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllPartners_ifNoCriteriaGiven() {

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
                            { partnerNumber: 10001 },
                            { partnerNumber: 10002 },
                            { partnerNumber: 10003 },
                            { partnerNumber: 10004 },
                            { partnerNumber: 10010 }
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
            final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "partnerNumber": "20002",
                                "partnerRel": {
                                     "anchorUuid": "%s",
                                     "holderUuid": "%s",
                                     "contactUuid": "%s"
                                },
                                "personUuid": "%s",
                                "contactUuid": "%s",
                                "details": {
                                    "registrationOffice": "Temp Registergericht Aurich",
                                    "registrationNumber": "111111"
                                }
                            }
                            """.formatted(
                                    givenMandantPerson.getUuid(),
                                    givenPerson.getUuid(),
                                    givenContact.getUuid(),
                                    givenPerson.getUuid(),
                                    givenContact.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/partners")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("partnerNumber", is(20002))
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
            final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "partnerNumber": "20003",
                                "partnerRel": {
                                     "anchorUuid": "%s",
                                     "holderUuid": "%s",
                                     "contactUuid": "%s"
                                },
                                "personUuid": "%s",
                                "contactUuid": "%s",
                                "details": {}
                            }
                            """.formatted(
                            givenMandantPerson.getUuid(),
                            givenPerson.getUuid(),
                            GIVEN_NON_EXISTING_UUID,
                            givenPerson.getUuid(),
                            GIVEN_NON_EXISTING_UUID))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find " + HsOfficeContactEntity.class.getName() + " with id " + GIVEN_NON_EXISTING_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddPartner_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var mandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "partnerNumber": "20004",
                                "partnerRel": {
                                    "anchorUuid": "%s",
                                    "holderUuid": "%s",
                                    "contactUuid": "%s"
                                },
                                "personUuid": "%s",
                                "contactUuid": "%s",
                                "details": {}
                            }
                            """.formatted(
                                    mandantPerson.getUuid(),
                                    GIVEN_NON_EXISTING_UUID,
                                    givenContact.getUuid(),
                                    GIVEN_NON_EXISTING_UUID,
                                    givenContact.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find " + HsOfficePersonEntity.class.getName() + " with id " + GIVEN_NON_EXISTING_UUID));
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
            final var givenPartner = givenSomeTemporaryPartnerBessler(20011);
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "partnerNumber": "20011",
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
                    .body("uuid", is(givenPartner.getUuid().toString())) // not patched!
                    .body("partnerNumber", is(givenPartner.getPartnerNumber())) // not patched!
                    .body("details.registrationNumber", is("222222"))
                    .body("contact.label", is(givenContact.getLabel()))
                    .body("person.tradeName", is(givenPerson.getTradeName()));
                // @formatter:on

            // finally, the partner is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartnerNumber()).isEqualTo(givenPartner.getPartnerNumber());
                        assertThat(partner.getPerson().getTradeName()).isEqualTo("Third OHG");
                        assertThat(partner.getContact().getLabel()).isEqualTo("fourth contact");
                        assertThat(partner.getDetails().getRegistrationOffice()).isEqualTo("Temp Registergericht Aurich");
                        assertThat(partner.getDetails().getRegistrationNumber()).isEqualTo("222222");
                        assertThat(partner.getDetails().getBirthName()).isEqualTo("Maja Schmidt");
                        assertThat(partner.getDetails().getBirthday()).isEqualTo("1938-04-08");
                        assertThat(partner.getDetails().getDateOfDeath()).isEqualTo("2022-01-12");
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20012);

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
            final var givenPartner = givenSomeTemporaryPartnerBessler(20013);

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
            assertThat(relationRepository.findByUuid(givenPartner.getPartnerRel().getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void contactAdminUser_canNotDeleteRelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20014);
            assertThat(givenPartner.getContact().getLabel()).isEqualTo("fourth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@fourthcontact.example.com")
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
            final var givenPartner = givenSomeTemporaryPartnerBessler(20015);
            assertThat(givenPartner.getContact().getLabel()).isEqualTo("fourth contact");

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

    private HsOfficePartnerEntity givenSomeTemporaryPartnerBessler(final Integer partnerNumber) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);

            final var givenPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth contact").get(0);

            final var partnerRel = new HsOfficeRelationEntity();
            partnerRel.setType(HsOfficeRelationType.PARTNER);
            partnerRel.setAnchor(givenMandantPerson);
            partnerRel.setHolder(givenPerson);
            partnerRel.setContact(givenContact);
            em.persist(partnerRel);

            final var newPartner = HsOfficePartnerEntity.builder()
                    .partnerRel(partnerRel)
                    .partnerNumber(partnerNumber)
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
        cleanupAllNew(HsOfficePartnerEntity.class);

        // TODO: should not be necessary anymore, once it's deleted via after delete trigger
        cleanupAllNew(HsOfficeRelationEntity.class);
    }
}
