package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies Szenario#274.01: with a real OIDC issuer configured (no fake-jwt profile),
 * the public auth-config endpoint reports that issuer as the authority to log in at. */
@Tag("generalIntegrationTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "management.port=0",
        "server.port=0",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.org/realms/example",
        // an HMAC secret avoids fetching the (nonexistent) issuer's discovery document on startup
        "spring.security.oauth2.resourceserver.jwt.hmac-secret=only-for-testing:aiN2Vah1kuS0airooth9"
})
class AuthConfigIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void authConfigReportsOidcMethodWithTheConfiguredIssuerAsAuthorityWithoutAnyLogin() {
        val result = restTemplate.getForEntity("/api/auth-config", Map.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody())
                .containsEntry("method", "oidc")
                .containsEntry("authority", "https://login.example.org/realms/example");
    }
}
