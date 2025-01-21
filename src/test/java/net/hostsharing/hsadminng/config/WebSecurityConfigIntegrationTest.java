package net.hostsharing.hsadminng.config;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"management.port=0", "server.port=0", "hsadminng.cas.server=http://localhost:8088/cas"})
@ActiveProfiles("wiremock") // IMPORTANT: To test prod config, do not use test profile!
@Tag("generalIntegrationTest")
class WebSecurityConfigIntegrationTest {

    @Value("${local.server.port}")
    private int serverPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Value("${hsadminng.cas.service}")
    private String serviceUrl;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WireMockServer wireMockServer;

    @Test
    public void shouldSupportPingEndpoint() {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/cas/p3/serviceValidate?service=" + serviceUrl + "&ticket=test-user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
                                    <cas:authenticationSuccess>
                                        <cas:user>test-user</cas:user>
                                    </cas:authenticationSuccess>
                                </cas:serviceResponse>
                                """)));


        // fake Authorization header
        final var headers = new HttpHeaders();
        headers.set("Authorization", "test-user");

        // http request
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("pong test-user");
    }

    @Test
    public void shouldSupportActuatorEndpoint() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldSupportSwaggerUi() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/swagger-ui/index.html", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldSupportApiDocs() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/v3/api-docs/swagger-config", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND); // permitted but not configured
    }

    @Test
    public void shouldSupportHealthEndpoint() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/health", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    public void shouldSupportMetricsEndpoint() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/metrics", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
