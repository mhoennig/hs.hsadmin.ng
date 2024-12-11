package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.test.DisableSecurityConfig;
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
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
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
    class ListSepaMandates {

        @Test
        void globalAdmin_canViewAllSepaMandates_ifNoCriteriaGiven() {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
                             "debitor": { "debitorNumber": "D-1000111" },
                             "bankAccount": { "holder": "First GmbH" },
                             "reference": "ref-10001-11",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": { "debitorNumber": "D-1000212" },
                             "bankAccount": { "holder": "Second e.K." },
                             "reference": "ref-10002-12",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": { "debitorNumber": "D-1000313" },
                             "bankAccount": { "holder": "Third OHG" },
                             "reference": "ref-10003-13",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         }
                     ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_canFindSepaMandateByName() {

            RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/office/sepamandates?iban=DE02120300000000202051")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .log().all()
                        .body("", lenientlyEquals("""
                        [
                             {
                                 "debitor": { "debitorNumber": "D-1000111" },
                                 "bankAccount": {
                                    "iban": "DE02120300000000202051",
                                    "holder": "First GmbH"
                                 },
                                 "reference": "ref-10001-11",
                                 "validFrom": "2022-10-01",
                                 "validTo": "2026-12-31"
                             }
                         ]
                        """));
                        // @formatter:on
        }
    }

    @Nested
    class PostNewSepaMandate {

        @Test
        void globalAdmin_canPostNewSepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("Third").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "debitor.uuid": "%s",
                                   "bankAccount.uuid": "%s",
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
                        .body("debitor.partner.partnerNumber", is("P-10003"))
                        .body("bankAccount.iban", is("DE02200505501015871393"))
                        .body("reference", is("temp ref CAT A"))
                        .body("validFrom", is("2022-10-13"))
                        .body("validTo", equalTo(null))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new sepaMandate can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }

        // TODO.test: move validation tests to a ...WebMvcTest
        @Test
        void globalAdmin_canNotPostNewSepaMandateWhenDebitorUuidIsMissing() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("Third").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "bankAccount.uuid": "%s",
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
        void globalAdmin_canNotPostNewSepaMandate_ifBankAccountDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("Third").get(0);
            final var givenBankAccountUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitor.uuid": "%s",
                                   "bankAccount.uuid": "%s",
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
                    .body("message", is("ERROR: [400] Unable to find BankAccount with uuid 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotPostNewSepaMandate_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitor.uuid": "%s",
                                   "bankAccount.uuid": "%s",
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
                    .body("message", is("ERROR: [400] Unable to find Debitor with uuid 00000000-0000-0000-0000-000000000000"));
                // @formatter:on
        }
    }

    @Nested
    class GetSepaMandate {

        @Test
        void globalAdmin_canGetArbitrarySepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "debitor": { "debitorNumber": "D-1000111" },
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
        void normalUser_canNotGetUnrelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void bankAccountAdminUser_canGetRelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandateUuid = sepaMandateRepo.findSepaMandateByOptionalIban("DE02120300000000202051")
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "bankaccount-admin@FirstGmbH.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/sepamandates/" + givenSepaMandateUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "debitor": { "debitorNumber": "D-1000111" },
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
    class PatchSepaMandate {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfSepaMandate() {

            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
                    .body("debitor.debitorNumber", is("D-1000111"))
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
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
                    .body("debitor.debitorNumber", is("D-1000111"))
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
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
    class DeleteSepaMandate {

        @Test
        void globalAdmin_canDeleteArbitrarySepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given sepaMandate is gone
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isEmpty();
        }

        @Test
        void bankAccountAdminUser_canNotDeleteRelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "bankaccount-admin@FirstGmbH.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/sepamandates/" + givenSepaMandate.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given sepaMandate is still there
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isNotEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedSepaMandate() {
            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateForDebitorNumber(1000111);

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
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
            final var givenDebitor = debitorRepo.findDebitorByDebitorNumber(debitorNumber).orElseThrow();
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
