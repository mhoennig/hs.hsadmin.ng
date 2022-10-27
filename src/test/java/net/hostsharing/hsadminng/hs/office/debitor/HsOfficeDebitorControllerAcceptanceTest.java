package net.hostsharing.hsadminng.hs.office.debitor;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeDebitorControllerAcceptanceTest {

    private static final int LOWEST_TEMP_DEBITOR_NUMBER = 20000;
    private static int nextDebitorNumber = LOWEST_TEMP_DEBITOR_NUMBER;

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    @Accepts({ "Debitor:F(Find)" })
    class ListDebitors {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllDebitors_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                         {
                             "debitorNumber": 10001,
                             "partner": { "person": { "personType": "LEGAL" } },
                             "billingContact": { "label": "first contact" },
                             "vatId": null,
                             "vatCountryCode": null,
                             "vatBusiness": true,
                             "refundBankAccount": { "holder": "First GmbH" }
                         },
                         {
                             "debitorNumber": 10002,
                             "partner": { "person": { "tradeName": "Second e.K." } },
                             "billingContact": { "label": "second contact" },
                             "vatId": null,
                             "vatCountryCode": null,
                             "vatBusiness": true,
                             "refundBankAccount": { "holder": "Second e.K." }
                         },
                         {
                             "debitorNumber": 10003,
                             "partner": { "person": { "tradeName": "Third OHG" } },
                             "billingContact": { "label": "third contact" },
                             "vatId": null,
                             "vatCountryCode": null,
                             "vatBusiness": true,
                             "refundBankAccount": { "holder": "Third OHG" }
                         }
                     ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRoles_canFindDebitorDebitorByDebitorNumber() throws JSONException {

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/office/debitors?debitorNumber=10002")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                     [
                         {
                             "debitorNumber": 10002,
                             "partner": { "person": { "tradeName": "Second e.K." } },
                             "billingContact": { "label": "second contact" },
                             "vatId": null,
                             "vatCountryCode": null,
                             "vatBusiness": true
                         }
                     ]
                    """));
            // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Debitor:C(Create)" })
    class CreateDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canAddDebitorWithBankAccount() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Fourth").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "partnerUuid": "%s",
                                   "billingContactUuid": "%s",
                                   "debitorNumber": "%s",
                                   "vatId": "VAT123456",
                                   "vatCountryCode": "DE",
                                   "vatBusiness": true,
                                   "refundBankAccountUuid": "%s"
                                 }
                            """.formatted( givenPartner.getUuid(), givenContact.getUuid(), ++nextDebitorNumber, givenBankAccount.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/debitors")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("vatId", is("VAT123456"))
                        .body("billingContact.label", is(givenContact.getLabel()))
                        .body("partner.person.tradeName", is(givenPartner.getPerson().getTradeName()))
                        .body("refundBankAccount.holder", is(givenBankAccount.getHolder()))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new debitor can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canAddDebitorWithoutJustRequiredData() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "partnerUuid": "%s",
                                   "billingContactUuid": "%s",
                                   "debitorNumber": "%s"
                                 }
                            """.formatted( givenPartner.getUuid(), givenContact.getUuid(), ++nextDebitorNumber))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("billingContact.label", is(givenContact.getLabel()))
                    .body("partner.person.tradeName", is(givenPartner.getPerson().getTradeName()))
                    .body("vatId", equalTo(null))
                    .body("vatCountryCode", equalTo(null))
                    .body("vatBusiness", equalTo(false))
                    .body("refundBankAccount", equalTo(null))
                    .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new debitor can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddDebitor_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenContactUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "partnerUuid": "%s",
                                   "billingContactUuid": "%s",
                                   "debitorNumber": "%s",
                                   "vatId": "VAT123456",
                                   "vatCountryCode": "DE",
                                   "vatBusiness": true
                                 }
                            """.formatted( givenPartner.getUuid(), givenContactUuid, ++nextDebitorNumber))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Contact with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddDebitor_ifPartnerDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "partnerUuid": "%s",
                                   "billingContactUuid": "%s",
                                   "debitorNumber": "%s",
                                   "vatId": "VAT123456",
                                   "vatCountryCode": "DE",
                                   "vatBusiness": true
                                 }
                            """.formatted( givenPartnerUuid, givenContact.getUuid(), ++nextDebitorNumber))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/debitors")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Partner with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Debitor:R(Read)" })
    class GetDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "partner": { person: { "tradeName": "First GmbH" } },
                        "billingContact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Debitor:X(Access Control)" })
        void normalUser_canNotGetUnrelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Debitor:X(Access Control)" })
        void contactAdminUser_canGetRelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = debitorRepo.findDebitorByOptionalNameLike("first contact").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/debitors/" + givenDebitorUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "partner": { person: { "tradeName": "First GmbH" } },
                        "billingContact": { "label": "first contact" },
                        "refundBankAccount": { "holder": "First GmbH" }
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Debitor:U(Update)" })
    class PatchDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryDebitor() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "contactUuid": "%s",
                                   "vatId": "VAT222222",
                                   "vatCountryCode": "AA",
                                   "vatBusiness": true
                                 }
                            """.formatted(givenContact.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("vatId", is("VAT222222"))
                    .body("vatCountryCode", is("AA"))
                    .body("vatBusiness", is(true))
                    .body("billingContact.label", is(givenContact.getLabel()))
                    .body("partner.person.tradeName", is(givenDebitor.getPartner().getPerson().getTradeName()));
                // @formatter:on

            // finally, the debitor is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartner().getPerson().getTradeName()).isEqualTo(givenDebitor.getPartner()
                                .getPerson()
                                .getTradeName());
                        assertThat(partner.getBillingContact().getLabel()).isEqualTo("forth contact");
                        assertThat(partner.getVatId()).isEqualTo("VAT222222");
                        assertThat(partner.getVatCountryCode()).isEqualTo("AA");
                        assertThat(partner.isVatBusiness()).isEqualTo(true);
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryDebitor() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            final var newBillingContact = contactRepo.findContactByOptionalLabelLike("sixth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "billingContactUuid": "%s",
                                   "vatId": "VAT999999"
                                 }
                            """.formatted(newBillingContact.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("billingContact.label", is("sixth contact"))
                    .body("vatId", is("VAT999999"))
                    .body("vatCountryCode", is(givenDebitor.getVatCountryCode()))
                    .body("vatBusiness", is(givenDebitor.isVatBusiness()));
            // @formatter:on

            // finally, the debitor is actually updated
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isPresent().get()
                    .matches(partner -> {
                        assertThat(partner.getPartner().getPerson().getTradeName()).isEqualTo(givenDebitor.getPartner()
                                .getPerson()
                                .getTradeName());
                        assertThat(partner.getBillingContact().getLabel()).isEqualTo("sixth contact");
                        assertThat(partner.getVatId()).isEqualTo("VAT999999");
                        assertThat(partner.getVatCountryCode()).isEqualTo(givenDebitor.getVatCountryCode());
                        assertThat(partner.isVatBusiness()).isEqualTo(givenDebitor.isVatBusiness());
                        return true;
                    });
        }

    }

    @Nested
    @Accepts({ "Debitor:D(Delete)" })
    class DeleteDebitor {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given debitor is gone
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Debitor:X(Access Control)" })
        void contactAdminUser_canNotDeleteRelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            assumeThat(givenDebitor.getBillingContact().getLabel()).isEqualTo("forth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@forthcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/debitors/" + givenDebitor.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given debitor is still there
            assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "Debitor:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedDebitor() {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor();
            assumeThat(givenDebitor.getBillingContact().getLabel()).isEqualTo("forth contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
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
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Fourth").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
            final var newDebitor = HsOfficeDebitorEntity.builder()
                    .debitorNumber(++nextDebitorNumber)
                    .partner(givenPartner)
                    .billingContact(givenContact)
                    .build();

            return debitorRepo.save(newDebitor);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var count = em.createQuery(
                            "DELETE FROM HsOfficeDebitorEntity d WHERE d.debitorNumber > " + LOWEST_TEMP_DEBITOR_NUMBER)
                    .executeUpdate();
            System.out.printf("deleted %d entities%n", count);
        });
    }
}
