package net.hostsharing.hsadminng.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Named endpoint-scopes for API-keys, the single source of truth mapping each scope name
 * to its allowlist of HTTP-method+path patterns.
 *
 * The scope names are stored per API-key in rbac.api_key_scope and enforced fail-closed by
 * the ApiKeyScopeEnforcementFilter: a scoped API-key may only call endpoints matched by at
 * least one of its scopes. An API-key without scopes is unrestricted, limited only by the
 * roles granted to its subject.
 *
 * Each enum constant must have a counterpart with the same wire-name in the generated
 * ApiKeyScopeResource enum, i.e. as an enum value in the OpenAPI YAML. The constant names
 * may differ, e.g. for `*:read` the generator strips the `*`.
 */
public enum ApiKeyScope {

    RBAC_SUBJECT_SYNC(
            "rbac.subjects:sync",
            endpoint(HttpMethod.GET, "/api/rbac/subjects"),
            endpoint(HttpMethod.GET, "/api/rbac/subjects/{uuid}"),
            // deliberately without .../{uuid}/permissions, which is not needed by the sync
            endpoint(HttpMethod.PUT, "/api/rbac/subjects/{uuid}")
            // without POST /api/rbac/subjects - as it would allow creation of unrestricted API-Keys
            // without DELETE /api/rbac/subjects/{uuid} - the sync deactivates via PUT with deactivated=true;
            // together with the PUT rejection of type API_KEY, a sync-key cannot touch API-keys at all
    ),

    // read-only API-keys: any GET endpoint, but nothing which changes data
    ANY_READ(
            "*:read",
            endpoint(HttpMethod.GET, "/api/**"));

    private record Endpoint(HttpMethod method, String pathPattern, RequestMatcher matcher) {

        String display() {
            return method + " " + pathPattern;
        }
    }

    private final String wireName;
    private final List<Endpoint> endpoints;

    ApiKeyScope(final String wireName, final Endpoint... endpoints) {
        this.wireName = wireName;
        this.endpoints = List.of(endpoints);
    }

    public String wireName() {
        return wireName;
    }

    public boolean allows(final HttpServletRequest request) {
        return endpoints.stream().anyMatch(endpoint -> endpoint.matcher().matches(request));
    }

    /** The allowed endpoints as human-readable HTTP-method and path pattern, e.g. for GET /api/rbac/scopes. */
    public List<String> allowedEndpoints() {
        return endpoints.stream().map(Endpoint::display).toList();
    }

    // unknown wire-names, e.g. from a DB row of a removed scope, resolve to empty and thus never match
    public static Optional<ApiKeyScope> forWireName(final String wireName) {
        return Arrays.stream(values()).filter(scope -> scope.wireName.equals(wireName)).findFirst();
    }

    private static Endpoint endpoint(final HttpMethod method, final String pathPattern) {
        return new Endpoint(method, pathPattern, PathPatternRequestMatcher.withDefaults().matcher(method, pathPattern));
    }
}
