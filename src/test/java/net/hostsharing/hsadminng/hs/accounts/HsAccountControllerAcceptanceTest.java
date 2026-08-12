package net.hostsharing.hsadminng.hs.accounts;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.accounts.HsAccountEntity.HsAccountEntityBuilder;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.function.Consumer;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

@Tag("generalIntegrationTest")
@Transactional
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("fake-jwt")
// too complex database interaction for just a RestTest, thus a fully integrated test
class HsAccountControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    Integer port;

    @Autowired
    Context context;

    @Autowired
    RbacSubjectRepository rbacSubjectRepo;

    @Autowired
    HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    HsAccountRepository accountRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    void setUp() {
        context.define("hsh-alex_superuser");
    }

    @Nested
    class GetCurrentUser {

        @Test
        void shouldFetchCurrentLoginUser() {
            // given
            context.define("hsh-alex_superuser");

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/current")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("subject.name", equalTo("hsh-alex_superuser"))
                        .body("globalAdmin", equalTo(true));
            // @formatter:on
        }
    }

    @Nested
    class ListAccounts {

        @Test
        void unknownUser_isNotAllowedToListAccounts() {
            // @formatter:off
            RestAssured
                    .given()
                    .header("Authorization", bearer("unknown-user"))
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/accounts/accounts")
                    .then().log().body().assertThat()
                    .statusCode(401);
            // @formatter:on
        }

        @Test
        void globalAdmin_canListAllAccounts() {
            // @formatter:off
            RestAssured
                    .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/accounts/accounts")
                    .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("subject.name", hasItems(
                            "hsh-alex_superuser", "hsh-fran_superuser", "tst-drew_selfregistered"));
            // @formatter:on
        }

        @Test
        void normalUser_canListOnlyAccountsOfSamePerson() {
            // @formatter:off
            RestAssured
                    .given()
                    .header("Authorization", bearer("tst-drew_selfregistered"))
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/accounts/accounts")
                    .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("size()", equalTo(1))
                    .body("[0].subject.name", equalTo("tst-drew_selfregistered"));
            // @formatter:on
        }
    }

    @Nested
    class GetAccountByUuid {

        @Test
        void shouldGetAccountByUuid() {
            // given
            val legalPerson = givenLegalPerson("tst-drew_selfregistered");
            val accountEntity = givenNewAccount("tst-drew_selfregistered",
                    "test-subject1", legalPerson, builder -> {
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer(accountEntity.getSubject().getName()))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts/" + accountEntity.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("$", lenientlyEquals("""
                            {
                                "person": {
                                    "personType": "LEGAL_PERSON",
                                    "tradeName": "Test Company",
                                    "salutation": null,
                                    "title": null,
                                    "givenName": null,
                                    "familyName": null
                                },
                                "subject": {
                                    "uuid": "%s",
                                    "name": "test-subject1",
                                    "type": "USER"
                                },
                                "globalUid": null,
                                "globalGid": null
                            }
                            """.formatted(accountEntity.getUuid())));
            // @formatter:on
        }
    }

    @Nested
    class PostNewAccount {

        @Test
        void shouldRejectCreatingAccountIfNotGlobalAdmin() {
            // New accounts will be created only in Keycloak and synched from Keycloak to hsadmin-NG.
            // For now, we only allow global-admins to create new accounts.

            // given
            val testPerson = givenPersonWithUuid("tst-drew_selfregistered");

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "person.uuid": "%s",
                                "subject.uuid": "%s",
                                "globalUid": 30001,
                                "globalGid": 40001
                            }
                            """.formatted(testPerson.getUuid(), UUID.randomUUID()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/accounts")
                    .then().log().all().assertThat()
                        .statusCode(403)
                        .contentType("application/json")
                        .body("message", containsString("Das Subjekt tst-drew_selfregistered hat keine Berechtigung, die Rolle rbac.global#global:ADMIN anzunehmen"));
            // @formatter:on
        }

        @Test
        void shouldRejectCreatingAccountForNonNaturalPerson() {
            // given
            val firstGmbHPerson = realPersonRepo.findPersonByOptionalNameLike("First").getFirst();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(firstGmbHPerson.getUuid(), UUID.randomUUID()))
                        .port(port)
                    .when()
                    .post("http://localhost/api/hs/accounts/accounts")
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message",
                            containsString("Nur natürliche Personen sind erlaubt, aber ${personUuid} ist LEGAL_PERSON"
                                    .replace("${personUuid}", firstGmbHPerson.getUuid().toString())));
            // @formatter:on
        }
    }

    /**
     * `hs_accounts.account` is not ReBAC-restricted, thus the controller has to apply the
     * visibility condition itself; these tests pin that it actually does.
     */
    @Nested
    class ForeignAccountAccess {

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canNotGetForeignAccountByUuid() {
            // given
            val foreignPerson = givenLegalPerson("hsh-alex_superuser");
            val foreignAccount = givenNewAccount("hsh-alex_superuser",
                    "test-foreign-subject", foreignPerson, builder -> {
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts/" + foreignAccount.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(404);
            // @formatter:on
        }

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canNotListAccountsOfForeignPerson() {
            // given
            val foreignPerson = givenLegalPerson("hsh-alex_superuser");
            givenNewAccount("hsh-alex_superuser",
                    "test-foreign-subject-of-person", foreignPerson, builder -> {
            });

            assertThatAccountFilterRejectsPersonUuid(foreignPerson.getUuid());
        }

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canNotDeleteForeignAccount() {
            // given
            val foreignPerson = givenLegalPerson("hsh-alex_superuser");
            val foreignAccount = givenNewAccount("hsh-alex_superuser",
                    "test-foreign-subject-to-delete", foreignPerson, builder -> {
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .delete("http://localhost/api/hs/accounts/accounts/" + foreignAccount.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(403)
                        // the endpoint itself has to refuse; today only the subject-delete trigger
                        // happens to abort the transaction, which any change of the delete order removes
                        .body("message", not(containsString("not allowed to delete subject")));
            // @formatter:on

            // then the foreign account still exists
            context.define("hsh-alex_superuser");
            assertThat(accountRepo.findByUuid(foreignAccount.getUuid())).isPresent();
        }

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canNotProbeExistenceOfInvisibleForeignPersonViaAccountFilter() {
            // given an existing person that is invisible to the attacker
            val foreignPerson = givenLegalPerson("hsh-alex_superuser");

            // the personUuid filter must not distinguish an invisible-but-existing person from a
            // non-existing one, so both have to be rejected the very same way
            assertThatAccountFilterRejectsPersonUuid(foreignPerson.getUuid());
            assertThatAccountFilterRejectsPersonUuid(UUID.randomUUID());
        }

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canListAccountsFilteredByItsOwnPerson() {
            // given the own person-uuid, exactly as the unfiltered account-list hands it out
            val ownPersonUuid = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts")
                    .then().assertThat()
                        .statusCode(200)
                    .extract().path("[0].person.uuid").toString(); // @formatter:on

            // filtering by that very person must yield the same account, not a rejection
            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts?personUuid=" + ownPersonUuid)
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .body("size()", equalTo(1))
                        .body("[0].subject.name", equalTo("tst-drew_selfregistered"));
            // @formatter:on
        }

        // verifies Fix for Security-Finding A-03
        @Test
        void normalUser_canNotAccessAccountOfForeignPersonInSameOrganization() {
            // given an account of a foreign person, but of a subject in the attacker's own organization
            val foreignPerson = givenLegalPerson("hsh-alex_superuser");
            val foreignAccount = givenNewAccount("hsh-alex_superuser",
                    "tst-foreign_sameorganization", foreignPerson, builder -> {
            });

            // sharing an organization is no reason to see a foreign person's account
            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts/" + foreignAccount.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(404);
            // @formatter:on

            assertThatAccountFilterRejectsPersonUuid(foreignPerson.getUuid());
        }

        private void assertThatAccountFilterRejectsPersonUuid(final UUID personUuid) {
            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/accounts?personUuid=" + personUuid)
                    .then().log().all().assertThat()
                        .statusCode(400)
                        .body("message", containsString(
                                "personUuid \"%s\" not found or not accessible".formatted(personUuid)));
            // @formatter:on
        }
    }

    // TODO.spec Task#5637: add @Nested class DeleteAccount and tests for delete, when we have a spec

    // Helper methods

    private HsOfficePersonRealEntity givenLegalPerson(final String executingSubjectName) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);
            return toCleanup(realPersonRepo.save(HsOfficePersonRealEntity.builder()
                    .personType(LEGAL_PERSON)
                    .tradeName("Test Company")
                    .build()));
        }).assertSuccessful().returnedValue();
    }

    private HsOfficePersonRealEntity givenPersonWithUuid(final String executingSubjectName) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);
            return toCleanup(realPersonRepo.save(HsOfficePersonRealEntity.builder()
                    .personType(NATURAL_PERSON)
                    .familyName("Test")
                    .givenName("Person")
                    .build()));
        }).returnedValue();
    }

    private HsAccountEntity givenNewAccount(
            final String executingSubjectName,
            final String newSubjectName, final HsOfficePersonRealEntity person,
            final Consumer<HsAccountEntityBuilder> modifier
    ) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);

            // only RbacSubject entities can be created
            val subject = rbacSubjectRepo.create(RbacSubjectEntity.builder()
                .name(newSubjectName)
                .build());

            context.define(subject.getName());
            val attachedPerson = em.find(HsOfficePersonRealEntity.class, person.getUuid());
            val accountBuilder = HsAccountEntity.builder()
                    .person(attachedPerson)
                    .subject(em.find(RealSubjectEntity.class, subject.getUuid()));
            modifier.accept(accountBuilder);
            return toCleanup(accountRepo.save(accountBuilder.build()));
        }).assertSuccessful().returnedValue();
    }
}
