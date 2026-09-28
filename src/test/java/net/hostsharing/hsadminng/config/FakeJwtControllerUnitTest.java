package net.hostsharing.hsadminng.config;

import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.REFRESH_TOKEN_EXPIRES_IN_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

class FakeJwtControllerUnitTest {

    private static final String ISSUER = "http://localhost:8080/fake-jwt";
    private static final String CLIENT_ID = "test-client";
    private static final String REDIRECT_URI = "http://localhost:3000/callback";
    private static final String PKCE_VERIFIER = "unit-test-pkce-verifier-0123456789abcdef";
    private static final String DEFAULT_CLIENT_HOSTS = "localhost, 127.0.0.1, [::1]";

    private final FakeJwtController controller =
            new FakeJwtController(ISSUER, DEFAULT_CLIENT_HOSTS, mock(FakeJwtSelectableUsers.class));

    @Test
    void passwordGrantReturnsAccessAndRefreshToken() {
        val response = passwordGrant("openid profile");

        val body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body)
                .containsEntry("token_type", "Bearer")
                .containsEntry("expires_in", 3600)
                .containsEntry("refresh_expires_in", REFRESH_TOKEN_EXPIRES_IN_SECONDS)
                .containsEntry("scope", "openid profile")
                .containsKeys("access_token", "refresh_token");
        assertThat(subjectOf((String) body.get("access_token"))).isEqualTo("fake-user");
        assertThat(JwtFakeBearer.subjectFromRefreshToken((String) body.get("refresh_token"))).isEqualTo("fake-user");
    }

    @Test
    void refreshTokenJwtExpiresAfter72HoursNotAfterOneHour() {
        val response = passwordGrant("openid");

        val accessToken = (String) response.getBody().get("access_token");
        val refreshToken = (String) response.getBody().get("refresh_token");
        val accessExpirationMillis = expirationMillisOf(accessToken) - System.currentTimeMillis();
        val refreshExpirationMillis = expirationMillisOf(refreshToken) - System.currentTimeMillis();

        assertThat(accessExpirationMillis).isBetween(
                (3600L - 5L) * 1000L,
                (3600L + 5L) * 1000L);
        assertThat(refreshExpirationMillis).isBetween(
                (REFRESH_TOKEN_EXPIRES_IN_SECONDS - 5L) * 1000L,
                (REFRESH_TOKEN_EXPIRES_IN_SECONDS + 5L) * 1000L);
    }

    @Test
    void refreshGrantReturnsNewAccessTokenForRefreshToken() {
        val passwordGrantBody = passwordGrant("openid").getBody();
        val refreshToken = (String) passwordGrantBody.get("refresh_token");

        val refreshGrantResponse = controller.token(
                "refresh_token", null, null, null, null, null, null, refreshToken, "openid");

        val refreshGrantBody = refreshGrantResponse.getBody();
        assertThat(refreshGrantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshGrantBody.get("access_token")).isNotEqualTo(passwordGrantBody.get("access_token"));
        assertThat(subjectOf((String) refreshGrantBody.get("access_token"))).isEqualTo("fake-user");
        assertThat(JwtFakeBearer.subjectFromRefreshToken((String) refreshGrantBody.get("refresh_token"))).isEqualTo("fake-user");
    }

    @Test
    void refreshGrantRequiresRefreshTokenParameter() {
        val exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> controller.token("refresh_token", null, null, null, null, null, null, null, "openid"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshGrantRejectsInvalidRefreshToken() {
        val exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> controller.token(
                        "refresh_token", null, null, null, null, null, null, "not-a-refresh-token", "openid"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void discoveryDocumentDerivesAllEndpointsFromTheIssuer() {
        val discovery = controller.openidConfiguration();

        assertThat(discovery)
                .containsEntry("issuer", ISSUER)
                .containsEntry("authorization_endpoint", ISSUER + "/auth")
                .containsEntry("token_endpoint", ISSUER + "/token")
                .containsEntry("jwks_uri", ISSUER + "/.well-known/jwks.json")
                .containsEntry("end_session_endpoint", ISSUER + "/logout")
                .containsKeys("response_types_supported", "code_challenge_methods_supported");
    }

    @Test
    @SneakyThrows
    void authorizationCodeFlowWithPkceReturnsTokensIncludingIdToken() {
        val authorization = controller.authorize(
                "code", CLIENT_ID, REDIRECT_URI, "some-state", "some-nonce",
                pkceChallenge(PKCE_VERIFIER), "S256", "code-flow-user");
        assertThat(authorization.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        val location = authorization.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location).startsWith(REDIRECT_URI + "?code=").contains("&state=some-state");
        val code = location.substring(location.indexOf("code=") + 5, location.indexOf("&state="));

        val tokenResponse = controller.token(
                "authorization_code", CLIENT_ID, code, REDIRECT_URI, PKCE_VERIFIER, null, null, null, "openid profile");

        val body = tokenResponse.getBody();
        assertThat(subjectOf((String) body.get("access_token"))).isEqualTo("code-flow-user");
        val idTokenClaims = SignedJWT.parse((String) body.get("id_token")).getJWTClaimsSet();
        assertThat(idTokenClaims.getIssuer()).isEqualTo(ISSUER);
        assertThat(idTokenClaims.getSubject()).isEqualTo("code-flow-user");
        assertThat(idTokenClaims.getAudience()).containsExactly(CLIENT_ID);
        assertThat(idTokenClaims.getStringClaim("nonce")).isEqualTo("some-nonce");
    }

    @Test
    void authorizationCodeIsSingleUseAndBoundToClientRedirectAndVerifier() {
        val code = issueAuthorizationCode();

        // wrong PKCE verifier
        assertBadRequest(() -> controller.token(
                "authorization_code", CLIENT_ID, code, REDIRECT_URI, "wrong-verifier", null, null, null, "openid"));
        // the failed attempt already consumed the code
        assertBadRequest(() -> controller.token(
                "authorization_code", CLIENT_ID, code, REDIRECT_URI, PKCE_VERIFIER, null, null, null, "openid"));

        val freshCode = issueAuthorizationCode();
        assertBadRequest(() -> controller.token(
                "authorization_code", "other-client", freshCode, REDIRECT_URI, PKCE_VERIFIER, null, null, null, "openid"));
    }

    @Test
    void authorizationCodeWithPlainChallengeRedeemsWithTheLiteralVerifier() {
        val location = controller.authorize(
                        "code", CLIENT_ID, REDIRECT_URI, null, null, "plain-secret", "plain", "plain-user")
                .getHeaders().getFirst(HttpHeaders.LOCATION);
        val code = location.substring(location.indexOf("code=") + 5);

        // a missing verifier is rejected, and the code is consumed by that attempt
        assertBadRequest(() -> controller.token(
                "authorization_code", CLIENT_ID, code, REDIRECT_URI, null, null, null, null, "openid"));

        val freshLocation = controller.authorize(
                        "code", CLIENT_ID, REDIRECT_URI, null, null, "plain-secret", "plain", "plain-user")
                .getHeaders().getFirst(HttpHeaders.LOCATION);
        val freshCode = freshLocation.substring(freshLocation.indexOf("code=") + 5);
        val tokenResponse = controller.token(
                "authorization_code", CLIENT_ID, freshCode, REDIRECT_URI, "plain-secret", null, null, null, "openid");

        assertThat(subjectOf((String) tokenResponse.getBody().get("access_token"))).isEqualTo("plain-user");
    }

    @Test
    void authorizationRejectsNonLoopbackRedirectUris() {
        val exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> controller.authorize(
                        "code", CLIENT_ID, "https://evil.example/callback", null, null, null, "plain", "someone"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void authorizationAcceptsConfiguredClientHostsButNoOthers() {
        val configured = new FakeJwtController(
                ISSUER, " frontend.dev.example.org , other.example.org ", mock(FakeJwtSelectableUsers.class));

        val authorization = configured.authorize(
                "code", CLIENT_ID, "https://frontend.dev.example.org/callback", null, null, null, "plain", "someone");
        assertThat(authorization.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        val other = configured.authorize(
                "code", CLIENT_ID, "https://other.example.org/callback", null, null, null, "plain", "someone");
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        for (val redirectUri : List.of(REDIRECT_URI, "https://evil.example/callback")) {
            val exception = catchThrowableOfType(
                    ResponseStatusException.class,
                    () -> configured.authorize(
                            "code", CLIENT_ID, redirectUri, null, null, null, "plain", "someone"));
            assertThat(exception.getStatusCode()).describedAs(redirectUri).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void authorizationRejectsRelativeSchemelessNonHttpAndFragmentRedirectUris() {
        for (val redirectUri : List.of(
                "/callback",
                "//localhost:3000/callback",
                "ftp://localhost/callback",
                "http://localhost:3000/callback#fragment")) {
            val exception = catchThrowableOfType(
                    ResponseStatusException.class,
                    () -> controller.authorize("code", CLIENT_ID, redirectUri, null, null, null, "plain", "someone"));

            assertThat(exception).describedAs(redirectUri).isNotNull();
            assertThat(exception.getStatusCode()).describedAs(redirectUri).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void userinfoReturnsTheSubjectOfTheBearerAccessToken() {
        val accessToken = (String) passwordGrant("openid").getBody().get("access_token");

        val userinfo = controller.userinfo("Bearer " + accessToken);

        assertThat(userinfo).isEqualTo(Map.of("sub", "fake-user", "preferred_username", "fake-user"));
    }

    @Test
    void userinfoRejectsRefreshAndIdTokensAsBearer() {
        val body = controller.token(
                "password", CLIENT_ID, null, null, null, "fake-user", "ignored-password", null, "openid profile")
                .getBody();

        for (val token : List.of((String) body.get("refresh_token"), (String) body.get("id_token"))) {
            val exception = catchThrowableOfType(
                    ResponseStatusException.class,
                    () -> controller.userinfo("Bearer " + token));

            assertThat(exception).isNotNull();
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void logoutRedirectsToTheGivenLoopbackUriWithState() {
        val response = controller.logout("http://localhost:3000/", "some-state");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost:3000/?state=some-state");
    }

    @Test
    void logoutWithoutRedirectUriReturnsToTheLastKnownClientOrigin() {
        issueAuthorizationCode();

        val response = controller.logout(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("http://localhost:3000/");
    }

    @Test
    void logoutWithoutRedirectUriAndWithoutAnyPriorLoginRendersAPlainConfirmation() {
        val response = controller.logout(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Logged out");
    }

    private org.springframework.http.ResponseEntity<Map<String, Object>> passwordGrant(final String scope) {
        return controller.token("password", null, null, null, null, "fake-user", "ignored-password", null, scope);
    }

    private String issueAuthorizationCode() {
        val location = controller.authorize(
                        "code", CLIENT_ID, REDIRECT_URI, null, null, pkceChallenge(PKCE_VERIFIER), "S256", "some-user")
                .getHeaders().getFirst(HttpHeaders.LOCATION);
        return location.substring(location.indexOf("code=") + 5);
    }

    private void assertBadRequest(final Runnable tokenRequest) {
        val exception = catchThrowableOfType(ResponseStatusException.class, tokenRequest::run);
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @SneakyThrows
    private static String pkceChallenge(final String verifier) {
        val hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @SneakyThrows
    private String subjectOf(final String token) {
        return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
    }

    @SneakyThrows
    private long expirationMillisOf(final String token) {
        return SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime().getTime();
    }
}
