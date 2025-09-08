package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@Profile("fake-jwt")
@NoSecurityRequirement
@Slf4j
public class FakeJwtController {

    @PostMapping(value = "/fake-jwt/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Timed("app.config.jwt.token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(defaultValue = "openid profile") String scope) {

        log.info("Fake JWT: Issuing token for user: {}", username);

        // Accept any username/password for local testing
        String token = JwtFakeBearer.bearer(username).replace("Bearer ", "");

        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", 3600,
                "scope", scope
        ));
    }
}
