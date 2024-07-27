package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.junit.jupiter.api.*;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeBankAccountControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListBankAccounts {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllBankAccounts_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/bankaccounts")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                              {
                                  "holder": "Anita Bessler",
                                  "iban": "DE02300606010002474689",
                                  "bic": "DAAEDEDD"
                              },
                              {
                                  "holder": "First GmbH",
                                  "iban": "DE02120300000000202051",
                                  "bic": "BYLADEM1001"
                              },
                              {
                                  "holder": "Fourth eG",
                                  "iban": "DE02200505501015871393",
                                  "bic": "HASPDEHH"
                              },
                              {
                                  "holder": "Mel Bessler",
                                  "iban": "DE02100100100006820101",
                                  "bic": "PBNKDEFF"
                              },
                              {
                                  "holder": "Paul Winkler",
                                  "iban": "DE02600501010002034304",
                                  "bic": "SOLADEST600"
                              },
                              {
                                  "holder": "Peter Smith",
                                  "iban": "DE02500105170137075030",
                                  "bic": "INGDDEFF"
                              },
                              {
                                  "holder": "Second e.K.",
                                  "iban": "DE02100500000054540402",
                                  "bic": "BELADEBE"
                              },
                              {
                                  "holder": "Third OHG",
                                  "iban": "DE02300209000106531065",
                                  "bic": "CMCIDEDD"
                              }
                          ]
                        """
                            ));
                // @formatter:on
        }
    }

    @Nested
    class CreateBankAccount {

        @Test
        void globalAdmin_withoutAssumedRole_canAddBankAccount() {

            context.define("superuser-alex@hostsharing.net");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "holder": "temp test holder",
                                "iban": "DE88100900001234567892",
                                "bic": "BEVODEBB"
                            }
                            """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/bankaccounts")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("holder", is("temp test holder"))
                        .body("iban", is("DE88100900001234567892"))
                        .body("bic", is("BEVODEBB"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bankaccount can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    class GetBankAccount {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/bankaccounts/" + givenBankAccountUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "holder": "First GmbH"
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/bankaccounts/" + givenBankAccountUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Disabled("TODO: not implemented yet - also add Accepts annotation when done")
        void bankaccountAdminUser_canGetRelatedBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "bankaccount-admin@firstbankaccount.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/bankaccounts/" + givenBankAccountUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "holder": "...",
                         "iban": "...",
                         "bic": "..."
                     }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchBankAccount {

        @Test
        void patchIsNotImplemented() {

            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "holder": "temp holder - patched",
                           "iban": "DE02701500000000594937",
                           "bic": "SSKMDEMM"
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().assertThat()
                    .statusCode(405);
                // @formatter:on

            // and the bankaccount is unchanged
            context.define("superuser-alex@hostsharing.net");
            assertThat(bankAccountRepo.findByUuid(givenBankAccount.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getHolder()).isEqualTo(givenBankAccount.getHolder());
                        assertThat(person.getIban()).isEqualTo(givenBankAccount.getIban());
                        assertThat(person.getBic()).isEqualTo(givenBankAccount.getBic());
                        return true;
                    });
        }
    }

    @Nested
    class DeleteBankAccount {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bankaccount is gone
            assertThat(bankAccountRepo.findByUuid(givenBankAccount.getUuid())).isEmpty();
        }

        @Test
        void bankaccountOwner_canDeleteRelatedBankAaccount() {
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-test-user@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bankaccount is still there
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                assertThat(bankAccountRepo.findByUuid(givenBankAccount.getUuid())).isEmpty();
            }).assertSuccessful();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBankAccount() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // unrelated user cannot even view the bankaccount
            // @formatter:on

            // then the given bankaccount is still there
            assertThat(bankAccountRepo.findByUuid(givenBankAccount.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeBankAccountEntity givenSomeTemporaryBankAccountCreatedBy(final String creatingUser) {
        return jpaAttempt.transacted(() -> {
            context.define(creatingUser);
            final var newBankAccount = HsOfficeBankAccountEntity.builder()
                    .holder("temp acc #" + RandomStringUtils.randomAlphabetic(3))
                    .iban("DE93500105179473626226")
                    .bic("INGDDEFFXXX")
                    .build();

            return bankAccountRepo.save(newBankAccount);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeBankAccountEntity b WHERE b.holder LIKE 'temp %'").executeUpdate();
        });
    }

}
