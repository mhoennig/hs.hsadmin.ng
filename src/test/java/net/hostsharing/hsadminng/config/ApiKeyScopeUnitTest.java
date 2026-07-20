package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyScopeUnitTest {

    @ParameterizedTest
    @CsvSource({
            "GET, /api/rbac/subjects, true",
            "GET, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000, true",
            "PUT, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000, true",
            "POST, /api/rbac/subjects, false",
            "DELETE, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000, false",
            "GET, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000/permissions, false",
            "GET, /api/rbac/roles, false",
            "GET, /api/hs/office/memberships, false",
            "POST, /api/rbac/grants, false",
    })
    void rbacSubjectSyncAllowsSyncingSubjects(final String method, final String path, final boolean allowed) {
        // given
        val request = new MockHttpServletRequest(method, path);

        // then
        assertThat(ApiKeyScope.RBAC_SUBJECT_SYNC.allows(request)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/rbac/subjects, true",
            "GET, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000/permissions, true",
            "GET, /api/hs/office/memberships, true",
            "GET, /api/hs/accounts/current, true",
            "POST, /api/rbac/subjects, false",
            "PUT, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000, false",
            "DELETE, /api/rbac/subjects/40e2a1e8-0000-0000-0000-000000000000, false",
            "POST, /api/rbac/grants, false",
            "PATCH, /api/hs/hosting/assets/40e2a1e8-0000-0000-0000-000000000000, false",
    })
    void anyReadAllowsAllGetEndpointsButNoWrites(final String method, final String path, final boolean allowed) {
        // given
        val request = new MockHttpServletRequest(method, path);

        // then
        assertThat(ApiKeyScope.ANY_READ.allows(request)).isEqualTo(allowed);
    }

    @Test
    void forWireNameResolvesAllScopesAndRejectsUnknownNames() {
        for (val scope : ApiKeyScope.values()) {
            assertThat(ApiKeyScope.forWireName(scope.wireName())).contains(scope);
        }
        assertThat(ApiKeyScope.forWireName("unknown:scope")).isEmpty();
    }

    // the parity check of this enum against the generated OpenAPI ApiKeyScopeResource enum lives in
    // RbacSubjectControllerRestTest to avoid a config -> rbac package cycle (ArchitectureTest)
}
