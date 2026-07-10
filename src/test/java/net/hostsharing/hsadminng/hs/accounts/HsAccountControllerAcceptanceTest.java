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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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
