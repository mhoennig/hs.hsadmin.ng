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
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.EX_PARTNER;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
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
    HsOfficeRelationRepository relationRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
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
    @Transactional
    class AddPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canAddPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").stream().findFirst().orElseThrow();
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Third").stream().findFirst().orElseThrow();
            final var givenContact = contactRepo.findContactByOptionalCaptionLike("fourth").stream().findFirst().orElseThrow();

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
                                "details": {
                                    "registrationOffice": "Temp Registergericht Aurich",
                                    "registrationNumber": "111111"
                                }
                            }
                            """.formatted(
                                    givenMandantPerson.getUuid(),
                                    givenPerson.getUuid(),
                                    givenContact.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/partners")
                    .then().log().body().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                        {
                            "partnerNumber": 20002,
                            "partnerRel": {
                                "anchor": { "tradeName": "Hostsharing eG" },
                                "holder": { "tradeName": "Third OHG" },
                                "type": "PARTNER",
                                "mark": null,
                                "contact": { "caption": "fourth contact" }
                            },
                            "details": {
                                "registrationOffice": "Temp Registergericht Aurich",
                                "registrationNumber": "111111"
                            }
                        }
                        """))
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
            final var givenContact = contactRepo.findContactByOptionalCaptionLike("fourth").get(0);

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
    @Transactional
    class GetPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var partners = partnerRepo.findAll();
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
                        "partnerNumber": 10001,
                             "partnerRel": {
                                 "anchor": { "tradeName": "Hostsharing eG" },
                                 "holder": { "tradeName": "First GmbH" },
                                 "type": "PARTNER",
                                 "contact": { "caption": "first contact" }
                             },
                             "details": {
                                 "registrationOffice": "Hamburg",
                                 "registrationNumber": "RegNo123456789"
                             }
                         }
                    }
                    """)); // @formatter:on
        }

        @Test
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
                        "partnerRel": {
                            "holder": { "tradeName": "First GmbH" },
                            "contact": { "caption": "first contact" }
                        }
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Transactional
    class PatchPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryPartner() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20011);
            final var givenPartnerRel = givenSomeTemporaryPartnerRel("Third OHG", "third contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "partnerNumber": "20011",
                               "partnerRelUuid": "%s",
                               "details": {
                                   "registrationOffice": "Temp Registergericht Aurich",
                                   "registrationNumber": "222222",
                                   "birthName": "Maja Schmidt",
                                   "birthday": "1938-04-08",
                                   "dateOfDeath": "2022-01-12"
                               }
                             }
                            """.formatted(givenPartnerRel.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                    {
                        "partnerNumber": 20011,
                        "partnerRel": {
                            "anchor": { "tradeName": "Hostsharing eG" },
                            "holder": { "tradeName": "Third OHG" },
                            "type": "PARTNER",
                            "contact": { "caption": "third contact" }
                        },
                        "details": {
                            "registrationOffice": "Temp Registergericht Aurich",
                            "registrationNumber": "222222",
                            "birthName": "Maja Schmidt",
                            "birthPlace": null,
                            "birthday": "1938-04-08",
                            "dateOfDeath": "2022-01-12"
                        }
                    }
                    """));
                // @formatter:on

            // finally, the partner is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartnerNumber()).isEqualTo(givenPartner.getPartnerNumber());
                        assertThat(partner.getPartnerRel().getHolder().getTradeName()).isEqualTo("Third OHG");
                        assertThat(partner.getPartnerRel().getContact().getCaption()).isEqualTo("third contact");
                        assertThat(partner.getDetails().getRegistrationOffice()).isEqualTo("Temp Registergericht Aurich");
                        assertThat(partner.getDetails().getRegistrationNumber()).isEqualTo("222222");
                        assertThat(partner.getDetails().getBirthName()).isEqualTo("Maja Schmidt");
                        assertThat(partner.getDetails().getBirthday()).isEqualTo("1938-04-08");
                        assertThat(partner.getDetails().getDateOfDeath()).isEqualTo("2022-01-12");
                        return true;
                    });
        }

        @Test
        void patchingThePartnerRelCreatesExPartnerRel() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20011);
            final var givenPartnerRel = givenSomeTemporaryPartnerRel("Third OHG", "third contact");

            RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                   "partnerRelUuid": "%s"
                                }
                                """.formatted(givenPartnerRel.getUuid()))
                        .port(port)
                    .when()
                        .patch("http://localhost/api/hs/office/partners/" + givenPartner.getUuid())
                    .then().log().body()
                        .assertThat().statusCode(200);
            // @formatter:on

            // then the partner got actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartnerRel().getHolder().getTradeName()).isEqualTo("Third OHG");
                        assertThat(partner.getPartnerRel().getContact().getCaption()).isEqualTo("third contact");
                        return true;
                    });

            // and an ex-partner-relation got created
            final var anchorpartnerPersonUUid = givenPartner.getPartnerRel().getAnchor().getUuid();
            assertThat(relationRepo.findRelationRelatedToPersonUuidAndRelationType(anchorpartnerPersonUUid, EX_PARTNER))
                    .map(HsOfficeRelationEntity::toShortString)
                    .contains("rel(anchor='LP Hostsharing eG', type='EX_PARTNER', holder='UF Erben Bessler')");
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
                    .body("partnerRel.contact.caption", is(givenPartner.getPartnerRel().getContact().getCaption()));
            // @formatter:on

            // finally, the partner details and only the partner details are actually updated
            assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartnerRel().getContact().getCaption()).isEqualTo(givenPartner.getPartnerRel().getContact().getCaption());
                        assertThat(partner.getDetails().getRegistrationOffice()).isEqualTo("Temp Registergericht Leer");
                        assertThat(partner.getDetails().getRegistrationNumber()).isEqualTo("333333");
                        assertThat(partner.getDetails().getBirthName()).isEqualTo("Maja Schmidt");
                        assertThat(partner.getDetails().getBirthday()).isEqualTo("1938-04-08");
                        assertThat(partner.getDetails().getDateOfDeath()).isEqualTo("2022-01-12");
                        return true;
                    });
        }

    }

    @Nested
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
            assertThat(relationRepo.findByUuid(givenPartner.getPartnerRel().getUuid())).isEmpty();
        }

        @Test
        void contactAdminUser_canNotDeleteRelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20014);
            assertThat(givenPartner.getPartnerRel().getContact().getCaption()).isEqualTo("fourth contact");

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
        void normalUser_canNotDeleteUnrelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(20015);
            assertThat(givenPartner.getPartnerRel().getContact().getCaption()).isEqualTo("fourth contact");

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

    private HsOfficeRelationEntity givenSomeTemporaryPartnerRel(
            final String partnerHolderName,
            final String contactName) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").stream().findFirst().orElseThrow();
            final var givenPerson = personRepo.findPersonByOptionalNameLike(partnerHolderName).stream().findFirst().orElseThrow();
            final var givenContact = contactRepo.findContactByOptionalCaptionLike(contactName).stream().findFirst().orElseThrow();

            final var partnerRel = new HsOfficeRelationEntity();
            partnerRel.setType(HsOfficeRelationType.PARTNER);
            partnerRel.setAnchor(givenMandantPerson);
            partnerRel.setHolder(givenPerson);
            partnerRel.setContact(givenContact);
            em.persist(partnerRel);
            return partnerRel;
        }).assertSuccessful().returnedValue();
    }
    private HsOfficePartnerEntity givenSomeTemporaryPartnerBessler(final Integer partnerNumber) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var partnerRel = em.merge(givenSomeTemporaryPartnerRel("Erben Bessler", "fourth contact"));

            final var newPartner = HsOfficePartnerEntity.builder()
                    .partnerRel(partnerRel)
                    .partnerNumber(partnerNumber)
                    .details(HsOfficePartnerDetailsEntity.builder()
                            .registrationOffice("Temp Registergericht Leer")
                            .registrationNumber("333333")
                            .build())
                    .build();

            return partnerRepo.save(newPartner).load();
        }).assertSuccessful().returnedValue();
    }

    @AfterEach
    void cleanup() {
        cleanupAllNew(HsOfficePartnerEntity.class);

        // TODO: should not be necessary anymore, once it's deleted via after delete trigger
        cleanupAllNew(HsOfficeRelationEntity.class);
    }
}
