package net.hostsharing.hsadminng.config;

import lombok.val;
import net.hostsharing.hsadminng.HsadminNgApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.config.HttpHeadersBuilder.headers;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@TestPropertySource(properties = {
        "server.port=0",
        "logging.level.root=DEBUG"
})
@ActiveProfiles("fake-jwt")
@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterIntegrationTest {

    @Value("${local.server.port}")
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldAcceptRequestWithValidJwt() {
        // given
        val givenSubject = "some-subject";
        val bearer = bearer(givenSubject);

        // when
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/pong",
                HttpMethod.GET,
                new HttpEntity<>(null, headers(entry("Authorization", bearer))),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("ponged " + givenSubject);
    }

    @Test
    public void shouldRejectRequestWithInvalidJwt() {
        // given
        val givenInvalidBearer = "Bearer invalid";

        // when
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/pong",
                HttpMethod.GET,
                new HttpEntity<>(null, headers(entry("Authorization", givenInvalidBearer))),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
