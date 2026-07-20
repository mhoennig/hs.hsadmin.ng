package net.hostsharing.hsadminng.config;

import jakarta.servlet.FilterChain;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiKeyScopeEnforcementFilterUnitTest {

    private final FilterChain filterChain = mock(FilterChain.class);
    private final ApiKeyScopeEnforcementFilter filter = new ApiKeyScopeEnforcementFilter();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestPassesThrough() throws Exception {
        // given
        val request = new MockHttpServletRequest("DELETE", "/api/rbac/subjects/" + UUID.randomUUID());
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void keycloakJwtAuthenticationPassesThroughUnrestricted() throws Exception {
        // given a real OIDC JWT authentication, which has no "token_type" claim
        authenticate(jwtBuilder().build());
        val request = new MockHttpServletRequest("DELETE", "/api/rbac/subjects/" + UUID.randomUUID());
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unscopedApiKeyAuthenticationPassesThroughUnrestricted() throws Exception {
        // given
        authenticate(apiKeyJwtBuilder().build());
        val request = new MockHttpServletRequest("DELETE", "/api/rbac/subjects/" + UUID.randomUUID());
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void scopedApiKeyAuthenticationPassesThroughToMatchingEndpoint() throws Exception {
        // given
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "rbac.subjects:sync").build());
        val request = new MockHttpServletRequest("GET", "/api/rbac/subjects/" + UUID.randomUUID());
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void scopedApiKeyAuthenticationPassesThroughToNonApiPathsAndAlwaysAllowedApiPaths() throws Exception {
        // given
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "rbac.subjects:sync").build());

        // GET "/api/rbac/context" is always allowed to let every API-key inspect its own properties
        for (val path : List.of("/actuator/health", "/swagger-ui/index.html", "/api/ping", "/api/version", "/api/rbac/context")) {
            val request = new MockHttpServletRequest("GET", path);
            val response = new MockHttpServletResponse();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    void scopedApiKeyAuthenticationGetsEnforcedForNonGetOnAlwaysAllowedApiPath() throws Exception {
        // given the always-allowed exemption is GET-only, so a non-GET on the same path is enforced
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "rbac.subjects:sync").build());
        val request = new MockHttpServletRequest("POST", "/api/rbac/context");
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then it is not exempt and, not matched by the sync-scope, gets rejected fail-closed
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("API-key scopes do not allow POST /api/rbac/context");
    }

    @Test
    void scopedApiKeyAuthenticationGetsRejectedForOutOfScopeEndpoint() throws Exception {
        // given
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "rbac.subjects:sync").build());
        val request = new MockHttpServletRequest("POST", "/api/rbac/subjects");
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("API-key scopes do not allow POST /api/rbac/subjects");
    }

    @Test
    void scopedApiKeyAuthenticationGetsRejectedForDeletingSubjects() throws Exception {
        // given the sync-scope, which deliberately excludes DELETE (the sync deactivates via PUT instead)
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "rbac.subjects:sync").build());
        val request = new MockHttpServletRequest("DELETE", "/api/rbac/subjects/" + UUID.randomUUID());
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("API-key scopes do not allow DELETE /api/rbac/subjects/");
    }

    @Test
    void apiKeyAuthenticationWithUnknownScopeGetsRejectedFailClosed() throws Exception {
        // given a scope name without any endpoint mapping, e.g. from a DB row of a removed scope
        authenticate(apiKeyJwtBuilder()
                .claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, "unknown:scope").build());
        val request = new MockHttpServletRequest("GET", "/api/rbac/subjects");
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private static Jwt.Builder jwtBuilder() {
        final var now = Instant.now();
        return Jwt.withTokenValue("test")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
    }

    private static Jwt.Builder apiKeyJwtBuilder() {
        return jwtBuilder()
                .claim(ApiKeyAuthenticationFilter.TOKEN_TYPE_CLAIM, ApiKeyAuthenticationFilter.TOKEN_TYPE_API_KEY);
    }

    private static void authenticate(final Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
