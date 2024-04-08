package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeSepaMandateControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsOfficeSepaMandateRepository sepaMandateRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    @Accepts({ "SepaMandate:F(Find)" })
    class ListSepaMandates {

        @Test
        void globalAdmin_canViewAllSepaMandates_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .log().all()
                    .body("", lenientlyEquals("""
                    [
                         {
                             "debitor": { "debitorNumber": 1000111 },
                             "bankAccount": { "holder": "First GmbH" },
                             "reference": "ref-10001-11",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": { "debitorNumber": 1000212 },
                             "bankAccount": { "holder": "Second e.K." },
                             "reference": "ref-10002-12",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": { "debitorNumber": 1000313 },
                             "bankAccount": { "holder": "Third OHG" },
                             "reference": "ref-10003-13",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         }
                     ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "SepaMandate:C(Create)" })
    class AddSepaMandate {

        @Test
        void globalAdmin_canAddSepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("Third").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref CAT A",
                                   "agreement": "2020-01-02",
                                   "validFrom": "2022-10-13"
                                 }
                            """.formatted(givenDebitor.getUuid(), givenBankAccount.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/sepamandates")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("debitor.partner.partnerNumber", is(10003))
                        .body("bankAccount.iban", is("DE02200505501015871393"))
                        .body("reference", is("temp ref CAT A"))
                        .body("validFrom", is("2022-10-13"))
                        .body("validTo", equalTo(null))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new sepaMandate can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        // TODO.test: move validation tests to a ...WebMvcTest
        @Test
        void globalAdmin_canNotAddSepaMandateWhenDebitorUuidIsMissing() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("Third").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref CAT B",
                                   "validFrom": "2022-10-13"
                                 }
                            """.formatted(givenBankAccount.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/sepamandates")
                .then().assertThat()
                    .statusCode(400);  // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddSepaMandate_ifBankAccountDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("Third").get(0);
            final var givenBankAccountUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref CAT C",
                                   "agreement": "2022-10-12",
                                   "validFrom": "2022-10-13",
                                   "validTo": "2024-12-31"
                                 }
                            """.formatted(givenDebitor.getUuid(), givenBankAccountUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/sepamandates")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find BankAccount with uuid 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddSepaMandate_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp refCAT D",
                                   "agreement": "2022-10-12",
                                   "validFrom": "2022-10-13",
                                   "validTo": "2024-12-31"
                                 }
                            """.formatted(givenDebitorUuid, givenBankAccount.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/sepamandates")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Debitor with uuid 00000000-0000-0000-0000-000000000000"));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "SepaMandate:R(Read)" })
    class GetSepaMandate {

        @Test
        void globalAdmin_canGetArbitrarySepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "debitor": { "debitorNumber": 1000111 },
                         "bankAccount": {
                            "holder": "First GmbH",
                            "iban": "DE02120300000000202051"
                         },
                         "reference": "ref-10001-11",
                         "validFrom": "2022-10-01",
                         "validTo": "2026-12-31"
                     }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "SepaMandate:X(Access Control)" })
        void normalUser_canNotGetUnrelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "SepaMandate:X(Access Control)" })
        void bankAccountAdminUser_canGetRelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "bankaccount-admin@FirstGmbH.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "debitor": { "debitorNumber": 1000111 },
                         "bankAccount": {
                            "holder": "First GmbH",
                            "iban": "DE02120300000000202051"
                         },
                         "reference": "ref-10001-11",
                         "validFrom": "2022-10-01",
                         "validTo": "2026-12-31"
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "SepaMandate:U(Update)" })
    class PatchSepaMandate {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfSepaMandate() {

            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "reference": "temp ref CAT Z - patched",
                               "agreement": "2020-06-01",
                               "validFrom": "2020-06-05",
                               "validTo": "2022-12-31"
                           }
                          """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("debitor.debitorNumber", is(1000111))
                    .body("bankAccount.iban", is("DE02120300000000202051"))
                    .body("reference", is("temp ref CAT Z - patched"))
                    .body("agreement", is("2020-06-01"))
                    .body("validFrom", is("2020-06-05"))
                    .body("validTo", is("2022-12-31"));
            // @formatter:on

            // finally, the sepaMandate is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)");
                        assertThat(mandate.getBankAccount().toShortString()).isEqualTo("First GmbH");
                        assertThat(mandate.getReference()).isEqualTo("temp ref CAT Z - patched");
                        assertThat(mandate.getValidFrom()).isEqualTo("2020-06-05");
                        assertThat(mandate.getValidTo()).isEqualTo("2022-12-31");
                        return true;
                    });
        }

        @Test
        void globalAdmin_canPatchJustValidToOfArbitrarySepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "validTo": "2022-12-31"
                           }
                          """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("debitor.debitorNumber", is(1000111))
                    .body("bankAccount.iban", is("DE02120300000000202051"))
                    .body("reference", is("temp ref CAT Z"))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", is("2022-12-31"));
            // @formatter:on

            // finally, the sepaMandate is actually updated
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)");
                        assertThat(mandate.getBankAccount().toShortString()).isEqualTo("First GmbH");
                        assertThat(mandate.getReference()).isEqualTo("temp ref CAT Z");
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2023-01-01)");
                        return true;
                    });
        }

        @Test
        void globalAdmin_canNotPatchReferenceOfArbitrarySepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "reference": "temp ref CAT new"
                           }
                           """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().assertThat()
                    // TODO.impl: I'd prefer a 400,
                    //      but OpenApi Spring Code Gen does not convert additonalProperties=false into a validation
                    .statusCode(200); // @formatter:on

            // finally, the sepaMandate is actually updated
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2023-03-31)");
                        return true;
                    });
        }
    }

    @Nested
    @Accepts({ "SepaMandate:D(Delete)" })
    class DeleteSepaMandate {

        @Test
        void globalAdmin_canDeleteArbitrarySepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given sepaMandate is gone
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "SepaMandate:X(Access Control)" })
        void bankAccountAdminUser_canNotDeleteRelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "bankaccount-admin@FirstGmbH.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given sepaMandate is still there
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "SepaMandate:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given sepaMandate is still there
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeSepaMandateEntity givenSomeTemporarySepaMandateForDebitorNumber(final int debitorNumber) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(debitorNumber).get(0);
            final var bankAccountHolder = ofNullable(givenDebitor.getPartner().getPartnerRel().getHolder().getTradeName())
                    .orElse(givenDebitor.getPartner().getPartnerRel().getHolder().getFamilyName());
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike(bankAccountHolder).get(0);
            final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitor(givenDebitor)
                    .bankAccount(givenBankAccount)
                    .reference("temp ref CAT Z")
                    .agreement(LocalDate.parse("2022-10-31"))
                    .validity(Range.closedOpen(
                            LocalDate.parse("2022-11-01"), LocalDate.parse("2023-03-31")))
                    .build();

            return sepaMandateRepo.save(newSepaMandate);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            final var count = em.createQuery("DELETE FROM HsOfficeSepaMandateEntity s WHERE s.reference like 'temp %'")
                    .executeUpdate();
            if (count == 0) {
                System.out.println("nothing deleted");
            }
        }).assertSuccessful();
    }
}
