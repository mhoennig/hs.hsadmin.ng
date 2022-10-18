package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeSepaMandateControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeSepaMandateRepository sepaMandateRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    Set<UUID> tempSepaMandateUuids = new HashSet<>();

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
                    .body("", lenientlyEquals("""
                    [
                         {
                             "debitor": {
                                 "debitorNumber": 10002,
                                 "billingContact": { "label": "second contact" }
                             },
                             "bankAccount": { "holder": "Second e.K." },
                             "reference": "refSeconde.K.",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": {
                                 "debitorNumber": 10001,
                                 "billingContact": { "label": "first contact" }
                             },
                             "bankAccount": { "holder": "First GmbH" },
                             "reference": "refFirstGmbH",
                             "validFrom": "2022-10-01",
                             "validTo": "2026-12-31"
                         },
                         {
                             "debitor": {
                                 "debitorNumber": 10003,
                                 "billingContact": { "label": "third contact" }
                             },
                             "bankAccount": { "holder": "Third OHG" },
                             "reference": "refThirdOHG",
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
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIban("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref A",
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
                        .body("debitor.partner.person.tradeName", is("Third OHG"))
                        .body("bankAccount.iban", is("DE02200505501015871393"))
                        .body("reference", is("temp ref A"))
                        .body("validFrom", is("2022-10-13"))
                        .body("validTo", equalTo(null))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new sepaMandate can be accessed under the generated UUID
            final var newUserUuid = toCleanup(UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newUserUuid).isNotNull();
        }

        // TODO.test: move validation tests to a ...WebMvcTest
        @Test
        void globalAdmin_canNotAddSepaMandateWhenDebitorUuidIsMissing() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("Third").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIban("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref A",
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
            final var givenBankAccountUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref A",
                                   "validFrom": "2022-10-13",
                                   "validTo": "2024-12-31"
                                 }
                            """.formatted(givenDebitor.getUuid(), givenBankAccountUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/sepamandates")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find BankAccount with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddSepaMandate_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitorUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIban("DE02200505501015871393").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "debitorUuid": "%s",
                                   "bankAccountUuid": "%s",
                                   "reference": "temp ref A",
                                   "validFrom": "2022-10-13",
                                   "validTo": "2024-12-31"
                                 }
                            """.formatted(givenDebitorUuid, givenBankAccount.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/sepamandates")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", is("Unable to find Debitor with uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
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
                         "debitor": {
                             "debitorNumber": 10001,
                             "billingContact": { "label": "first contact" }
                         },
                         "bankAccount": { 
                            "holder": "First GmbH",
                            "iban": "DE02120300000000202051"
                         },
                         "reference": "refFirstGmbH",
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
                         "debitor": {
                             "debitorNumber": 10001,
                             "billingContact": { "label": "first contact" }
                         },
                         "bankAccount": { 
                            "holder": "First GmbH",
                            "iban": "DE02120300000000202051"
                         },
                         "reference": "refFirstGmbH",
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
        void globalAdmin_canPatchValidToOfArbitrarySepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandate();

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
                    .body("debitor.partner.person.tradeName", is("First GmbH"))
                    .body("bankAccount.iban", is("DE02120300000000202051"))
                    .body("reference", is("temp ref X"))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", is("2022-12-31"));
            // @formatter:on

            // finally, the sepaMandate is actually updated
            assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("debitor(10001: First GmbH)");
                        assertThat(mandate.getBankAccount().toShortString()).isEqualTo("First GmbH");
                        assertThat(mandate.getReference()).isEqualTo("temp ref X");
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2023-01-01)");
                        return true;
                    });
        }

        @Test
        void globalAdmin_canNotPatchReferenceOfArbitrarySepaMandate() {

            context.define("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandate();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "reference": "new ref"
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
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,)");
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
            final var givenSepaMandate = givenSomeTemporarySepaMandate();

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
            final var givenSepaMandate = givenSomeTemporarySepaMandate();
            assertThat(givenSepaMandate.getReference()).isEqualTo("temp ref X");

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
            final var givenSepaMandate = givenSomeTemporarySepaMandate();
            assertThat(givenSepaMandate.getReference()).isEqualTo("temp ref X");

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

    private HsOfficeSepaMandateEntity givenSomeTemporarySepaMandate() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("First").get(0);
            final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitor(givenDebitor)
                    .bankAccount(givenBankAccount)
                    .reference("temp ref X")
                    .validity(Range.closedOpen(
                            LocalDate.parse("2022-11-01"), LocalDate.parse("2023-03-31")))
                    .build();

            toCleanup(newSepaMandate.getUuid());

            return sepaMandateRepo.save(newSepaMandate);
        }).assertSuccessful().returnedValue();
    }

    private UUID toCleanup(final UUID tempSepaMandateUuid) {
        tempSepaMandateUuids.add(tempSepaMandateUuid);
        return tempSepaMandateUuid;
    }

    @AfterEach
    void cleanup() {
        tempSepaMandateUuids.forEach(uuid -> {
            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net", null);
                System.out.println("DELETING temporary sepaMandate: " + uuid);
                final var count = sepaMandateRepo.deleteByUuid(uuid);
                System.out.println("DELETED temporary sepaMandate: " + uuid + (count > 0 ? " successful" : " failed"));
            });
        });
    }

}
