package net.hostsharing.hsadminng.hs.office.debitor;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
class HsOfficeDebitorControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    private static final int LOWEST_TEMP_DEBITOR_SUFFIX = 90;
    private static byte nextDebitorSuffix = LOWEST_TEMP_DEBITOR_SUFFIX;

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeContactRealRepository contactRealRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    HsOfficePersonRealRepository personRealRepo;

    @Autowired
    HsOfficeRelationRealRepository relationRealRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class GetSingleDebitor {

        @Test
        void globalAdmin_withoutAssumedRoles_canGetDebitorByDebitorUuid() {

            final var givenDebitor = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.findDebitorByDebitorNumber(1000212).orElseThrow();
            }).assertSuccessful().returnedValue();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "debitorNumber": "D-1000212",
                            "partner": { "partnerNumber": "P-10002" },
                            "debitorRel": {
                               "contact": { "caption": "second contact" }
                            },
                            "vatId": null,
                            "vatCountryCode": null,
                            "vatBusiness": true
                        }
                        """));
            // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRoles_canGetDebitorByDebitorNumber() {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/D-1000212")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                         {
                             "debitorNumber": "D-1000212",
                             "partner": { "partnerNumber": "P-10002" },
                             "debitorRel": {
                                "contact": { "caption": "second contact" }
                             },
                             "vatId": null,
                             "vatCountryCode": null,
                             "vatBusiness": true
                         }
                        """));
            // @formatter:on
        }
    }

    @Nested
    class GetListOfDebitors {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllDebitors_ifNoCriteriaGiven() {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                      {
                        "debitorRel": {
                          "anchor": {
                            "personType": "LEGAL_PERSON",
                            "tradeName": "First GmbH",
                            "givenName": null,
                            "familyName": null
                          },
                          "holder": {
                            "personType": "LEGAL_PERSON",
                            "tradeName": "First GmbH",
                            "givenName": null,
                            "familyName": null
                          },
                          "type": "DEBITOR",
                          "mark": null,
                          "contact": {
                            "caption": "first contact",
                            "emailAddresses": { "main": "contact-admin@firstcontact.example.com" },
                            "phoneNumbers": { "phone_office": "+49 123 1234567" }
                          }
                        },
                        "debitorNumber": "D-1000111",
                        "debitorNumberSuffix": "11",
                        "partner": {
                          "partnerNumber": "P-10001",
                          "partnerRel": {
                            "anchor": {
                              "personType": "LEGAL_PERSON",
                              "tradeName": "Hostsharing eG",
                              "givenName": null,
                              "familyName": null
                            },
                            "holder": {
                              "personType": "LEGAL_PERSON",
                              "tradeName": "First GmbH",
                              "givenName": null,
                              "familyName": null
                            },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": {
                              "caption": "first contact",
                              "emailAddresses": { "main": "contact-admin@firstcontact.example.com" },
                              "phoneNumbers": { "phone_office": "+49 123 1234567" }
                            }
                          },
                          "details": {
                            "registrationOffice": "Hamburg",
                            "registrationNumber": "RegNo123456789",
                            "birthName": null,
                            "birthPlace": null,
                            "birthday": null,
                            "dateOfDeath": null
                          }
                        },
                        "billable": true,
                        "vatId": null,
                        "vatCountryCode": null,
                        "vatBusiness": true,
                        "vatReverseCharge": false,
                        "refundBankAccount": {
                          "holder": "First GmbH",
                          "iban": "DE02120300000000202051",
                          "bic": "BYLADEM1001"
                        },
                        "defaultPrefix": "fir"
                      },
                      {
                        "debitorRel": {
                          "anchor": {"tradeName": "Second e.K."},
                          "holder": {"tradeName": "Second e.K."},
                          "type": "DEBITOR",
                          "contact": {
                            "emailAddresses": { "main": "contact-admin@secondcontact.example.com" }
                           }
                        },
                        "debitorNumber": "D-1000212",
                        "debitorNumberSuffix": "12",
                        "partner": {
                          "partnerNumber": "P-10002",
                          "partnerRel": {
                            "anchor": {"tradeName": "Hostsharing eG"},
                            "holder": {"tradeName": "Second e.K."},
                            "type": "PARTNER",
                            "contact": {
                                "emailAddresses": { "main": "contact-admin@secondcontact.example.com" }
                            }
                          },
                          "details": {
                            "registrationOffice": "Hamburg",
                            "registrationNumber": "RegNo123456789"
                          }
                        },
                        "billable": true,
                        "vatId": null,
                        "vatCountryCode": null,
                        "vatBusiness": true,
                        "vatReverseCharge": false,
                        "refundBankAccount": {"iban": "DE02100500000054540402"},
                        "defaultPrefix": "sec"
                      },
                      {
                        "debitorRel": {
                          "anchor": {"tradeName": "Third OHG"},
                          "holder": {"tradeName": "Third OHG"},
                          "type": "DEBITOR",
                          "contact": {
                                "emailAddresses": { "main": "contact-admin@thirdcontact.example.com" }
                            }
                        },
                        "debitorNumber": "D-1000313",
                        "debitorNumberSuffix": "13",
                        "partner": {
                          "partnerNumber": "P-10003",
                          "partnerRel": {
                            "anchor": {"tradeName": "Hostsharing eG"},
                            "holder": {"tradeName": "Third OHG"},
                            "type": "PARTNER",
                            "contact": {
                                "emailAddresses": { "main": "contact-admin@thirdcontact.example.com" }
                            }
                          },
                          "details": {
                            "registrationOffice": "Hamburg",
                            "registrationNumber": "RegNo123456789"
                          }
                        },
                        "billable": true,
                        "vatId": null,
                        "vatCountryCode": null,
                        "vatBusiness": true,
                        "vatReverseCharge": false,
                        "refundBankAccount": {"iban": "DE02300209000106531065"},
                        "defaultPrefix": "thi"
                      }
                    ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRoles_canFindDebitorsByPartnerNumber() {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors?partnerNumber=P-10002")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "debitorNumber": "D-1000212",
                            "partner": {
                                "partnerNumber": "P-10002"
                            }
                        }
                    ]
                    """));
                    // @formatter:on
        }
    }

    @Nested
    class PostNewDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canAddDebitorWithBankAccount() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Fourth").get(0);
            final var givenBillingPerson = personRealRepo.findPersonByOptionalNameLike("Fourth").get(0);

            final var givenDebitorRelUUid = jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                return relationRealRepo.save(HsOfficeRelationRealEntity.builder()
                        .type(DEBITOR)
                        .anchor(givenPartner.getPartnerRel().getHolder())
                        .holder(givenBillingPerson)
                        .contact(givenContact)
                        .build()).getUuid();
            }).assertSuccessful().returnedValue();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "debitorRel.uuid": "%s",
                                   "debitorNumberSuffix": "%s",
                                   "billable": "true",
                                   "vatId": "VAT123456",
                                   "vatCountryCode": "DE",
                                   "vatBusiness": true,
                                   "vatReverseCharge": "false",
                                   "refundBankAccount.uuid": "%s",
                                   "defaultPrefix": "for"
                                 }
                            """.formatted( givenDebitorRelUUid, ++nextDebitorSuffix, givenBankAccount.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/debitors")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("vatId", is("VAT123456"))
                        .body("defaultPrefix", is("for"))
                        .body("debitorRel.contact.caption", is(givenContact.getCaption()))
                        .body("debitorRel.holder.tradeName", is(givenBillingPerson.getTradeName()))
                        .body("refundBankAccount.holder", is(givenBankAccount.getHolder()))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new debitor can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }

        @Test
        void globalAdmin_canAddDebitorWithoutJustRequiredData() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                               "debitorRel": {
                                    "anchor.uuid": "%s",
                                    "holder.uuid": "%s",
                                    "contact.uuid": "%s"
                                },
                                "debitorNumberSuffix": "%s",
                                "defaultPrefix": "for",
                                "billable": "true",
                                "vatReverseCharge": "false"
                            }
                            """.formatted(
                                givenPartner.getPartnerRel().getHolder().getUuid(),
                                givenPartner.getPartnerRel().getHolder().getUuid(),
                                givenContact.getUuid(),
                                ++nextDebitorSuffix))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("debitorRel.contact.caption", is(givenContact.getCaption()))
                    .body("partner.partnerRel.holder.tradeName", is(givenPartner.getPartnerRel().getHolder().getTradeName()))
                    .body("vatId", equalTo(null))
                    .body("vatCountryCode", equalTo(null))
                    .body("vatBusiness", equalTo(false))
                    .body("refundBankAccount", equalTo(null))
                    .body("defaultPrefix", equalTo("for"))
                    .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new debitor can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddDebitor_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContactUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                               "debitorRel": {
                                    "anchor.uuid": "%s",
                                    "holder.uuid": "%s",
                                    "contact.uuid": "%s"
                                },
                                "debitorNumberSuffix": "%s",
                                "defaultPrefix": "for",
                                "billable": "true",
                                "vatReverseCharge": "false"
                            }
                            """.formatted(
                                givenPartner.getPartnerRel().getAnchor().getUuid(),
                                givenPartner.getPartnerRel().getAnchor().getUuid(),
                                givenContactUuid, ++nextDebitorSuffix))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("ERROR: [400] Unable to find RealContact by debitorRel.contactUuid: 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddDebitor_ifDebitorRelDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorRelUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "debitorRel.uuid": "%s",
                                "debitorNumberSuffix": "%s",
                                "defaultPrefix": "for",
                                "billable": "true",
                                "vatReverseCharge": "false"
                            }
                            """.formatted(givenDebitorRelUuid, ++nextDebitorSuffix))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("ERROR: [400] Unable to find RealRelation by debitorRelUuid: 00000000-0000-0000-0000-000000000000"));
                // @formatter:on
        }
    }

    @Nested
    class GetSingleDebitorByUuid {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorsByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "debitorRel": {
                             "anchor": { "personType": "LEGAL_PERSON", "tradeName": "First GmbH"},
                             "holder": { "personType": "LEGAL_PERSON", "tradeName": "First GmbH"},
                             "type": "DEBITOR",
                             "contact": {
                                 "caption": "first contact",
                                     "postalAddress": {
                                        "country": "Germany"
                                     },
                                     "emailAddresses": { "main": "contact-admin@firstcontact.example.com" },
                                     "phoneNumbers": { "phone_office":  "+49 123 1234567" }
                                 }
                             }
                         },
                         "debitorNumber": 1000111,
                         "debitorNumberSuffix": "11",
                         "partner": {
                             "partnerNumber": 10001,
                             "partnerRel": {
                                 "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG"},
                                 "holder": { "personType": "LEGAL_PERSON", "tradeName": "First GmbH"},
                                 "type": "PARTNER",
                                 "mark": null,
                                 "contact": {
                                     "caption": "first contact",
                                 "postalAddress": {
                                    "country": "Germany"
                                 },
                                 "emailAddresses": { "main": "contact-admin@firstcontact.example.com" },
                                 "phoneNumbers": { "phone_office": "+49 123 1234567" }
                             },
                             "details": {
                                 "registrationOffice": "Hamburg",
                                 "registrationNumber": "RegNo123456789"
                             }
                         },
                         "billable": true,
                         "vatBusiness": true,
                         "vatReverseCharge": false,
                         "refundBankAccount": {
                             "holder": "First GmbH",
                             "iban": "DE02120300000000202051",
                             "bic": "BYLADEM1001"
                         },
                         "defaultPrefix": "fir"
                     }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorsByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void contactAdminUser_canGetRelatedDebitorExceptRefundBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorsByOptionalNameLike("first contact").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "debitorNumber": "D-1000111",
                        "partner": { "partnerNumber": "P-10001" },
                        "debitorRel": { "contact": { "caption": "first contact" } },
                        "refundBankAccount": null
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchArbitraryDebitor() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "contactUuid": "%s",
                                   "vatId": "VAT222222",
                                   "vatCountryCode": "AA",
                                   "vatBusiness": true,
                                   "defaultPrefix": "for"
                                 }
                            """.formatted(givenContact.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                            {
                                "debitorRel": {
                                    "anchor": { "tradeName": "Fourth eG" },
                                    "holder": { "tradeName": "Fourth eG" },
                                    "type": "DEBITOR",
                                    "mark": null,
                                    "contact": { "caption": "fourth contact" }
                                },
                                "debitorNumber": "D-10004${debitorNumberSuffix}",
                                "debitorNumberSuffix": "${debitorNumberSuffix}",
                                "partner": {
                                    "partnerNumber": "P-10004",
                                    "partnerRel": {
                                        "anchor": { "tradeName": "Hostsharing eG" },
                                        "holder": { "tradeName": "Fourth eG" },
                                        "type": "PARTNER",
                                        "mark": null,
                                        "contact": { "caption": "fourth contact" }
                                    },
                                    "details": {
                                        "registrationOffice": "Hamburg",
                                        "registrationNumber": "RegNo123456789",
                                        "birthName": null,
                                        "birthPlace": null,
                                        "birthday": null,
                                        "dateOfDeath": null
                                    }
                                },
                                "billable": true,
                                "vatId": "VAT222222",
                                "vatCountryCode": "AA",
                                "vatBusiness": true,
                                "vatReverseCharge": false,
                                "defaultPrefix": "for"
                            }
                            """
                            .replace("${debitorNumberSuffix}", givenDebitor.getDebitorNumberSuffix()))
                    );
                // @formatter:on

            // finally, the debitor is actually updated
            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net");
                assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isPresent().get()
                        .matches(debitor -> {
                            assertThat(debitor.getDebitorRel().getHolder().getTradeName())
                                    .isEqualTo(givenDebitor.getDebitorRel().getHolder().getTradeName());
                            assertThat(debitor.getDebitorRel().getContact().getCaption()).isEqualTo("fourth contact");
                            assertThat(debitor.getVatId()).isEqualTo("VAT222222");
                            assertThat(debitor.getVatCountryCode()).isEqualTo("AA");
                            assertThat(debitor.isVatBusiness()).isEqualTo(true);
                            return true;
                        });
            }).assertSuccessful();
        }

        @Test
        void theContactOwner_canNotPatchARelatedDebitor() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();

            // @formatter:on
            RestAssured // @formatter:off
                    .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office.contact#fourthcontact:ADMIN")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "vatId": "VAT999999"
                             }
                        """)
                    .port(port)
                    .when()
                    .patch("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                    .then().log().all().assertThat()
                    .statusCode(403)
                    .body("message", containsString("ERROR: [403] Subject"))
                    .body("message", containsString("is not allowed to update hs_office.debitor uuid "));

        }
    }

    @Nested
    class DeleteDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given debitor is gone
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isEmpty();
        }

        @Test
        void contactAdminUser_canNotDeleteRelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            assertThat(givenDebitor.getDebitorRel().getContact().getCaption()).isEqualTo("fourth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "contact-admin@fourthcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given debitor is still there
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isNotEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            assertThat(givenDebitor.getDebitorRel().getContact().getCaption()).isEqualTo("fourth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given debitor is still there
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeDebitorEntity givenSomeTemporaryDebitor() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Fourth").get(0).load();
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").get(0);
            final var newDebitor = HsOfficeDebitorEntity.builder()
                    .debitorNumberSuffix(nextDebitorSuffix())
                    .billable(true)
                    .debitorRel(
                            HsOfficeRelationRealEntity.builder()
                                    .type(DEBITOR)
                                    .anchor(givenPartner.getPartnerRel().getHolder())
                                    .holder(givenPartner.getPartnerRel().getHolder())
                                    .contact(givenContact)
                                    .build()
                    )
                    .defaultPrefix("abc")
                    .vatReverseCharge(false)
                    .build();

            return debitorRepo.save(newDebitor).load();
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var count = em.createQuery(
                            "DELETE FROM HsBookingDebitorEntity d WHERE d.debitorNumberSuffix >= " + LOWEST_TEMP_DEBITOR_SUFFIX)
                    .executeUpdate();
            System.out.printf("deleted %d entities%n", count);
        });
    }

    private String nextDebitorSuffix() {
        return String.format("%02d", nextDebitorSuffix++);
    }
}
