package net.hostsharing.hsadminng.config;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("generalIntegrationTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "management.port=0", "server.port=0" })
@ActiveProfiles("fake-jwt") // IMPORTANT: In this test, want to test the prod config, do NOT use test profile!
class WebSecurityConfigIntegrationTest {

    public static final String GIVEN_FAKE_SUBJECT = "fake-user-name";
    @Value("${local.server.port}")
    private int serverPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void accessToApiWithValidJwtShouldBePermitted() {
        // when
        val result = restTemplate.exchange(
                serverUrl("/api/pong"),
                HttpMethod.GET,
                httpHeaders(entry("Authorization", bearer(GIVEN_FAKE_SUBJECT))),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("ponged " + GIVEN_FAKE_SUBJECT);
    }

    @Test
    void accessToOpenApiWithoutTokenShouldBePermitted() {
        val result = this.restTemplate.getForEntity(
                serverUrl("/api/ping"), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToProtectedApiWithInvalidTokenShouldBeDenied() {
        // when
        val result = restTemplate.exchange(
                serverUrl("/api/pong"),
                HttpMethod.GET,
                httpHeaders(entry("Authorization", "Bearer INVALID-JWT")),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessToActuatorShouldBePermitted() {
        val result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToSwaggerUiShouldBePermitted() {
        val result = this.restTemplate.getForEntity(
                serverUrl("/swagger-ui/index.html"), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToApiDocsEndpointShouldBePermitted() {
        val result = this.restTemplate.getForEntity(
                serverUrl("/v3/api-docs/swagger-config"), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).contains("\"configUrl\":\"/v3/api-docs/swagger-config\"");
    }

    @Test
    void accessToActuatorEndpointShouldBePermitted() {
        val result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/health", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().get("status")).isEqualTo("UP");
    }

    private @NotNull String serverUrl(final String path) {
        return "http://localhost:" + this.serverPort + path;
    }

    @SafeVarargs
    private HttpEntity<?> httpHeaders(final Map.Entry<String, String>... headerValues) {
        val headers = new HttpHeaders();
        for (Map.Entry<String, String> headerValue : headerValues) {
            headers.add(headerValue.getKey(), headerValue.getValue());
        }
        return new HttpEntity<>(headers);
    }
}
