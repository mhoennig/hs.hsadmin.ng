package net.hostsharing.hsadminng.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"management.port=0", "server.port=0"})
// IMPORTANT: To test prod config, do not use test profile!
class WebSecurityConfigIntegrationTest {

    @Value("${local.server.port}")
    private int serverPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldSupportPingEndpoint() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.serverPort + "/api/ping", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).startsWith("pong");
    }

    @Test
    public void shouldSupportActuatorEndpoint() {
        final var result = this.restTemplate.getForEntity(
                "http://localhost:" + this.managementPort + "/actuator", Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
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
