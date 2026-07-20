package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiKeyScenarioTests extends ScenarioTest {

    // fixed UUIDs for stable scenario reports; unlike USER/GROUP subject UUIDs,
    // API_KEY subject UUIDs do not stem from Keycloak but are chosen by the creator
    private static final String VALID_MASTER_API_KEY_UUID = "a91c0001-0000-0000-0000-000000000001";
    private static final String INVALID_NAME_API_KEY_UUID = "a91c0002-0000-0000-0000-000000000002";
    private static final String FORBIDDEN_API_KEY_UUID = "a91c0003-0000-0000-0000-000000000003";
    private static final String VALID_REPORTING_API_KEY_UUID = "a91c0004-0000-0000-0000-000000000004";
    private static final String VALID_BOOTSTRAPPED_API_KEY_UUID = "a91c0005-0000-0000-0000-000000000005";
    private static final String VALID_SUBJECT_SYNC_API_KEY_UUID = "a91c0006-0000-0000-0000-000000000006";
    private static final String WRITE_ATTEMPT_API_KEY_UUID = "a91c0007-0000-0000-0000-000000000007";
    private static final String BUSINESS_API_ATTEMPT_API_KEY_UUID = "a91c0008-0000-0000-0000-000000000008";
    private static final String READ_ONLY_API_KEY_UUID = "a91c0009-0000-0000-0000-000000000009";
    private static final String SELF_INSPECTING_API_KEY_UUID = "a91c000a-0000-0000-0000-00000000000a";
    private static final String TEMPORARY_API_KEY_UUID = "a91c000b-0000-0000-0000-00000000000b";
    private static final String REVOKE_ATTEMPT_API_KEY_UUID = "a91c000c-0000-0000-0000-00000000000c";

    /**
     * Verifies that a global-admin can create an API_KEY subject which, once the global ADMIN
     * role got granted to it, authenticates as a global-admin just via its API-key.
     */
    @Test
    @Order(9610)
    @Produces("ApiKey-Subject: master.key")
    void aGlobalAdminCanCreateAnApiKeySubjectWithGlobalAdminRole() {
        new CreateApiKeySubjectWithGlobalAdminRole(scenarioTest, asGlobalAgent())
                .given("subjectUuid", VALID_MASTER_API_KEY_UUID)
                .given("subjectName", "master.key")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies that API_KEY subject names must match the API-key name grammar
     * (lowercase letters and digits with single dots as hierarchical separators)
     * which especially excludes '-', '@' and '/' to keep API-key names disjoint
     * from all username forms and from GROUP subject names.
     */
    @Test
    @Order(9620)
    void anApiKeySubjectNameMustMatchTheApiKeyNameGrammar() {
        new CreateApiKeySubjectWithGlobalAdminRole(scenarioTest, asGlobalAgent())
                .given("subjectUuid", INVALID_NAME_API_KEY_UUID)
                .given("subjectName", "master-api-key")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }

    /**
     * Verifies that only global-admins can create API_KEY subjects.
     */
    @Test
    @Order(9630)
    void aNonGlobalAdminCannotCreateApiKeySubjects() {
        new CreateApiKeySubjectWithGlobalAdminRole(scenarioTest,
                asSubject("tst-customer_admin_xxx").whichIs("an authenticated user without the global-admin role"))
                .given("subjectUuid", FORBIDDEN_API_KEY_UUID)
                .given("subjectName", "forbidden.key")
                .thenExpect(HttpStatus.FORBIDDEN);
    }

    /**
     * Verifies that a regular business API can be used with a valid API-key, acting as global-admin.
     * The API-key creation is part of this use-case because the clear-text API-key is only
     * available in the response of creating the API_KEY subject.
     */
    @Test
    @Order(9640)
    void aBusinessApiCanBeUsedWithAValidApiKeyActingAsGlobalAdmin() {
        new ViewMembershipsUsingApiKey(scenarioTest, asGlobalAgent())
                .given("subjectUuid", VALID_REPORTING_API_KEY_UUID)
                .given("subjectName", "reporting.key")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies that the provisioning API-key, provisioned from the configuration
     * (HSADMINNG_PROVISIONING_API_KEY) on application start, acts as global-admin and can
     * bootstrap further API-keys via the API, without any Keycloak login.
     */
    @Test
    @Order(9650)
    @Produces("ApiKey-Subject: bootstrapped.key")
    void theConfiguredProvisioningApiKeyCanCreateFurtherApiKeys() {
        new CreateFurtherApiKeyUsingTheProvisioningApiKey(scenarioTest)
                .given("subjectUuid", VALID_BOOTSTRAPPED_API_KEY_UUID)
                .given("subjectName", "bootstrapped.key")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies that a global-admin can create an API-key restricted to named endpoint-scopes,
     * here `rbac.subjects:sync`, which - together with the global ADMIN role for data visibility -
     * can sync ALL subjects, like needed for a Keycloak subject synchronization.
     */
    @Test
    @Order(9660)
    @Produces("ApiKey-Subject: subject.sync.key")
    void aGlobalAdminCanCreateAnEndpointScopedApiKeySubject() {
        new CreateEndpointScopedApiKeySubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", VALID_SUBJECT_SYNC_API_KEY_UUID)
                .given("subjectName", "subject.sync.key")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies that an API-key restricted to the endpoint-scope `rbac.subjects:sync` is rejected
     * (403) on endpoints outside its scope — e.g. POST /api/rbac/subjects — despite the global
     * ADMIN role granted to its API_KEY subject.
     */
    @Test
    @Order(9670)
    void anEndpointScopedApiKeyCannotUseOtherEndpoints() {
        new AttemptToCreateSubjectUsingEndpointScopedApiKey(scenarioTest, asGlobalAgent())
                .given("subjectUuid", WRITE_ATTEMPT_API_KEY_UUID)
                .given("subjectName", "write.attempt.key")
                .thenExpect(HttpStatus.FORBIDDEN);
    }

    /**
     * Verifies that an API-key restricted to the endpoint-scope `rbac.subjects:sync` cannot
     * use any business API, despite the global ADMIN role granted to its API_KEY subject.
     */
    @Test
    @Order(9680)
    void anEndpointScopedApiKeyCannotUseBusinessApis() {
        new AttemptToViewMembershipsUsingEndpointScopedApiKey(scenarioTest, asGlobalAgent())
                .given("subjectUuid", BUSINESS_API_ATTEMPT_API_KEY_UUID)
                .given("subjectName", "business.attempt.key")
                .thenExpect(HttpStatus.FORBIDDEN);
    }

    /**
     * Verifies that the endpoint-scope `*:read` makes an API-key read-only: with the global
     * ADMIN role it can use all GET endpoints, but nothing which changes data.
     */
    @Test
    @Order(9690)
    void aGlobalAdminCanCreateAReadOnlyApiKeySubject() {
        new CreateReadOnlyApiKeySubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", READ_ONLY_API_KEY_UUID)
                .given("subjectName", "readonly.key")
                .thenExpect(HttpStatus.CREATED);
    }

    /**
     * Verifies that the available API-key endpoint-scopes and the endpoints they allow
     * can be listed, e.g. to look up valid values for creating a scoped API-key.
     */
    @Test
    @Order(9700)
    void theAvailableApiKeyScopesCanBeListed() {
        new ViewApiKeyScopes(scenarioTest, asGlobalAgent())
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies that an API-key can inspect its own properties (subject, endpoint-scopes and
     * expiry timestamp) via `GET /api/rbac/context`, which is always allowed, even for
     * endpoint-scoped API-keys.
     */
    @Test
    @Order(9710)
    void anApiKeyCanInspectItsOwnProperties() {
        new InspectApiKeyContext(scenarioTest, asGlobalAgent())
                .given("subjectUuid", SELF_INSPECTING_API_KEY_UUID)
                .given("subjectName", "self.inspecting.key")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies that an API-key is revoked immediately and permanently by deleting its API_KEY
     * subject; as a safeguard, the DELETE request has to repeat the subject's name and type.
     */
    @Test
    @Order(9720)
    void anApiKeyCanBeRevokedByDeletingItsApiKeySubject() {
        new RevokeApiKeySubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", TEMPORARY_API_KEY_UUID)
                .given("subjectName", "temporary.key")
                .thenExpect(HttpStatus.NO_CONTENT);
    }

    /**
     * Verifies that an API-key is not revoked if the safeguard query-parameters of the DELETE
     * request do not match the subject identified by the UUID: the request is rejected with
     * 400 Bad Request and the API-key keeps authenticating.
     */
    @Test
    @Order(9730)
    void anApiKeyIsNotRevokedIfTheSafeguardParametersDoNotMatch() {
        new AttemptToRevokeApiKeySubjectWithMismatchingName(scenarioTest, asGlobalAgent())
                .given("subjectUuid", REVOKE_ATTEMPT_API_KEY_UUID)
                .given("subjectName", "revoke.attempt.key")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }
}
