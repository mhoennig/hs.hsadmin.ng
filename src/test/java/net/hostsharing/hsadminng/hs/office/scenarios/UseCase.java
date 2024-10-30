package net.hostsharing.hsadminng.hs.office.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.restassured.http.ContentType;
import lombok.Getter;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.reflection.AnnotationFinder;
import org.apache.commons.collections4.map.LinkedMap;
import org.hibernate.AssertionFailure;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.net.URLEncoder.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

public abstract class UseCase<T extends UseCase<?>> {

    private static final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected final ScenarioTest testSuite;
    private final TestReport testReport;
    private final Map<String, Function<String, UseCase<?>>> requirements = new LinkedMap<>();
    private final String resultAlias;
    private final Map<String, Object> givenProperties = new LinkedHashMap<>();

    private String nextTitle; // just temporary to override resultAlias for sub-use-cases

    public UseCase(final ScenarioTest testSuite) {
        this(testSuite, getResultAliasFromProducesAnnotationInCallStack());
    }

    public UseCase(final ScenarioTest testSuite, final String resultAlias) {
        this.testSuite = testSuite;
        this.testReport = testSuite.testReport;
        this.resultAlias = resultAlias;
        if (resultAlias != null) {
            testReport.printPara("### UseCase " + title(resultAlias));
        }
    }

    public final void requires(final String alias, final Function<String, UseCase<?>> useCaseFactory) {
        if (!ScenarioTest.containsAlias(alias)) {
            requirements.put(alias, useCaseFactory);
        }
    }

    public final HttpResponse doRun() {
        testReport.printPara("### Given Properties");
        testReport.printLine("""
                | name | value |
                |------|-------|""");
        givenProperties.forEach((key, value) ->
                testReport.printLine("| " + key + " | " + value.toString().replace("\n", "<br>") + " |"));
        testReport.printLine("");
        testReport.silent(() ->
                requirements.forEach((alias, factory) -> {
                    if (!ScenarioTest.containsAlias(alias)) {
                        factory.apply(alias).run().keep();
                    }
                })
        );
        return run();
    }

    protected abstract HttpResponse run();

    public final UseCase<T> given(final String propName, final Object propValue) {
        givenProperties.put(propName, propValue);
        ScenarioTest.putProperty(propName, propValue);
        return this;
    }

    public final JsonTemplate usingJsonBody(final String jsonTemplate) {
        return new JsonTemplate(jsonTemplate);
    }

    public final void obtain(
            final String alias,
            final Supplier<HttpResponse> http,
            final Function<HttpResponse, String> extractor,
            final String... extraInfo) {
        withTitle(ScenarioTest.resolve(alias), () -> {
            http.get().keep(extractor);
            Arrays.stream(extraInfo).forEach(testReport::printPara);
        });
    }

    public final void obtain(final String alias, final Supplier<HttpResponse> http, final String... extraInfo) {
        withTitle(ScenarioTest.resolve(alias), () -> {
            http.get().keep();
            Arrays.stream(extraInfo).forEach(testReport::printPara);
        });
    }

    private void withTitle(final String title, final Runnable code) {
        this.nextTitle = title;
        code.run();
        this.nextTitle = null;
    }

    @SneakyThrows
    public final HttpResponse httpGet(final String uriPath) {
        final var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("current-subject", ScenarioTest.RUN_AS_USER)
                .timeout(Duration.ofSeconds(10))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.GET, uriPath, null, response);
    }

    @SneakyThrows
    public final HttpResponse httpPost(final String uriPath, final JsonTemplate bodyJsonTemplate) {
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("current-subject", ScenarioTest.RUN_AS_USER)
                .timeout(Duration.ofSeconds(10))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.POST, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpPatch(final String uriPath, final JsonTemplate bodyJsonTemplate) {
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .method(HttpMethod.PATCH.toString(), BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("current-subject", ScenarioTest.RUN_AS_USER)
                .timeout(Duration.ofSeconds(10))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.PATCH, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpDelete(final String uriPath) {
        final var request = HttpRequest.newBuilder()
                .DELETE()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("current-subject", ScenarioTest.RUN_AS_USER)
                .timeout(Duration.ofSeconds(10))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.DELETE, uriPath, null, response);
    }

    public final UUID uuid(final String alias) {
        return ScenarioTest.uuid(alias);
    }

    public String uriEncoded(final String text) {
        return encode(ScenarioTest.resolve(text));
    }

    public static class JsonTemplate {

        private final String template;

        private JsonTemplate(final String jsonTemplate) {
            this.template = jsonTemplate;
        }

        String resolvePlaceholders() {
            return ScenarioTest.resolve(template);
        }
    }

    public class HttpResponse {

        @Getter
        private final java.net.http.HttpResponse<String> response;

        @Getter
        private final HttpStatus status;

        private UUID locationUuid;

        @SneakyThrows
        public HttpResponse(
                final HttpMethod httpMethod,
                final String uri,
                final String requestBody,
                final java.net.http.HttpResponse<String> response
        ) {
            this.response = response;
            this.status = HttpStatus.valueOf(response.statusCode());
            if (this.status == HttpStatus.CREATED) {
                final var location = response.headers().firstValue("Location").orElseThrow();
                assertThat(location).startsWith("http://localhost:");
                locationUuid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
            }

            reportRequestAndResponse(httpMethod, uri, requestBody);
        }

        public HttpResponse expecting(final HttpStatus httpStatus) {
            assertThat(HttpStatus.valueOf(response.statusCode())).isEqualTo(httpStatus);
            return this;
        }

        public HttpResponse expecting(final ContentType contentType) {
            assertThat(response.headers().firstValue("content-type"))
                    .contains(contentType.toString());
            return this;
        }

        public void keep(final Function<HttpResponse, String> extractor) {
            final var alias = nextTitle != null ? nextTitle : resultAlias;
            assertThat(alias).as("cannot keep result, no alias found").isNotNull();

            final var value = extractor.apply(this);
            ScenarioTest.putAlias(
                    alias,
                    new ScenarioTest.Alias<>(UseCase.this.getClass(), UUID.fromString(value)));
        }

        public void keep() {
            final var alias = nextTitle != null ? nextTitle : resultAlias;
            assertThat(alias).as("cannot keep result, no alias found").isNotNull();
            ScenarioTest.putAlias(
                    alias,
                    new ScenarioTest.Alias<>(UseCase.this.getClass(), locationUuid));
        }

        @SneakyThrows
        public HttpResponse expectArrayElements(final int expectedElementCount) {
            final var rootNode = objectMapper.readTree(response.body());
            assertThat(rootNode.isArray()).as("array expected, but got: " + response.body()).isTrue();

            final var root = (List<?>) objectMapper.readValue(response.body(), new TypeReference<List<Object>>() {
            });
            assertThat(root.size()).as("unexpected number of array elements").isEqualTo(expectedElementCount);
            return this;
        }

        @SneakyThrows
        public String getFromBody(final String path) {
            return JsonPath.parse(response.body()).read(path);
        }

        @SneakyThrows
        private void reportRequestAndResponse(final HttpMethod httpMethod, final String uri, final String requestBody) {

            // the title
            if (nextTitle != null) {
                testReport.printLine("\n### " + nextTitle + "\n");
            } else if (resultAlias != null) {
                testReport.printLine("\n### " + resultAlias + "\n");
            }

            // the request
            testReport.printLine("```");
            testReport.printLine(httpMethod.name() + " " + uri);
            testReport.printLine((requestBody != null ? requestBody.trim() : ""));

            // the response
            testReport.printLine("=> status: " + status + " " + (locationUuid != null ? locationUuid : ""));
            if (httpMethod == HttpMethod.GET || status.isError()) {
                final var jsonNode = objectMapper.readTree(response.body());
                final var prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                testReport.printLine(prettyJson);
            }
            testReport.printLine("```");
            testReport.printLine("");
        }
    }

    protected T self() {
        //noinspection unchecked
        return (T) this;
    }

    private static @Nullable String getResultAliasFromProducesAnnotationInCallStack() {
        return AnnotationFinder.findCallerAnnotation(Produces.class, Test.class)
                .map(produces -> oneOf(produces.value(), produces.explicitly()))
                .orElse(null);
    }

    private static String oneOf(final String one, final String another) {
        if (isNotBlank(one) && isBlank(another)) {
            return one;
        } else if (isBlank(one) && isNotBlank(another)) {
            return another;
        }
        throw new AssertionFailure("exactly one value required, but got '" + one + "' and '" + another + "'");
    }

    private String title(String resultAlias) {
        return getClass().getSimpleName().replaceAll("([a-z])([A-Z]+)", "$1 $2") + " => " + resultAlias;
    }
}
