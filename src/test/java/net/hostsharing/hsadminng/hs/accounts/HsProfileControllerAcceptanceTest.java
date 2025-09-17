package net.hostsharing.hsadminng.hs.accounts;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.accounts.HsProfileEntity.HsProfileEntityBuilder;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
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

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@Tag("generalIntegrationTest")
@Transactional
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("fake-jwt")
// too complex database interaction for just a RestTest, thus a fully integrated test
class HsProfileControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    Integer port;

    @Autowired
    Context context;

    @Autowired
    RbacSubjectRepository rbacSubjectRepo;

    @Autowired
    HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    HsProfileScopeRealRepository scopeRepo;

    @Autowired
    HsProfileRepository profileRepo;

    @Autowired
    HsProfileScopeRbacRepository scopeRbacRepo;

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
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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
    class GetProfileByUuid {

        @Test
        void shouldFilterInvalidScopesRegardingNonNaturalPerson() {
            // given
            val legalPerson = givenLegalPerson("selfregistered-user-drew@hostsharing.org");
            val profileEntity = givenNewProfile("selfregistered-user-drew@hostsharing.org",
                    "test-subject1", legalPerson, builder -> {
                builder.scopes(new HashSet<>(scopeRepo.findAll()));
            });

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer(profileEntity.getSubject().getName()))
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/accounts/profiles/" + profileEntity.getUuid())
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
                                "scopes": [
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
                                ]
                            }
                            """));
            // @formatter:on
        }
    }

    @Nested
    class PostNewProfile {

        @Test
        void shouldRejectCreatingProfileForUnrepresentedPerson() {
            // given
            val testPerson = givenPersonWithUuid("selfregistered-user-drew@hostsharing.org");
            val publicScope = scopeRepo.findByTypeAndQualifier("SSH", "external").orElseThrow();
            assertThat(publicScope.isPublicAccess()).as("precondition failed").isTrue();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "person.uuid": "%s",
                                "nickname": "new-user",
                                "active": true,
                                "globalUid": 30001,
                                "globalGid": 40001,
                                "scopes": [
                                    {
                                        "uuid" : "%s"
                                    }
                                ]
                            }
                            """.formatted(testPerson.getUuid(), publicScope.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/profiles")
                    .then().log().all().assertThat()
                        .statusCode(400)
                        .contentType("application/json")
                        .body("message", containsString("wird von der eingeloggten Person nicht repräsentiert"));
            // @formatter:on
        }

        @Test
        void shouldRejectCreatingProfileWithPrivateScopeForNormalUser() {
            // given
            val drewPerson = realPersonRepo.findPersonByOptionalNameLike("Drew").getFirst();
            val privateInternalSshScope = scopeRepo.findByTypeAndQualifier("SSH", "internal")
                    .map(HsProfileControllerAcceptanceTest::asPrivateScope).orElseThrow();
            val privateInternalMatrixScope = scopeRepo.findByTypeAndQualifier("MATRIX", "internal")
                    .map(HsProfileControllerAcceptanceTest::asPrivateScope).orElseThrow();
            val publicExternalMatrixScope = scopeRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsProfileControllerAcceptanceTest::asPublicScope).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "person.uuid": "%s",
                                "nickname": "new-user",
                                "active": true,
                                "globalUid": 30001,
                                "globalGid": 40001,
                                "scopes": [
                                    { "uuid" : "%s" },
                                    { "uuid" : "%s" },
                                    { "uuid" : "%s" }
                                ]
                            }
                            """.formatted(
                                drewPerson.getUuid(),
                                publicExternalMatrixScope.getUuid(),
                                privateInternalSshScope.getUuid(),
                                privateInternalMatrixScope.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/accounts/profiles")
                    .then().log().all().assertThat()
                        .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Zugriff auf Geltungsbereich verweigert: 'MATRIX:internal', 'SSH:internal'"));
            // @formatter:on
        }

        @Test
        void shouldRejectCreatingProfileWithNaturalPersonRequirementForNonNaturalPerson() {
            // given
            val firstGmbHPerson = realPersonRepo.findPersonByOptionalNameLike("First").getFirst();
            val hsadminProdScopeOnlyForNaturalPersons = scopeRepo.findByTypeAndQualifier("HSADMIN", "prod")
                    .map(HsProfileControllerAcceptanceTest::asNaturalPersonScope).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "person.uuid": "%s",
                                    "nickname": "new-user",
                                    "active": true,
                                    "globalUid": 30001,
                                    "globalGid": 40001,
                                    "scopes": [
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(
                                firstGmbHPerson.getUuid(),
                                hsadminProdScopeOnlyForNaturalPersons.getUuid()))
                        .port(port)
                    .when()
                    .post("http://localhost/api/hs/accounts/profiles")
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Geltungsbereich verlangt eine natürliche Person: 'HSADMIN:prod'"));
            // @formatter:on
        }
    }

    @Nested
    class PatchProfile {

        @Test
        void shouldRejectPatchingProfileWithPrivateScopeForNormalUser() {
            // given
            context.define("selfregistered-user-drew@hostsharing.org");
            val drewProfileUuid = profileRepo.findByCurrentSubject().stream().findFirst().orElseThrow()
                    .getSubject().getUuid();
            val privateInternalSshScope = scopeRepo.findByTypeAndQualifier("SSH", "internal")
                    .map(HsProfileControllerAcceptanceTest::asPrivateScope).orElseThrow();
            val privateInternalMatrixScope = scopeRepo.findByTypeAndQualifier("MATRIX", "internal")
                    .map(HsProfileControllerAcceptanceTest::asPrivateScope).orElseThrow();
            val publicExternalMatrixScope = scopeRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsProfileControllerAcceptanceTest::asPublicScope).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "scopes": [
                                        { "uuid" : "%s" },
                                        { "uuid" : "%s" },
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(
                                    privateInternalSshScope.getUuid(),
                                    publicExternalMatrixScope.getUuid(),
                                    privateInternalMatrixScope.getUuid()))
                        .port(port)
                    .when()
                        .patch("http://localhost/api/hs/accounts/profiles/" + drewProfileUuid)
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("Zugriff auf Geltungsbereich verweigert: 'MATRIX:internal', 'SSH:internal'"));
            // @formatter:on
        }

        @Test
        void shouldRejectPatchingProfileAndRemovingTheOwnHsadminProfile() {
            // given
            context.define("selfregistered-user-drew@hostsharing.org");
            val drewProfileUuid = profileRepo.findByCurrentSubject().stream().findFirst().orElseThrow()
                    .getSubject().getUuid();
            val publicExternalMatrixScope = scopeRepo.findByTypeAndQualifier("MATRIX", "external")
                    .map(HsProfileControllerAcceptanceTest::asPublicScope).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                    .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                    .header("Accept-Language", "de")
                    .contentType(ContentType.JSON)
                    .body("""
                                {
                                    "scopes": [
                                        { "uuid" : "%s" }
                                    ]
                                }
                                """.formatted(publicExternalMatrixScope.getUuid()))
                    .port(port)
                    .when()
                    .patch("http://localhost/api/hs/accounts/profiles/" + drewProfileUuid)
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType("application/json")
                    .body("message", containsString("die eigenen hsadmin-Profile dürfen nicht entfernt werden"));
            // @formatter:on
        }


        @Test
        void shouldRejectActivatingProfileForNormalUser() {
            // given
            context.define("selfregistered-user-drew@hostsharing.org");
            val drewProfile = profileRepo.findByCurrentSubject().stream().findFirst().orElseThrow();
            val inactiveProfileUuid = createNewInactiveProfile(drewProfile.getPerson()).getSubject().getUuid();

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .header("Accept-Language", "de")
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "active": true
                                }
                                """)
                        .port(port)
                    .when()
                        .patch("http://localhost/api/hs/accounts/profiles/" + inactiveProfileUuid)
                    .then().log().all().assertThat()
                        .statusCode(403)
                        .contentType("application/json")
                        .body("message", containsString("Only global admins are allowed to activate an inactive profile"));
            // @formatter:on
        }

    }

    // Helper methods

    private HsProfileEntity createNewInactiveProfile(final HsOfficePersonRealEntity person) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            // only RbacSubject entities can be created
            val rbacSubjectEntity = rbacSubjectRepo.create(RbacSubjectEntity.builder()
                    .name("some-inactive-profile")
                    .build());
            // but we need the RealSubjectEntity to be attached to the profile entity
            val realSubjectEntity = em.find(RealSubjectEntity.class, rbacSubjectEntity.getUuid());

            val inactiveCopy = HsProfileEntity.builder()
                    .person(person)
                    .subject(realSubjectEntity)
                    .active(false).build();
            em.persist(inactiveCopy);
            em.flush();
            return toCleanup(inactiveCopy);
        }).assertSuccessful().returnedValue();
    }

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

    private static HsProfileScopeRealEntity asNaturalPersonScope(@NotNull HsProfileScopeRealEntity scope) {
        assertThat(scope.isOnlyForNaturalPersons()).as("precondition failed").isTrue();
        return scope;
    }

    private static HsProfileScopeRealEntity asPrivateScope(@NotNull HsProfileScopeRealEntity scope) {
        assertThat(scope.isPublicAccess()).as("precondition failed").isFalse();
        return scope;
    }

    private static HsProfileScopeRealEntity asPublicScope(@NotNull HsProfileScopeRealEntity scope) {
        assertThat(scope.isPublicAccess()).as("precondition failed").isTrue();
        return scope;
    }

    private HsProfileEntity givenNewProfile(
            final String executingSubjectName,
            final String newSubjectName, final HsOfficePersonRealEntity person,
            final Consumer<HsProfileEntityBuilder> modifier
    ) {
        return jpaAttempt.transacted(() -> {
            context.define(executingSubjectName);

            // only RbacSubject entities can be created
            val subject = rbacSubjectRepo.create(RbacSubjectEntity.builder()
                .name(newSubjectName)
                .build());

            context.define(subject.getName());
            val attachedPerson = em.find(HsOfficePersonRealEntity.class, person.getUuid());
            val profileBuilder = HsProfileEntity.builder()
                    .person(attachedPerson)
                    .subject(em.find(RealSubjectEntity.class, subject.getUuid()))
                    .scopes(Set.of());
            modifier.accept(profileBuilder);
            return toCleanup(profileRepo.save(profileBuilder.build()));
        }).assertSuccessful().returnedValue();
    }
}
