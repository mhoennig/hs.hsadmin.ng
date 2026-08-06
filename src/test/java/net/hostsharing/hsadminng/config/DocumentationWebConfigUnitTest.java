package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unitTest")
class DocumentationWebConfigUnitTest {

    private static final String API_DEFINITIONS = "src/main/resources/api-definition";
    private static final String SPEC_URL_PREFIX = "/doc/api/spec/";
    private static final String IMPLEMENTED_API_URL = "/v3/api-docs";

    // The test context replaces application.yml by the one from src/test/resources, so the Swagger
    // UI configuration can only be checked against the production file directly.
    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> configuredSwaggerUiUrls() throws Exception {
        val urls = new ArrayList<Map<String, String>>();
        try (val yaml = new FileInputStream("src/main/resources/application.yml")) {
            for (final Object document : new Yaml().loadAll(yaml)) {
                val springdoc = (Map<String, Object>) ((Map<String, Object>) document).get("springdoc");
                if (springdoc != null) {
                    val swaggerUi = (Map<String, Object>) springdoc.get("swagger-ui");
                    urls.addAll((List<Map<String, String>>) swaggerUi.get("urls"));
                }
            }
        }
        return urls;
    }

    @Test
    void everyConfiguredApiDefinitionExists() throws Exception {
        val specUrls = configuredSwaggerUiUrls().stream()
                .map(url -> url.get("url"))
                .filter(url -> url.startsWith(SPEC_URL_PREFIX))
                .toList();

        assertThat(specUrls).isNotEmpty();
        specUrls.forEach(url ->
                assertThat(Path.of(API_DEFINITIONS, url.substring(SPEC_URL_PREFIX.length())))
                        .as("API definition configured for Swagger UI as %s", url)
                        .exists());
    }

    @Test
    void theTestOnlyApiIsNotOffered() throws Exception {
        assertThat(Path.of(API_DEFINITIONS, "test/test.yaml")).exists();
        assertThat(configuredSwaggerUiUrls()).noneSatisfy(url ->
                assertThat(url.get("url")).contains("test/test.yaml"));
    }

    @Test
    void theImplementedApiIsOfferedLast() throws Exception {
        val urls = configuredSwaggerUiUrls();

        assertThat(urls.getLast().get("url")).isEqualTo(IMPLEMENTED_API_URL);
        assertThat(urls.getLast().get("name")).contains("generated from controllers");
    }
}
