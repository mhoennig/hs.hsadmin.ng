package net.hostsharing.hsadminng.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static net.hostsharing.hsadminng.config.HttpHeadersBuilder.headers;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"server.port=0", "hsadminng.cas.server=http://localhost:8088/cas"})
@ActiveProfiles("wiremock") // IMPORTANT: To test prod config, do not use test profile!
@Tag("generalIntegrationTest")
class CasAuthenticationFilterIntegrationTest {

    @Value("${local.server.port}")
    private int serverPort;

    @Value("${hsadminng.cas.service}")
    private String serviceUrl;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WireMockServer wireMockServer;

    @Test
    public void shouldAcceptRequest() {
        // given
        final var username = "test-user-" + randomAlphanumeric(4);
        wireMockServer.stubFor(get(urlEqualTo("/cas/p3/serviceValidate?service=" + serviceUrl + "&ticket=valid"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
                                    <cas:authenticationSuccess>
                                        <cas:user>%{username}</cas:user>
                                    </cas:authenticationSuccess>
                                </cas:serviceResponse>
                                """.replace("%{username}", username)
                        )));

        // when
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                new HttpEntity<>(null, headers("Authorization", "valid")),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("pong " + username + "\n");
    }

    @Test
    public void shouldRejectRequest() {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/cas/p3/serviceValidate?service=" + serviceUrl + "&ticket=invalid"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
                                    <cas:authenticationFailure code="INVALID_REQUEST"></cas:authenticationFailure>
                                </cas:serviceResponse>
                                """)));

        // when
        final var result = restTemplate.exchange(
                "http://localhost:" + this.serverPort + "/api/ping",
                HttpMethod.GET,
                new HttpEntity<>(null, headers("Authorization", "invalid")),
                String.class
        );

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
