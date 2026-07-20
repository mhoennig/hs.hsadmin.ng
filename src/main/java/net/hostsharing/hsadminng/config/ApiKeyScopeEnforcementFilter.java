package net.hostsharing.hsadminng.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Enforces the endpoint-scopes of a scoped API-key fail-closed: a request must be matched by
 * one of the key's scopes, else 403. Unscoped API-keys and Keycloak OIDC JWTs are unaffected.
 */
class ApiKeyScopeEnforcementFilter extends OncePerRequestFilter {

    // GET-only endpoints every API-key may reach regardless of its scopes:
    private static final List<RequestMatcher> OPENLY_ACCESSIBLE_API_ENDPOINTS = List.of(
            endpoint(HttpMethod.GET, "/api/ping"), // verify that the server can be reached
            endpoint(HttpMethod.GET, "/api/version"), // get version information, e.g. to adapt its behavior
            endpoint(HttpMethod.GET, "/api/rbac/context") // inspect its own subject, scopes and expiry
    );

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final var scopes = scopesOfApiKeyAuthentication();
        if (scopes.isEmpty() || isOpenlyAccessible(request) || anyScopeAllows(scopes, request)) {
            filterChain.doFilter(request, response);
            return;
        }
        rejectOutOfScopeRequest(request, response);
    }

    private static List<String> scopesOfApiKeyAuthentication() {
        return ApiKeyAuthenticationFilter.currentApiKeyJwt()
                .map(jwt -> jwt.getClaimAsString(ApiKeyAuthenticationFilter.SCOPE_CLAIM))
                .filter(scopeClaim -> !scopeClaim.isBlank())
                .map(scopeClaim -> List.of(scopeClaim.split(" ")))
                .orElse(List.of());
    }

    private static boolean isOpenlyAccessible(final HttpServletRequest request) {
        // outside /api is, for example, OpenAPI documentation
        return !request.getRequestURI().startsWith("/api/")
                || OPENLY_ACCESSIBLE_API_ENDPOINTS.stream().anyMatch(matcher -> matcher.matches(request));
    }

    private static boolean anyScopeAllows(final List<String> scopes, final HttpServletRequest request) {
        return scopes.stream()
                .map(ApiKeyScope::forWireName)
                .flatMap(Optional::stream)
                .anyMatch(scope -> scope.allows(request));
    }

    private static void rejectOutOfScopeRequest(
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"path":"%s","statusCode":403,"statusPhrase":"Forbidden","message":"ERROR: [403] API-key scopes do not allow %s %s"}"""
                .formatted(request.getRequestURI(), request.getMethod(), request.getRequestURI()));
    }

    private static RequestMatcher endpoint(final HttpMethod method, final String pathPattern) {
        return PathPatternRequestMatcher.withDefaults().matcher(method, pathPattern);
    }
}
