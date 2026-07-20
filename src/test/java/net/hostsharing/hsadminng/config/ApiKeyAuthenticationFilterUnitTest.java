package net.hostsharing.hsadminng.config;

import lombok.val;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiKeyAuthenticationFilterUnitTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final FilterChain filterChain = mock(FilterChain.class);
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(jdbcTemplate);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withoutApiKeyHeaderPassesThroughUnauthenticated() throws Exception {
        // given
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void withValidApiKeyAuthenticatesAsRelatedSubject() throws Exception {
        // given
        val subjectUuid = UUID.randomUUID();
        val apiKey = ApiKey.generate("some.key");
        given(jdbcTemplate.queryForList(anyString(), eq(ApiKey.hash(apiKey))))
                .willReturn(List.of(subjectUuidAndScopeRow(subjectUuid, null)));
        val request = new MockHttpServletRequest();
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, apiKey);
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then the authentication looks like a real OIDC JWT authentication with the subject UUID as "sub",
        // marked as API-key authentication, without any scopes (unrestricted)
        verify(filterChain).doFilter(request, response);
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(subjectUuid.toString());
        val jwt = ((JwtAuthenticationToken) authentication).getToken();
        assertThat(jwt.getClaimAsString(ApiKeyAuthenticationFilter.TOKEN_TYPE_CLAIM))
                .isEqualTo(ApiKeyAuthenticationFilter.TOKEN_TYPE_API_KEY);
        assertThat(jwt.hasClaim(ApiKeyAuthenticationFilter.SCOPE_CLAIM)).isFalse();
        assertThat(jwt.hasClaim(ApiKeyAuthenticationFilter.API_KEY_EXPIRES_AT_CLAIM)).isFalse();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void withValidScopedApiKeyAuthenticatesWithScopeClaimAndAuthorities() throws Exception {
        // given
        val subjectUuid = UUID.randomUUID();
        val apiKey = ApiKey.generate("some.key");
        given(jdbcTemplate.queryForList(anyString(), eq(ApiKey.hash(apiKey))))
                .willReturn(List.of(
                        subjectUuidAndScopeRow(subjectUuid, "rbac.subjects:sync"),
                        subjectUuidAndScopeRow(subjectUuid, "other.things:read")));
        val request = new MockHttpServletRequest();
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, apiKey);
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then the synthesized JWT carries the scopes in the "scope" claim and as SCOPE_ authorities
        verify(filterChain).doFilter(request, response);
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        val jwt = ((JwtAuthenticationToken) authentication).getToken();
        assertThat(jwt.getClaimAsString(ApiKeyAuthenticationFilter.SCOPE_CLAIM))
                .isEqualTo("other.things:read rbac.subjects:sync");
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("SCOPE_rbac.subjects:sync", "SCOPE_other.things:read");
    }

    @Test
    void withExpiredApiKeyRejectsRequestAsUnauthorized() throws Exception {
        // given
        val subjectUuid = UUID.randomUUID();
        val apiKey = ApiKey.generate("expired.key");
        val row = subjectUuidAndScopeRow(subjectUuid, null);
        row.put("expires_at", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)));
        given(jdbcTemplate.queryForList(anyString(), eq(ApiKey.hash(apiKey))))
                .willReturn(List.of(row));
        val request = new MockHttpServletRequest();
        request.setRequestURI("/api/rbac/subjects");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, apiKey);
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("expired API-key");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void withNotYetExpiredApiKeyAuthenticatesAsRelatedSubject() throws Exception {
        // given
        val subjectUuid = UUID.randomUUID();
        val apiKey = ApiKey.generate("expiring.key");
        val expiresAt = java.time.Instant.now().plusSeconds(60);
        val row = subjectUuidAndScopeRow(subjectUuid, null);
        row.put("expires_at", java.sql.Timestamp.from(expiresAt));
        given(jdbcTemplate.queryForList(anyString(), eq(ApiKey.hash(apiKey))))
                .willReturn(List.of(row));
        val request = new MockHttpServletRequest();
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, apiKey);
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then the synthesized JWT also carries the API-key's expiry timestamp
        verify(filterChain).doFilter(request, response);
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo(subjectUuid.toString());
        val jwt = ((JwtAuthenticationToken) authentication).getToken();
        assertThat(jwt.getClaimAsInstant(ApiKeyAuthenticationFilter.API_KEY_EXPIRES_AT_CLAIM))
                .isEqualTo(expiresAt);
    }

    private static Map<String, Object> subjectUuidAndScopeRow(final UUID subjectUuid, final String scope) {
        final var row = new HashMap<String, Object>();
        row.put("uuid", subjectUuid);
        row.put("scope", scope);
        return row;
    }

    @Test
    void withBothAuthorizationAndApiKeyHeaderRejectsRequestAsUnauthorized() throws Exception {
        // given a request carrying both authentication headers, even with a valid API-key
        val request = new MockHttpServletRequest();
        request.setRequestURI("/api/rbac/subjects");
        request.addHeader("Authorization", "Bearer some-jwt");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, ApiKey.generate("some.key"));
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then the request is rejected without even looking up the API-key
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("multiple authentication methods are not allowed");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void withUnknownApiKeyRejectsRequestAsUnauthorized() throws Exception {
        // given
        given(jdbcTemplate.queryForList(anyString(), anyString()))
                .willReturn(List.of());
        val request = new MockHttpServletRequest();
        request.setRequestURI("/api/rbac/subjects");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "hsak_unknown");
        val response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid API-key");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
