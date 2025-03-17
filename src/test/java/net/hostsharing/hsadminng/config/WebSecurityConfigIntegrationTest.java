package net.hostsharing.hsadminng.config;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"management.port=0", "server.port=0", "hsadminng.cas.server=http://localhost:8088"})
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

    @BeforeEach
    void setUp() {
        wireMockServer.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
                                    <cas:authenticationFailure/>
                                </cas:serviceResponse>
                                """)));
    }

    @Test
    void accessToApiWithValidServiceTicketSouldBePermitted() {
        // given
        givenCasTicketValidationResponse("ST-fake-cas-ticket", "fake-user-name");

        // http request
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                httpHeaders(entry("Authorization", "Bearer ST-fake-cas-ticket")),
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("pong fake-user-name");
    }

    @Test
    void accessToApiWithValidTicketGrantingTicketShouldBePermitted() {
        // given
        givenCasServiceTicketForTicketGrantingTicket("TGT-fake-cas-ticket", "ST-fake-cas-ticket");
        givenCasTicketValidationResponse("ST-fake-cas-ticket", "fake-user-name");

        // http request
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                httpHeaders(entry("Authorization", "Bearer TGT-fake-cas-ticket")),
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("pong fake-user-name");
    }

    @Test
    void accessToApiWithInvalidTicketGrantingTicketShouldBePermitted() {
        // given
        givenCasServiceTicketForTicketGrantingTicket("TGT-fake-cas-ticket", "ST-fake-cas-ticket");
        givenCasTicketValidationResponse("ST-fake-cas-ticket", "fake-user-name");

        // http request
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                httpHeaders(entry("Authorization", "Bearer TGT-WRONG-cas-ticket")),
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessToApiWithoutTokenShouldBeDenied() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.serverPort + "/api/ping", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessToApiWithInvalidTokenShouldBeDenied() {
        // given
        givenCasTicketValidationResponse("ST-fake-cas-ticket", "fake-user-name");

        // when
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                httpHeaders(entry("Authorization", "Bearer ST-WRONG-cas-ticket")),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessToActuatorShouldBePermitted() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToSwaggerUiShouldBePermitted() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.serverPort + "/swagger-ui/index.html", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessToApiDocsEndpointShouldBePermitted() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.serverPort + "/v3/api-docs/swagger-config", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).contains("\"configUrl\":\"/v3/api-docs/swagger-config\"");
    }

    @Test
    void accessToActuatorEndpointShouldBePermitted() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator/health", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().get("status")).isEqualTo("UP");
    }

    private void givenCasServiceTicketForTicketGrantingTicket(final String ticketGrantingTicket, final String serviceTicket) {
        wireMockServer.stubFor(post(urlEqualTo("/cas/v1/tickets/" + ticketGrantingTicket))
                .withFormParam("service", equalTo(serviceUrl))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(serviceTicket)));
    }

    private void givenCasTicketValidationResponse(final String casToken, final String userName) {
        wireMockServer.stubFor(get(urlEqualTo("/cas/p3/serviceValidate?service=" + serviceUrl + "&ticket=" + casToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
                                    <cas:authenticationSuccess>
                                        <cas:user>${userName}</cas:user>
                                    </cas:authenticationSuccess>
                                </cas:serviceResponse>
                                """.replace("${userName}", userName))));
    }

    @SafeVarargs
    private HttpEntity<?> httpHeaders(final Map.Entry<String, String>... headerValues) {
        final var headers = new HttpHeaders();
        for ( Map.Entry<String, String> headerValue: headerValues ) {
            headers.add(headerValue.getKey(), headerValue.getValue());
        }
        return new HttpEntity<>(headers);
    }
}
