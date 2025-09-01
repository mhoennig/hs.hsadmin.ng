package net.hostsharing.hsadminng.hs.accounts;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.accounts.HsCredentialsEntity.HsCredentialsEntityBuilder;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Transactional
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Tag("generalIntegrationTest")
// too complex database interaction for just a RestTest, thus a fully integrated test
class HsCredentialsControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    RbacSubjectRepository subjectRepo;

    @Autowired
    HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    HsCredentialsContextRealRepository contextRepo;

    @Autowired
    HsCredentialsRepository credentialsRepo;

    @Autowired
    HsCredentialsContextRbacRepository loginContextRbacRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    void setUp() {
        context.define("superuser-alex@hostsharing.net");
    }

    @Nested
    class GetCurrentUser {

        @Test
        void shouldFetchCurrentLoginUser() throws Exception {
            // given
            context.define("superuser-alex@hostsharing.net");

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/current")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("subject.name", equalTo("superuser-alex@hostsharing.net"))
                        .body("globalAdmin", equalTo(true));
            // @formatter:on
        }
    }

    @Nested
    class GetCredentialsByUuid {

        @Test
        void shouldFilterInvalidContextsRegardingNonNaturalPerson() {
            // given
            val legalPerson = givenLegalPerson("selfregistered-user-drew@hostsharing.org");
            val credentialsEntity = givenNewCredentials("selfregistered-user-drew@hostsharing.org",
                    "test-subject1", legalPerson, builder -> {
                builder.loginContexts(new HashSet<>(contextRepo.findAll()));
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer " + credentialsEntity.getSubject().getName())
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/credentials/" + credentialsEntity.getUuid())
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
                                "nickname": "test-subject1",
                                "totpSecrets": null,
                                "phonePassword": null,
                                "emailAddress": null,
                                "smsNumber": null,
                                "active": false,
                                "globalUid": null,
                                "globalGid": null,
                                "onboardingToken": null,
                                "contexts": [
                                    {
                                        "uuid": "33333333-3333-3333-3333-333333333333",
                                        "type": "SSH",
                                        "qualifier": "external",
                                        "onlyForNaturalPersons": false,
                                        "publicAccess": true
                                    },
                                    {
                                        "uuid": "66666666-6666-6666-6666-666666666666",
                                        "type": "MASTODON",
                                        "qualifier": "external",
                                        "onlyForNaturalPersons": false,
                                        "publicAccess": true
                                    },
                                    {
                                        "uuid": "77777777-7777-7777-7777-777777777777",
                                        "type": "BBB",
                                        "qualifier": "external",
                                        "onlyForNaturalPersons": false,
                                        "publicAccess": true
                                    }
                                ],
                                "lastUsed": null
                            }
                            """));
            // @formatter:on
        }
    }

    @Nested
    class PostNewCredentials {

        @Test
        void shouldRejectCreatingCredentialsForUnrepresentedPerson() {
            // given
            val testPerson = givenPersonWithUuid("selfregistered-user-drew@hostsharing.org");
            val publicContext = contextRepo.findByTypeAndQualifier("SSH", "external").orElseThrow();
            assertThat(publicContext.isPublicAccess()).as("precondition failed").isTrue();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "person.uuid": "%s",
                                "nickname": "new-user",
                                "active": true,
                                "globalUid": 30001,
                                "globalGid": 40001,
                                "contexts": [
                                    {
                                        "uuid" : "%s"
                                    }
                                ]
                            }
                            """.formatted(testPerson.getUuid(), publicContext.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/credentials")
                    .then().log().all().assertThat()
                        .statusCode(400)
                        .contentType("application/json")
                        .body("message", containsString("wird von der eingeloggten Person nicht repräsentiert"));
            // @formatter:on
        }

        @Test
        void shouldRejectCreatingCredentialsWithPrivateContextForNormalUser() {
            // given
            val drewPerson = realPersonRepo.findPersonByOptionalNameLike("Drew").getFirst();
            val privateInternalSshContext = contextRepo.findByTypeAndQualifier("SSH", "internal")
                    .map(HsCredentialsControllerAcceptanceTest::asPrivateContext).orElseThrow();
            val privateInternalMatrixContext = contextRepo.findByTypeAndQualifier("MATRIX", "internal")
                    .map(HsCredentialsControllerAcceptanceTest::asPrivateContext).orElseThrow();
            val publicExternalMatrixContext = contextRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsCredentialsControllerAcceptanceTest::asPublicContext).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "person.uuid": "%s",
                                "nickname": "new-user",
                                "active": true,
                                "globalUid": 30001,
                                "globalGid": 40001,
                                "contexts": [
                                    { "uuid" : "%s" },
                                    { "uuid" : "%s" },
                                    { "uuid" : "%s" }
                                ]
                            }
                            """.formatted(
                                drewPerson.getUuid(),
                                publicExternalMatrixContext.getUuid(),
                                privateInternalSshContext.getUuid(),
                                privateInternalMatrixContext.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/credentials")
                    .then().log().all().assertThat()
                        .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Kontext-Zugriff verweigert: 'MATRIX:internal', 'SSH:internal'"));
            // @formatter:on
        }

        @Test
        void shouldRejectCreatingCredentialsWithNaturalPersonRequirementForNonNaturalPerson() {
            // given
            val firstGmbHPerson = realPersonRepo.findPersonByOptionalNameLike("First").getFirst();
            val hsadminProdContextOnlyForNaturalPersons = contextRepo.findByTypeAndQualifier("HSADMIN", "prod")
                    .map(HsCredentialsControllerAcceptanceTest::asNaturalPersonContext).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "person.uuid": "%s",
                                    "nickname": "new-user",
                                    "active": true,
                                    "globalUid": 30001,
                                    "globalGid": 40001,
                                    "contexts": [
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(
                                firstGmbHPerson.getUuid(),
                                hsadminProdContextOnlyForNaturalPersons.getUuid()))
                        .port(port)
                    .when()
                    .post("http://localhost/api/hs/accounts/credentials")
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Kontext verlangt eine natürliche Person: 'HSADMIN:prod'"));
            // @formatter:on
        }
    }

    @Nested
    class PatchCredentials {

        @Test
        void shouldRejectPatchingCredentialsWithPrivateContextForNormalUser() {
            // given
            context.define("selfregistered-user-drew@hostsharing.org");
            val drewCredentialsUuid = credentialsRepo.findByCurrentSubject().stream().findFirst().orElseThrow()
                    .getSubject().getUuid();
            val privateInternalSshContext = contextRepo.findByTypeAndQualifier("SSH", "internal")
                    .map(HsCredentialsControllerAcceptanceTest::asPrivateContext).orElseThrow();
            val privateInternalMatrixContext = contextRepo.findByTypeAndQualifier("MATRIX", "internal")
                    .map(HsCredentialsControllerAcceptanceTest::asPrivateContext).orElseThrow();
            val publicExternalMatrixContext = contextRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsCredentialsControllerAcceptanceTest::asPublicContext).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "contexts": [
                                        { "uuid" : "%s" },
                                        { "uuid" : "%s" },
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(
                                    privateInternalSshContext.getUuid(),
                                    publicExternalMatrixContext.getUuid(),
                                    privateInternalMatrixContext.getUuid()))
                        .port(port)
                    .when()
                        .patch("http://localhost/api/hs/accounts/credentials/" + drewCredentialsUuid)
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Kontext-Zugriff verweigert: 'MATRIX:internal', 'SSH:internal'"));
            // @formatter:on
        }

        @Test
        void shouldRejectPatchingCredentialsAndRemovingTheOwnHsadminCredentials() {
            // given
            context.define("selfregistered-user-drew@hostsharing.org");
            val drewCredentialsUuid = credentialsRepo.findByCurrentSubject().stream().findFirst().orElseThrow()
                    .getSubject().getUuid();
            val publicExternalMatrixContext = contextRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsCredentialsControllerAcceptanceTest::asPublicContext).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                    .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .header("Accept-Language", "de")
                    .contentType(ContentType.JSON)
                    .body("""
                                {
                                    "contexts": [
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(publicExternalMatrixContext.getUuid()))
                    .port(port)
                    .when()
                    .patch("http://localhost/api/hs/accounts/credentials/" + drewCredentialsUuid)
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("die eigenen hsadmin-Credentials dürfen nicht entfernt werden"));
            // @formatter:on
        }
    }

    @Nested
    class MarkCredentialsAsUsed {

        @Test
        void markCredentialsAsUsed() {
            // given
            val testPerson = givenNaturalPerson("selfregistered-user-drew@hostsharing.org");
            val credentialsEntity = givenNewCredentials("selfregistered-user-drew@hostsharing.org",
                    "test-subject2",
                    testPerson, builder -> {
                builder.onboardingToken("some-onboarding-token");
                builder.loginContexts(contextRepo.findAll().stream()
                        .filter(HsCredentialsContext::isPublicAccess).collect(Collectors.toSet()));
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/credentials/" + credentialsEntity.getUuid() + "/used")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("uuid", is(credentialsEntity.getUuid().toString()))
                        .body("onboardingToken", is(nullValue()))
                        .body("lastUsed", is(not(nullValue())));
            // @formatter:on
        }
    }

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

    private HsOfficePersonRealEntity givenNaturalPerson(final String executingSubjectName) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);
            return toCleanup(realPersonRepo.save(HsOfficePersonRealEntity.builder()
                    .personType(NATURAL_PERSON)
                    .familyName("Test")
                    .givenName("User")
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

    private static HsCredentialsContextRealEntity asNaturalPersonContext(@NotNull HsCredentialsContextRealEntity context) {
        assertThat(context.isOnlyForNaturalPersons()).as("precondition failed").isTrue();
        return context;
    }

    private static HsCredentialsContextRealEntity asPrivateContext(@NotNull HsCredentialsContextRealEntity context) {
        assertThat(context.isPublicAccess()).as("precondition failed").isFalse();
        return context;
    }

    private static HsCredentialsContextRealEntity asPublicContext(@NotNull HsCredentialsContextRealEntity context) {
        assertThat(context.isPublicAccess()).as("precondition failed").isTrue();
        return context;
    }

    private HsCredentialsEntity givenNewCredentials(
            final String executingSubjectName,
            final String newSubjectName, final HsOfficePersonRealEntity person,
            final Consumer<HsCredentialsEntityBuilder> modifier
    ) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);
            final RbacSubjectEntity rbacSubjectEntity = RbacSubjectEntity.builder()
                .name(newSubjectName)
                .build();
            val subject = subjectRepo.create(rbacSubjectEntity);

            context.define(subject.getName());
            val attachedPerson = em.find(HsOfficePersonRealEntity.class, person.getUuid());
            val credentialsBuilder = HsCredentialsEntity.builder()
                    .person(attachedPerson)
                    .subject(subjectRepo.findByUuid(subject.getUuid()))
                    .loginContexts(Set.of());
            modifier.accept(credentialsBuilder);
            return toCleanup(credentialsRepo.save(credentialsBuilder.build()));
        }).assertSuccessful().returnedValue();
    }
}
