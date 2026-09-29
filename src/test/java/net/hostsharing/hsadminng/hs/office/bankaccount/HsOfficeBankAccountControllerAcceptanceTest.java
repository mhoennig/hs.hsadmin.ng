package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@Tag("officeIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
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
    class GetListOfBankAccounts {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllBankAccounts_ifNoCriteriaGiven() {

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
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
                                  "iban": "DE38990200000002474689",
                                  "bic": "DEMVDEH0"
                              },
                              {
                                  "holder": "First GmbH",
                                  "iban": "DE34990200000000202051",
                                  "bic": "DEMVDEH0"
                              },
                              {
                                  "holder": "Fourth eG",
                                  "iban": "DE20990500001015871393",
                                  "bic": "DEMGDEM0"
                              },
                              {
                                  "holder": "Mel Bessler",
                                  "iban": "DE45990500000006820101",
                                  "bic": "DEMGDEM0"
                              },
                              {
                                  "holder": "Paul Winkler",
                                  "iban": "DE52990100000002034304",
                                  "bic": "DEMSDEH0"
                              },
                              {
                                  "holder": "Peter Smith",
                                  "iban": "DE33990300000137075030",
                                  "bic": "DEMRDEF0"
                              },
                              {
                                  "holder": "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.",
                                  "iban": "DE09990100000054540402",
                                  "bic": "DEMSDEH0"
                              },
                              {
                                  "holder": "Third OHG",
                                  "iban": "DE47990600000106531065",
                                  "bic": "DEMPDED0"
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
        void globalAdmin_withoutAssumedRole_canPostNewBankAccount() {

            context.define("hsh-alex_superuser");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "holder": "temp test holder",
                                "iban": "DE71990400001234567892",
                                "bic": "DEMDDEB0"
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
                        .body("iban", is("DE71990400001234567892"))
                        .body("bic", is("DEMDDEB0"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bankaccount can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }
    }

    @Nested
    class GetBankAccount {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryBankAccount() {
            context.define("hsh-alex_superuser");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
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
            context.define("hsh-alex_superuser");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-drew_selfregistered"))
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/bankaccounts/" + givenBankAccountUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Disabled("TODO: not implemented yet - also add Accepts annotation when done")
        void bankaccountAdminUser_canGetRelatedBankAccount() {
            context.define("hsh-alex_superuser");
            final var givenBankAccountUuid = bankAccountRepo.findByOptionalHolderLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-bankaccount_firstgmbh"))
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

            context.define("hsh-alex_superuser");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("tst-rene_selfregistered");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "holder": "temp holder - patched",
                           "iban": "DE56990300000000594937",
                           "bic": "DEMRDEF0"
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().assertThat()
                    .statusCode(405);
                // @formatter:on

            // and the bankaccount is unchanged
            context.define("hsh-alex_superuser");
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
            context.define("hsh-alex_superuser");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("tst-rene_selfregistered");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
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
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("tst-rene_selfregistered");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-rene_selfregistered"))
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/bankaccounts/" + givenBankAccount.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bankaccount is still there
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser", null);
                assertThat(bankAccountRepo.findByUuid(givenBankAccount.getUuid())).isEmpty();
            }).assertSuccessful();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBankAccount() {
            context.define("hsh-alex_superuser");
            final var givenBankAccount = givenSomeTemporaryBankAccountCreatedBy("tst-rene_selfregistered");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-drew_selfregistered"))
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
                    .holder("temp acc #" + RandomStringUtils.insecure().nextAlphabetic(3))
                    .iban("DE27990300009473626226")
                    .bic("DEMRDEF0")
                    .build();

            return bankAccountRepo.save(newBankAccount);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("hsh-alex_superuser", null);
            em.createQuery("DELETE FROM HsOfficeBankAccountEntity b WHERE b.holder LIKE 'temp %'").executeUpdate();
        });
    }

}
