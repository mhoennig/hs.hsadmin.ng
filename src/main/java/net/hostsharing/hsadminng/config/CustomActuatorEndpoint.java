package net.hostsharing.hsadminng.config;

import lombok.Getter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Component
@Endpoint(id="metric-links")
// BLOG: implement a custom Spring Actuator endpoint to view _clickable_ Spring Actuator (Micrometer) Metrics endpoints
// HOWTO: implement a custom Spring Actuator endpoint
public class CustomActuatorEndpoint {

    private final RestTemplate restTemplate = new RestTemplate();

    @ReadOperation
    public String getMetricsLinks() {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        final var metricsEndpoint = baseUrl + "/actuator/metrics";

        final var response = restTemplate.getForObject(metricsEndpoint, ActuatorMetricsEndpointResource.class);

        if (response == null || response.getNames() == null) {
            throw new IllegalStateException("no metrics available");
        }
        return generateJsonLinksToMetricEndpoints(response, metricsEndpoint);
    }

    private static String generateJsonLinksToMetricEndpoints(final ActuatorMetricsEndpointResource response, final String metricsEndpoint) {
        final var links = response.getNames().stream()
                .map(name -> "\"" + name + "\": \"" + metricsEndpoint + "/" + name + "\"")
                .toList();
        return "{\n" + String.join(",\n", links) + "\n}";
    }

    @Getter
    private static class ActuatorMetricsEndpointResource {
        private List<String> names;
    }
}
