package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.ACCESS_TOKEN_EXPIRES_IN_SECONDS;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.REFRESH_TOKEN_EXPIRES_IN_SECONDS;

/**
 * A minimal but complete OIDC provider for local development, so that frontends run their
 * standard authorization-code flow (PKCE) against it without any fake-jwt special handling.
 * It accepts any credentials; the password grant remains for CLI tools and tests.
 */
@RestController
@Profile("fake-jwt") // therefore, the whole controller is not active without this profile
@NoSecurityRequirement
@Slf4j
public class FakeJwtController {

    public static final String TOKEN_PATH = "/fake-jwt/token";
    public static final String AUTHORIZATION_PATH = "/fake-jwt/auth";
    public static final String LOGOUT_PATH = "/fake-jwt/logout";
    public static final String USERINFO_PATH = "/fake-jwt/userinfo";
    public static final String DISCOVERY_PATH = "/fake-jwt/.well-known/openid-configuration";
    public static final String JWKS_PATH = "/fake-jwt/.well-known/jwks.json";

    private static final int AUTHORIZATION_CODE_EXPIRES_IN_SECONDS = 60;

    private final String issuer;
    private final FakeJwtSelectableUsers selectableUsers;
    private final Set<String> allowedRedirectHosts;
    private final Map<String, PendingAuthorization> pendingAuthorizations = new ConcurrentHashMap<>();

    // fallback logout target for clients which omit post_logout_redirect_uri (single-developer use)
    private volatile String lastKnownClientOrigin;

    public FakeJwtController(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuer,
            @Value("${hsadminng.fake-jwt.client-hosts:localhost, 127.0.0.1, [::1]}") final String clientHosts,
            final FakeJwtSelectableUsers selectableUsers) {
        this.issuer = issuer;
        this.selectableUsers = selectableUsers;
        this.allowedRedirectHosts = Stream.of(clientHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping(value = DISCOVERY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed("app.config.jwt.discovery")
    public Map<String, Object> openidConfiguration() {
        val config = new LinkedHashMap<String, Object>();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/auth");
        config.put("token_endpoint", issuer + "/token");
        config.put("userinfo_endpoint", issuer + "/userinfo");
        config.put("end_session_endpoint", issuer + "/logout");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("response_types_supported", List.of("code"));
        config.put("grant_types_supported", List.of("authorization_code", "refresh_token", "password"));
        config.put("subject_types_supported", List.of("public"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));
        config.put("code_challenge_methods_supported", List.of("S256", "plain"));
        config.put("scopes_supported", List.of("openid", "profile", "email"));
        config.put("token_endpoint_auth_methods_supported", List.of("none"));
        config.put("claims_supported", List.of("sub", "preferred_username"));
        return config;
    }

    @GetMapping(value = JWKS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed("app.config.jwt.jwks")
    public String jwks() {
        return JwtFakeBearer.publicJwkSetJson();
    }

    @GetMapping(value = AUTHORIZATION_PATH, produces = MediaType.TEXT_HTML_VALUE)
    @Timed("app.config.jwt.authForm")
    public String authorizationForm(final HttpServletRequest request) {
        validateAuthorizationRequest(request.getParameter("response_type"),
                request.getParameter("client_id"), request.getParameter("redirect_uri"));

        val hiddenFields = new StringBuilder();
        request.getParameterMap().forEach((name, values) -> hiddenFields
                .append("<input type=\"hidden\" name=\"").append(htmlEscape(name))
                .append("\" value=\"").append(htmlEscape(values[0])).append("\">\n"));

        // a dropdown of the existing users; a free-text fallback keeps an empty database bootstrappable
        val userNames = selectableUsers.findAllActiveUserSubjectNames();
        val usernameField = userNames.isEmpty()
                ? "<input type=\"text\" name=\"username\" autofocus>"
                : userNames.stream()
                        .map(name -> "<option>" + htmlEscape(name) + "</option>")
                        .collect(Collectors.joining(
                                "\n", "<select name=\"username\" autofocus>\n", "\n</select>"));
        // loosely mimics the hsadmin-ng frontend login page: Hostsharing blue on a light Bootstrap-like layout
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Fake OIDC Login</title>
                <style>
                    body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
                           font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                           background-color: #f8f9fa; color: #212529; }
                    main { background: #fff; border: 1px solid #dee2e6; border-radius: 4px; padding: 2rem; width: 22rem; }
                    h1 { margin: 0 0 .5rem; font-size: 1.6rem; font-weight: 400; color: #005094; }
                    p { margin: 0 0 1.5rem; font-size: .9rem; color: #6c757d; }
                    label { display: block; margin: .5rem 0; font-weight: 500; }
                    input, select { display: block; box-sizing: border-box; width: 100%%; margin-top: .25rem; padding: .45rem .6rem;
                            font-size: .95rem; background: #fff; border: 1px solid #dee2e6; border-radius: 4px; }
                    button { margin-top: 1rem; padding: .35rem 1rem; font-size: .9rem; font-weight: 500;
                             color: #005094; background: #fff; border: 2px solid #005094; border-radius: 4px; cursor: pointer; }
                    button:hover { color: #fff; background: #005094; }
                </style>
                </head>
                <body>
                <main>
                <h1>Fake OIDC Login</h1>
                <p>Local development login.</p>
                <form method="post" action="%s">
                %s<label>User %s</label>
                <button type="submit">Sign In</button>
                </form>
                </main>
                </body>
                </html>
                """.formatted(AUTHORIZATION_PATH, hiddenFields, usernameField);
    }

    @PostMapping(value = AUTHORIZATION_PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Timed("app.config.jwt.authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam(name = "response_type", required = false) final String responseType,
            @RequestParam(name = "client_id", required = false) final String clientId,
            @RequestParam(name = "redirect_uri", required = false) final String redirectUri,
            @RequestParam(name = "state", required = false) final String state,
            @RequestParam(name = "nonce", required = false) final String nonce,
            @RequestParam(name = "code_challenge", required = false) final String codeChallenge,
            @RequestParam(name = "code_challenge_method", defaultValue = "plain") final String codeChallengeMethod,
            @RequestParam(name = "username", required = false) final String username) {
        validateAuthorizationRequest(responseType, clientId, redirectUri);
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (codeChallenge != null && !Set.of("S256", "plain").contains(codeChallengeMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported code_challenge_method");
        }

        lastKnownClientOrigin = URI.create(redirectUri).resolve("/").toString();

        val code = UUID.randomUUID().toString();
        pendingAuthorizations.put(code, new PendingAuthorization(
                username, clientId, redirectUri, nonce, codeChallenge, codeChallengeMethod,
                Instant.now().plusSeconds(AUTHORIZATION_CODE_EXPIRES_IN_SECONDS)));
        log.info("Fake OIDC: Issuing authorization code for user: {}", username);

        var location = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "code=" + urlEncode(code);
        if (state != null) {
            location += "&state=" + urlEncode(state);
        }
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
    }

    @PostMapping(value = TOKEN_PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Timed("app.config.jwt.token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam(name = "grant_type", defaultValue = "password") final String grantType,
            @RequestParam(name = "client_id", required = false) final String clientId,
            @RequestParam(name = "code", required = false) final String code,
            @RequestParam(name = "redirect_uri", required = false) final String redirectUri,
            @RequestParam(name = "code_verifier", required = false) final String codeVerifier,
            @RequestParam(name = "username", required = false) final String username,
            @RequestParam(name = "password", required = false) final String password,
            @RequestParam(name = "refresh_token", required = false) final String refreshToken,
            @RequestParam(defaultValue = "openid profile") final String scope) {

        return switch (grantType) {
            case "authorization_code" -> {
                val pending = consumeAuthorizationCode(code, clientId, redirectUri, codeVerifier);
                yield tokenResponse(pending.username(), scope, clientId, pending.nonce());
            }
            case "refresh_token" -> tokenResponse(subjectFromRefreshToken(refreshToken), scope, clientId, null);
            case "password" -> {
                // Accept any username/password for local testing
                log.info("Fake JWT: Issuing token for user: {}", username);
                yield tokenResponse(username, scope, clientId, null);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported grant_type");
        };
    }

    @GetMapping(USERINFO_PATH)
    @Timed("app.config.jwt.userinfo")
    public Map<String, Object> userinfo(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) final String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }
        try {
            val subject = JwtFakeBearer.verifiedAccessTokenSubject(authorization.substring("Bearer ".length()));
            return Map.of("sub", subject, "preferred_username", subject);
        } catch (final Exception exc) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid bearer token", exc);
        }
    }

    @GetMapping(LOGOUT_PATH)
    @Timed("app.config.jwt.logout")
    public ResponseEntity<String> logout(
            @RequestParam(name = "post_logout_redirect_uri", required = false) final String postLogoutRedirectUri,
            @RequestParam(name = "state", required = false) final String state) {
        // the provider keeps no session, so logging out only means returning to the client;
        // without an explicit post_logout_redirect_uri, the last known client origin serves as target:
        // the client then starts a fresh authorization request and thus shows the login form again
        // (redirecting to the form directly cannot work, state/nonce/PKCE must come fresh from the client)
        val target = postLogoutRedirectUri != null ? postLogoutRedirectUri : lastKnownClientOrigin;
        if (target == null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("<html><body>Logged out.</body></html>");
        }
        requireAllowedRedirectUri(target, "post_logout_redirect_uri");
        var location = target;
        if (state != null) {
            location += (location.contains("?") ? "&" : "?") + "state=" + urlEncode(state);
        }
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
    }

    private ResponseEntity<Map<String, Object>> tokenResponse(
            final String subject, final String scope, final String clientId, final String nonce) {
        // the fake token endpoint has no group source, so it issues tokens without group claims
        val accessToken = JwtFakeBearer.accessToken(subject, List.of());
        val refreshToken = JwtFakeBearer.refreshToken(subject);

        val response = new LinkedHashMap<String, Object>();
        response.put("access_token", accessToken);
        response.put("refresh_token", refreshToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", ACCESS_TOKEN_EXPIRES_IN_SECONDS);
        response.put("refresh_expires_in", REFRESH_TOKEN_EXPIRES_IN_SECONDS);
        response.put("scope", scope);
        if (clientId != null && scope.contains("openid")) {
            response.put("id_token", JwtFakeBearer.idToken(issuer, subject, clientId, nonce));
        }
        return ResponseEntity.ok(response);
    }

    private PendingAuthorization consumeAuthorizationCode(
            final String code, final String clientId, final String redirectUri, final String codeVerifier) {
        pendingAuthorizations.values().removeIf(pending -> pending.expiresAt().isBefore(Instant.now()));
        val pending = code == null ? null : pendingAuthorizations.remove(code);
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or expired code");
        }
        if (!pending.clientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client_id mismatch");
        }
        if (!pending.redirectUri().equals(redirectUri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri mismatch");
        }
        if (pending.codeChallenge() != null && !pending.matchesVerifier(codeVerifier)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid code_verifier");
        }
        return pending;
    }

    private void validateAuthorizationRequest(final String responseType, final String clientId, final String redirectUri) {
        if (!"code".equals(responseType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "response_type must be 'code'");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client_id is required");
        }
        requireAllowedRedirectUri(redirectUri, "redirect_uri");
    }

    private void requireAllowedRedirectUri(final String uri, final String parameterName) {
        try {
            val parsed = URI.create(uri);
            val scheme = parsed.getScheme() == null ? null : parsed.getScheme().toLowerCase();
            if (!Set.of("http", "https").contains(scheme)
                    || parsed.getRawFragment() != null
                    || !allowedRedirectHosts.contains(parsed.getHost())) {
                throw new IllegalArgumentException("not an absolute fragment-free http(s) URI on an allowed host: " + uri);
            }
        } catch (final Exception exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    parameterName + " must be an absolute fragment-free http(s) URI on an allowed redirect host", exc);
        }
    }

    private String subjectFromRefreshToken(final String refreshToken) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token is required");
        }
        try {
            return JwtFakeBearer.subjectFromRefreshToken(refreshToken);
        } catch (final Exception exc) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh_token", exc);
        }
    }

    private static String htmlEscape(final String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String urlEncode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** One issued but not yet redeemed authorization code of the fake OIDC flow. */
    record PendingAuthorization(
            String username,
            String clientId,
            String redirectUri,
            String nonce,
            String codeChallenge,
            String codeChallengeMethod,
            Instant expiresAt) {

        @SneakyThrows
        boolean matchesVerifier(final String codeVerifier) {
            if (codeVerifier == null) {
                return false;
            }
            if ("S256".equals(codeChallengeMethod)) {
                val hash = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                return codeChallenge.equals(Base64.getUrlEncoder().withoutPadding().encodeToString(hash));
            }
            return codeChallenge.equals(codeVerifier);
        }
    }
}
