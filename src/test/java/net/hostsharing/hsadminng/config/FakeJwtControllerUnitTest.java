package net.hostsharing.hsadminng.config;

import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.REFRESH_TOKEN_EXPIRES_IN_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class FakeJwtControllerUnitTest {

    private final FakeJwtController controller = new FakeJwtController();

    @Test
    void passwordGrantReturnsAccessAndRefreshToken() {
        val response = controller.token(
                null,
                "fake-user",
                "ignored-password",
                "password",
                null,
                "openid profile");

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
        val response = controller.token(null, "fake-user", "ignored-password", "password", null, "openid");

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
        val passwordGrantResponse = controller.token(
                null, "fake-user", "ignored-password", "password", null, "openid");
        val passwordGrantBody = passwordGrantResponse.getBody();
        val refreshToken = (String) passwordGrantBody.get("refresh_token");

        val refreshGrantResponse = controller.token(
                null, null, null, "refresh_token", refreshToken, "openid");

        val refreshGrantBody = refreshGrantResponse.getBody();
        assertThat(refreshGrantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshGrantBody.get("access_token")).isNotEqualTo(passwordGrantBody.get("access_token"));
        assertThat(subjectOf((String) refreshGrantBody.get("access_token"))).isEqualTo("fake-user");
        assertThat(JwtFakeBearer.subjectFromRefreshToken((String) refreshGrantBody.get("refresh_token"))).isEqualTo("fake-user");
    }

    @Test
    void refreshGrantRequiresRefreshTokenParameter() {
        val passwordGrantResponse = controller.token(
                null, "fake-user", "ignored-password", "password", null, "openid");
        val refreshToken = (String) passwordGrantResponse.getBody().get("refresh_token");

        val exception = catchThrowableOfType(
                () -> controller.token(null, null, null, "refresh_token", null, "openid"),
                ResponseStatusException.class);

        assertThat(refreshToken).isNotBlank();
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshGrantRejectsInvalidRefreshToken() {
        val exception = catchThrowableOfType(
                () -> controller.token(null, null, null, "refresh_token", "not-a-refresh-token", "openid"),
                ResponseStatusException.class);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
