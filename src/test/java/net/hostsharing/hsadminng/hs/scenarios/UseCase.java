package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.restassured.http.ContentType;
import lombok.Getter;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.reflection.AnnotationFinder;
import org.apache.commons.collections4.map.LinkedMap;
import org.assertj.core.api.AbstractStringAssert;
import org.hibernate.AssertionFailure;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.net.URLEncoder.encode;
import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.KEEP_COMMENTS;
import static net.hostsharing.hsadminng.test.DebuggerDetection.isDebuggerAttached;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

public abstract class UseCase<T extends UseCase<?>> {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final int HTTP_TIMEOUT_SECONDS = 20; // FIXME: configurable in environment
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected final ScenarioTest testSuite;
    private final TestReport testReport;
    private final Map<String, Function<String, UseCase<?>>> requirements = new LinkedMap<>();
    private final String resultAlias;
    private final Map<String, Object> givenProperties = new LinkedHashMap<>();

    private String nextTitle; // just temporary to override resultAlias for sub-use-cases
    private String introduction;

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
        requirements.put(alias, useCaseFactory);
    }

    public final HttpResponse doRun() {
        if (introduction != null) {
            testReport.printPara(introduction);
        }
        testReport.printPara("### Given Properties");
        testReport.printLine("""
                | name | value |
                |------|-------|""");
        givenProperties.forEach((key, value) ->
                testReport.printLine("| " + key + " | " + value.toString().replace("\n", "<br>") + " |"));
        testReport.printLine("");
        testReport.silent(() ->
                requirements.forEach((alias, factory) -> {
                    final var resolvedAlias = ScenarioTest.resolve(alias, DROP_COMMENTS);
                    if (!ScenarioTest.containsAlias(resolvedAlias)) {
                        factory.apply(resolvedAlias).run().keepAs(resolvedAlias);
                    }
                })
        );
        final var response = run();
        verify(response);
        keepInProduceAlias(response);

        resetProperties();

        return response;
    }

    protected abstract HttpResponse run();

    protected void verify(final HttpResponse response) {
    }

    public UseCase<T> introduction(final String introduction) {
        this.introduction = introduction;
        return this;
    }

    public final UseCase<T> given(final String propName, final Object propValue) {
        givenProperties.put(propName, ScenarioTest.resolve(propValue == null ? null : propValue.toString(), TemplateResolver.Resolver.KEEP_COMMENTS));
        ScenarioTest.putProperty(propName, propValue);
        return this;
    }

    public final JsonTemplate usingJsonBody(final String jsonTemplate) {
        return new JsonTemplate(jsonTemplate);
    }

    public final HttpResponse obtain(
            final String title,
            final Supplier<HttpResponse> http,
            final Function<HttpResponse, String> extractor,
            final String... extraInfo) {
        return withTitle(title, () -> {
            final var response = http.get().keep(extractor);
            response.optionallyReportRequestAndResponse();
            Arrays.stream(extraInfo).forEach(testReport::printPara);
            return response;
        });
    }

    public final HttpResponse obtain(final String alias, final Supplier<HttpResponse> httpCall, final String... extraInfo) {
        return withTitle(alias, () -> {
            final var response = httpCall.get().keep();
            response.optionallyReportRequestAndResponse();
            Arrays.stream(extraInfo).forEach(testReport::printPara);
            return response;
        });
    }

    public HttpResponse withTitle(final String resolvableTitle, final Supplier<HttpResponse> httpCall, final String... extraInfo) {
        this.nextTitle = resolvableTitle;
        final var response = httpCall.get();
        response.optionallyReportRequestAndResponse();
        Arrays.stream(extraInfo).forEach(testReport::printPara);
        this.nextTitle = null;
        return response;
    }

    @SneakyThrows
    public final HttpResponse httpGet(final String uriPathWithPlaceholders) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Authorization", "Bearer " + ScenarioTest.RUN_AS_USER)
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.GET, uriPath, null, response);
    }

    @SneakyThrows
    public final HttpResponse httpPost(final String uriPathWithPlaceholders, final JsonTemplate bodyJsonTemplate) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ScenarioTest.RUN_AS_USER)
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.POST, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpPatch(final String uriPathWithPlaceholders, final JsonTemplate bodyJsonTemplate) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .method(HttpMethod.PATCH.toString(), BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ScenarioTest.RUN_AS_USER)
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.PATCH, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpDelete(final String uriPathWithPlaceholders) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var request = HttpRequest.newBuilder()
                .DELETE()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ScenarioTest.RUN_AS_USER)
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.DELETE, uriPath, null, response);
    }

    protected PathAssertion path(final String path) {
        return new PathAssertion(path);
    }

    @SafeVarargs
    protected final void verify(
            final String title,
            final Supplier<UseCase.HttpResponse> http,
            final Consumer<UseCase.HttpResponse>... assertions) {
        withTitle(title, () -> {
            final var response = http.get();
            Arrays.stream(assertions).forEach(assertion ->  assertion.accept(response));
            return response;
        });
    }

    public final UUID uuid(final String alias) {
        return ScenarioTest.uuid(alias);
    }

    public String uriEncoded(final String text) {
        return encode(ScenarioTest.resolve(text, DROP_COMMENTS), StandardCharsets.UTF_8);
    }

    public static class JsonTemplate {

        private final String template;

        private JsonTemplate(final String jsonTemplate) {
            this.template = jsonTemplate;
        }

        String resolvePlaceholders() {
            return ScenarioTest.resolve(template, DROP_COMMENTS);
        }

    }

    private void keepInProduceAlias(final HttpResponse response) {
        final var producedAlias = testSuite.takeProducedAlias();
        if (response != null) {
            producedAlias.ifPresent(response::keepAs);
        }
    }

    private static Duration seconds(final int secondsIfNoDebuggerAttached) {
        return isDebuggerAttached() ? Duration.ofHours(1) : Duration.ofSeconds(secondsIfNoDebuggerAttached);
    }

    private void resetProperties() {
        givenProperties.forEach((propName, val) -> ScenarioTest.removeProperty(propName));
    }

    public final class HttpResponse {

        private final HttpMethod httpMethod;
        private final String uri;
        private final String requestBody;

        @Getter
        private final java.net.http.HttpResponse<String> response;

        @Getter
        private final HttpStatus status;

        @Getter
        private UUID locationUuid;

        private boolean reportGenerated = false;
        private boolean reportGeneratedWithResponse = false;

        @SneakyThrows
        public HttpResponse(
                final HttpMethod httpMethod,
                final String uri,
                final String requestBody,
                final java.net.http.HttpResponse<String> response
        ) {
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.requestBody = requestBody;
            this.response = response;
            this.status = HttpStatus.valueOf(response.statusCode());
            if (this.status == HttpStatus.CREATED) {
                final var location = response.headers().firstValue("Location").orElseThrow();
                assertThat(location).startsWith("http://localhost:");
                locationUuid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
            }
        }

        public HttpResponse expecting(final HttpStatus httpStatus) {
            optionallyReportRequestAndResponse();
            assertThat(HttpStatus.valueOf(response.statusCode())).isEqualTo(httpStatus);
            return this;
        }

        public HttpResponse expecting(final ContentType contentType) {
            optionallyReportRequestAndResponse();
            assertThat(response.headers().firstValue("content-type"))
                    .contains(contentType.toString());
            return this;
        }

        public HttpResponse keep(final Function<HttpResponse, String> extractor) {
            optionallyReportRequestAndResponse();

            final var alias = nextTitle != null ? ScenarioTest.resolve(nextTitle, DROP_COMMENTS) : resultAlias;
            assertThat(alias).as("cannot keep result, no alias found").isNotNull();

            final var value = extractor.apply(this);
            ScenarioTest.putAlias(alias, UUID.fromString(value));
            return this;
        }

        public HttpResponse keepAs(final String alias) {
            optionallyReportRequestAndResponse();

            ScenarioTest.putAlias(nonNullAlias(alias), locationUuid);
            return this;
        }

        public HttpResponse keep() {
            optionallyReportRequestAndResponse();

            final var alias = nextTitle != null ? ScenarioTest.resolve(nextTitle, DROP_COMMENTS) : resultAlias;
            assertThat(alias).as("cannot keep result, no title or alias found for locationUuid: " + locationUuid).isNotNull();

            return keepAs(alias);
        }

        @SneakyThrows
        public HttpResponse expectArrayElements(final int expectedElementCount) {
            optionallyReportRequestAndResponse();

            final var rootNode = objectMapper.readTree(response.body());
            assertThat(rootNode.isArray()).as("array expected, but got: " + response.body()).isTrue();

            final var root = (List<?>) objectMapper.readValue(response.body(), new TypeReference<List<Object>>() {
            });
            assertThat(root.size()).as("unexpected number of array elements").isEqualTo(expectedElementCount);
            return this;
        }

        @SneakyThrows
        public HttpResponse expectObject() {
            optionallyReportRequestAndResponse();

            final var rootNode = objectMapper.readTree(response.body());
            assertThat(rootNode.isArray()).as("object expected, but got array: " + response.body()).isFalse();
            return this;
        }

        @SneakyThrows
        public <V> V getFromBody(final String path) {
            final var body = response.body();
            final var resolvedPath = ScenarioTest.resolve(path, DROP_COMMENTS);
            return JsonPath.parse(body).read(resolvedPath);
        }

        @NotNull
        @SneakyThrows
        public <V> JsonOptional<V> getFromBodyAsOptional(final String path) {
            try {
                return JsonOptional.ofValue(getFromBody(path));
            } catch (final PathNotFoundException e) {
                return JsonOptional.notGiven();
            }
        }

        @SneakyThrows
        public AbstractStringAssert<?> path(final String path) {
            return assertThat(getFromBodyAsOptional(path).givenAsString());
        }

        public HttpResponse reportWithResponse() {
           return reportRequestAndResponse(true);
        }

        @SneakyThrows
        private HttpResponse reportRequestAndResponse(final boolean unconditionallyWithResponse) {
            if (reportGenerated) {
                throw new IllegalStateException("request report already generated");
            }

            // the title
            if (nextTitle != null) {
                testReport.printPara("### " + ScenarioTest.resolve(nextTitle, KEEP_COMMENTS));
            } else if (resultAlias != null) {
                testReport.printPara("### Create " + resultAlias);
            } else if (testReport.isSilent()) {
                testReport.printPara("### Untitled Section");
            } else {
                fail("please wrap the http...-call in the UseCase using `withTitle(...)`");
            }

            // the request
            testReport.printLine("```");
            testReport.printLine(httpMethod.name() + " " + uri);
            testReport.printJson(requestBody);

            // the response
            testReport.printLine("=> status: " + status + " " + (locationUuid != null ? locationUuid : ""));
            if (unconditionallyWithResponse || httpMethod == HttpMethod.GET || status.isError()) {
                testReport.printJson(response.body());
                this.reportGeneratedWithResponse = true;
            }
            testReport.printLine("```");
            testReport.printLine("");
            this.reportGenerated = true;
            return this;
        }

        @SneakyThrows
        private void optionallyReportRequestAndResponse() {
            if (!reportGenerated) {
                reportRequestAndResponse(false);
            }
        }

        private void verifyResponseReported(final String action) {
            if (!reportGenerated) {
                throw new IllegalStateException("report not generated yet, but expected for `" + action + "`");
            }
            if (!reportGeneratedWithResponse) {
                throw new IllegalStateException("report without response, but response report required for `" + action + "`");
            }
        }

        private String nonNullAlias(final String alias) {
            // This marker tag should not appear in the source-code, as here is nothing to fix.
            // But if it appears in generated Markdown files, it should show up when that marker tag is searched.
            final var onlyVisibleInGeneratedMarkdownNotInSource = new String(new char[]{'F', 'I', 'X', 'M', 'E'});
            return alias == null ? "unknown alias -- " + onlyVisibleInGeneratedMarkdownNotInSource : alias;
        }

        public HttpResponse extractUuidAlias(final String jsonPath, final String resolvableName) {
            verifyResponseReported("extractUuidAlias");

            final var resolvedName = ScenarioTest.resolve(resolvableName, DROP_COMMENTS);
            final var resolvedJsonPath = getFromBodyAsOptional(jsonPath).givenUUID();
            ScenarioTest.putAlias(resolvedName, resolvedJsonPath);
            return this;
        }

        public HttpResponse extractValue(final String jsonPath, final String resolvableName) {
            verifyResponseReported("extractValue");

            final var resolvedName = ScenarioTest.resolve(resolvableName, DROP_COMMENTS);
            final var resolvedJsonPath = getFromBodyAsOptional(jsonPath).givenAsString();
            ScenarioTest.putProperty(resolvedName, resolvedJsonPath);
            return this;
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

    @Override
    public String toString() {
        final var properties = givenProperties.entrySet().stream()
                .map(e -> "\t" + e.getKey() + "=" + e.getValue())
                .collect(joining("\n"));
        return getClass().getSimpleName() + "(\n\t" + properties + "\n)";
    }
}
