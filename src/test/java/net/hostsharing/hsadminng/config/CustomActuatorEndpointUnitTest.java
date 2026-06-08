package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomActuatorEndpointUnitTest {

    private final CustomActuatorEndpoint endpoint = new CustomActuatorEndpoint();
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        val restTemplate = (RestTemplate) ReflectionTestUtils.getField(endpoint, "restTemplate");
        server = MockRestServiceServer.createServer(restTemplate);
        val request = new MockHttpServletRequest("GET", "/actuator/metric-links");
        request.setServerName("example.org");
        request.setServerPort(443);
        request.setScheme("https");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getMetricsLinksReturnsLinksForKnownMetricNames() {
        // given
        server.expect(requestTo("https://example.org/actuator/metrics"))
                .andRespond(withSuccess("""
                        {
                            "names": [
                                "app.requests",
                                "jvm.memory.used"
                            ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        val result = endpoint.getMetricsLinks();

        // then
        assertThat(result).isEqualTo("""
                {
                "app.requests": "https://example.org/actuator/metrics/app.requests",
                "jvm.memory.used": "https://example.org/actuator/metrics/jvm.memory.used"
                }""");
        server.verify();
    }

    @Test
    void getMetricsLinksRejectsMissingMetricsResponse() {
        // given
        server.expect(requestTo("https://example.org/actuator/metrics"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // then
        assertThatThrownBy(endpoint::getMetricsLinks)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no metrics available");
        server.verify();
    }
}
