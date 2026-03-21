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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@TestPropertySource(properties = "hsadminng.cors.allowed-origins=https://allowed.example")
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
        val result = restTemplate.exchange(
                serverUrl("/api/pong"),
                HttpMethod.GET,
                httpHeaders(entry(HttpHeaders.AUTHORIZATION, bearer(GIVEN_FAKE_SUBJECT))),
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("ponged " + GIVEN_FAKE_SUBJECT);
    }

    @Test
    void accessToOpenApiWithoutTokenShouldBePermitted() {
        val result = this.restTemplate.getForEntity(serverUrl("/api/ping"), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToProtectedApiWithInvalidTokenShouldBeDenied() {
        val result = restTemplate.exchange(
                serverUrl("/api/pong"),
                HttpMethod.GET,
                httpHeaders(entry(HttpHeaders.AUTHORIZATION, "Bearer INVALID-JWT")),
                String.class
        );

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
        val result = this.restTemplate.getForEntity(serverUrl("/swagger-ui/index.html"), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToApiDocsEndpointShouldBePermitted() {
        val result = this.restTemplate.getForEntity(serverUrl("/v3/api-docs/swagger-config"), String.class);
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

    @Test
    void preflightToPingAllowsAnyOrigin() {
        val response = corsPreflightRequest("/api/ping", "https://anywhere.example");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("GET");
    }

    @Test
    void actualPingRequestAllowsAnyOrigin() {
        val response = corsGetRequest("/api/ping", "https://anywhere.example", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    }

    @Test
    void preflightToPongAllowsConfiguredOrigin() {
        val response = corsPreflightRequest("/api/pong", "https://allowed.example", "GET");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://allowed.example");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("GET");
    }

    @Test
    void preflightToPongBlocksOtherOrigin() {
        val response = corsPreflightRequest("/api/pong", "https://denied.example", "GET");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void actualPongRequestWithInvalidTokenAndAllowedOriginReturnsUnauthorizedWithCorsHeader() {
        val response = corsGetRequest("/api/pong", "https://allowed.example", "Bearer INVALID-JWT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://allowed.example");
    }

    @Test
    void actualPongRequestWithInvalidTokenAndDeniedOriginIsRejectedByCors() {
        val response = corsGetRequest("/api/pong", "https://denied.example", "Bearer INVALID-JWT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void preflightPostToPongAllowsConfiguredOrigin() {
        val response = corsPreflightRequest("/api/pong", "https://allowed.example", "POST");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://allowed.example");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
    }

    @Test
    void preflightPostToPongBlocksOtherOrigin() {
        val response = corsPreflightRequest("/api/pong", "https://denied.example", "POST");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void actualPongPostWithInvalidTokenAndAllowedOriginReturnsUnauthorizedWithCorsHeader() {
        val response = corsPostRequest("/api/pong", "https://allowed.example", "Bearer INVALID-JWT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://allowed.example");
    }

    @Test
    void actualPongPostWithInvalidTokenAndDeniedOriginIsRejectedByCors() {
        val response = corsPostRequest("/api/pong", "https://denied.example", "Bearer INVALID-JWT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private ResponseEntity<String> corsPreflightRequest(final String path, final String origin) {
        return corsPreflightRequest(path, origin, "GET");
    }

    private ResponseEntity<String> corsPreflightRequest(
            final String path,
            final String origin,
            final String accessControlRequestMethod) {
        return restTemplate.exchange(
                serverUrl(path),
                HttpMethod.OPTIONS,
                httpHeaders(
                        entry(HttpHeaders.ORIGIN, origin),
                        entry(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, accessControlRequestMethod)
                ),
                String.class
        );
    }

    private ResponseEntity<String> corsGetRequest(
            final String path,
            final String origin,
            final String authorization) {
        if (authorization != null) {
            return restTemplate.exchange(
                    serverUrl(path),
                    HttpMethod.GET,
                    httpHeaders(
                            entry(HttpHeaders.ORIGIN, origin),
                            entry(HttpHeaders.AUTHORIZATION, authorization),
                            entry(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
                    ),
                    String.class
            );
        }

        return restTemplate.exchange(
                serverUrl(path),
                HttpMethod.GET,
                httpHeaders(
                        entry(HttpHeaders.ORIGIN, origin),
                        entry(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
                ),
                String.class
        );
    }

    private ResponseEntity<String> corsPostRequest(
            final String path,
            final String origin,
            final String authorization) {
        return restTemplate.exchange(
                serverUrl(path),
                HttpMethod.POST,
                httpHeaders(
                        entry(HttpHeaders.ORIGIN, origin),
                        entry(HttpHeaders.AUTHORIZATION, authorization),
                        entry(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
                ),
                String.class
        );
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
