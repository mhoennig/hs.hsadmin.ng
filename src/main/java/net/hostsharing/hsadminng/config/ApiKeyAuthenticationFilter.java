package net.hostsharing.hsadminng.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Authenticates requests carrying an API-key in the "Hostsharing-Api-Key" header,
 * as an alternative to a Keycloak OIDC JWT in the "Authorization" header.
 *
 * The synthesized in-memory JwtAuthenticationToken has the same shape as a real OIDC JWT, but
 * without any issuer, signature or JwtDecoder involved. Both headers at once are rejected with
 * 401: there is no precedence between authentication methods. The "api_key_expires_at" claim is
 * distinct from the standard "exp" claim, which only bounds the lifetime of the in-memory JWT.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "Hostsharing-Api-Key";

    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String TOKEN_TYPE_API_KEY = "api-key";
    public static final String SCOPE_CLAIM = "scope";
    public static final String API_KEY_EXPIRES_AT_CLAIM = "api_key_expires_at";

    private static final String SUBJECT_UUID_AND_SCOPES_BY_API_KEY_HASH_SQL = """
            select s.uuid, ak.expires_at, sc.scope
                from rbac.api_key ak
                join rbac.subject s on s.uuid = ak.uuid
                left join rbac.api_key_scope sc on sc.apiKeyUuid = ak.uuid
                where ak.keyHash = ?
                  and s.type = 'API_KEY'
                  and s.deactivated_at is null
            """;

    private final JdbcTemplate jdbcTemplate;

    ApiKeyAuthenticationFilter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final var apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            rejectAsUnauthorized(request, response,
                    "multiple authentication methods are not allowed:"
                            + " use either the Authorization header or the Hostsharing-Api-Key header, not both");
            return;
        }

        final var rows = jdbcTemplate.queryForList(
                SUBJECT_UUID_AND_SCOPES_BY_API_KEY_HASH_SQL, ApiKey.hash(apiKey));
        if (rows.isEmpty()) {
            rejectAsUnauthorized(request, response, "invalid API-key");
            return;
        }
        final var expiresAtTimestamp = (java.sql.Timestamp) rows.getFirst().get("expires_at");
        final var expiresAt = expiresAtTimestamp == null ? null : expiresAtTimestamp.toInstant();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            rejectAsUnauthorized(request, response, "expired API-key");
            return;
        }
        final var subjectUuid = (UUID) rows.getFirst().get("uuid");
        final var scopes = rows.stream()
                .map(row -> (String) row.get("scope"))
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(synthesizedJwt(subjectUuid, scopes, expiresAt), scopeAuthorities(scopes)));
        filterChain.doFilter(request, response);
    }

    /** The JWT of the current authentication, if the request was authenticated with an API-key. */
    public static Optional<Jwt> currentApiKeyJwt() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth
                && TOKEN_TYPE_API_KEY.equals(jwtAuth.getToken().getClaimAsString(TOKEN_TYPE_CLAIM))) {
            return Optional.of(jwtAuth.getToken());
        }
        return Optional.empty();
    }

    private static Jwt synthesizedJwt(final UUID subjectUuid, final List<String> scopes, final Instant apiKeyExpiresAt) {
        final var now = Instant.now();
        final var jwtBuilder = Jwt.withTokenValue("api-key") // placeholder, this token value is never parsed nor validated
                .header("alg", "none")
                .subject(subjectUuid.toString())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_API_KEY)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
        if (!scopes.isEmpty()) {
            jwtBuilder.claim(SCOPE_CLAIM, String.join(" ", scopes));
        }
        if (apiKeyExpiresAt != null) {
            jwtBuilder.claim(API_KEY_EXPIRES_AT_CLAIM, apiKeyExpiresAt);
        }
        return jwtBuilder.build();
    }

    private static List<GrantedAuthority> scopeAuthorities(final List<String> scopes) {
        return scopes.stream()
                .<GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
    }

    private static void rejectAsUnauthorized(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"path":"%s","statusCode":401,"statusPhrase":"Unauthorized","message":"ERROR: [401] %s"}"""
                .formatted(request.getRequestURI(), reason));
    }
}
