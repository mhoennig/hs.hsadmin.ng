package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.ACCESS_TOKEN_EXPIRES_IN_SECONDS;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.REFRESH_TOKEN_EXPIRES_IN_SECONDS;

@RestController
@Profile("fake-jwt")
@NoSecurityRequirement
@Slf4j
public class FakeJwtController {

    @PostMapping(value = "/fake-jwt/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Timed("app.config.jwt.token")
    public ResponseEntity<Map<String, Object>> token(
            HttpServletRequest request,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "grant_type", defaultValue = "password") String grantType,
            @RequestParam(name = "refresh_token", required = false) String refreshToken,
            @RequestParam(defaultValue = "openid profile") String scope) {

        if ("refresh_token".equals(grantType)) {
            return tokenResponse(subjectFromRefreshToken(refreshToken), scope);
        }
        if (!"password".equals(grantType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported grant_type");
        }

        log.info("Fake JWT: Issuing token for user: {}", username);

        // Accept any username/password for local testing
        return tokenResponse(username, scope);
    }

    private ResponseEntity<Map<String, Object>> tokenResponse(final String subject, final String scope) {
        // the fake token endpoint has no group source, so it issues tokens without group claims
        final var accessToken = JwtFakeBearer.accessToken(subject, List.of());
        final var refreshToken = JwtFakeBearer.refreshToken(subject);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", ACCESS_TOKEN_EXPIRES_IN_SECONDS,
                "refresh_expires_in", REFRESH_TOKEN_EXPIRES_IN_SECONDS,
                "scope", scope
        ));
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
}
