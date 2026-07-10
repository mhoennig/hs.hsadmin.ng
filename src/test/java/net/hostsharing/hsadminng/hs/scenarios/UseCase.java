package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.hs.scenarios.MarkdownTableCellRenderer.toMarkdownTableCell;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.encodeQueryParameterValue;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.KEEP_COMMENTS;
import static net.hostsharing.hsadminng.test.DebuggerDetection.isDebuggerAttached;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

public abstract class UseCase<T extends UseCase<?>> {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper REPORT_OBJECT_MAPPER = new ObjectMapper();
    private static final String AUTH_HEADER_KEY = "Authorization";
    private static final String FAKE_AUTH_HEADER_KEY = "X-Fake-Authorization";
    private static final String ASSUMED_ROLES_HEADER_KEY = "Hostsharing-Assumed-Roles";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String HTTP_TIMEOUT_SECONDS_ENV_VAR = "HSADMINNG_SCENARIO_HTTP_TIMEOUT_SECONDS";
    public static final int HTTP_TIMEOUT_SECONDS_DEFAULT = 20;
    private static final int HTTP_TIMEOUT_SECONDS = httpTimeoutSeconds(HTTP_TIMEOUT_SECONDS_DEFAULT);
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final ScenarioTest testSuite;
    private final TestReport testReport;
    private final Map<String, Function<String, UseCase<?>>> requirements = new LinkedMap<>();
    private final String resultAlias;
    private final Map<String, Object> givenProperties = new LinkedHashMap<>();
    private final Map<String, Object> usingProperties = new LinkedHashMap<>();
    private final Map<String, Object> expectedProperties = new LinkedHashMap<>();

    private String nextTitle; // just temporary to override resultAlias for sub-use-cases
    private String nextRequestInfo;
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

    public final HttpResponse thenExpect(final HttpStatus expectedStatus) {
        if (introduction != null) {
            testReport.printPara(introduction);
        }
        testReport.printPara("### Properties");
        testReport.printRequiredProducerLinks();
        renderProperties("Given", givenProperties);
        renderProperties("Expected", expectedProperties);

        testReport.silent(() ->
                requirements.forEach((alias, factory) -> {
                    final var resolvedAlias = ScenarioTest.resolve(alias, DROP_COMMENTS);
                    if (!ScenarioTest.containsAlias(resolvedAlias)) {
                        factory.apply(resolvedAlias).run().keepAs(resolvedAlias);
                    }
                })
        );
        final var response = run(expectedStatus);
        assertThat(response).as("use case implementation must return main response, never null").isNotNull();
        if (!response.status.isError()) {
            verify(response);
        }
        keepInProduceAlias(response);

        resetProperties();

        return response;
    }

    private void renderProperties(final String title, final Map<String, Object> properties) {
        if (properties.isEmpty()) {
            return;
        }
        testReport.printLine("");
        testReport.printLine("#### " + title);
        testReport.printLine("");
        testReport.printLine("""
                | name | value |
                |------|-------|""");
        properties.forEach((key, value) ->
                testReport.printLine("| " + key + " | " + toMarkdownTableCell(value) + " |"));
        testReport.printLine("");
    }

    // this method is called by the test framework, override, but do not call from subclass
    protected HttpResponse run(final HttpStatus expectedStatus) {
        assertThat(expectedStatus).as("legacy signature only defined for HttpStatus.OK").isEqualTo(HttpStatus.OK);
        return run();
    }

    // legacy signature for backwards compatibility, only called by above method
    protected HttpResponse run() {return null;}

    protected void verify(final HttpResponse response) {
    }

    public UseCase<T> introduction(final String introduction) {
        this.introduction = introduction;
        return this;
    }

    public final UseCase<T> given(final String propName, final Object propValue) {
        return keepProperty(givenProperties, propName, propValue);
    }

    // To keep things simple, given, using and expected properties are available everywhere in all templates.
    // The distinction is mostly for readability.

    /** Similar to given, but just required to create for intermediate entities.
     *  Other than given properties, these do are NOT listed in the Given/Expected section at the top of the report.
     *  If it's necessary, it's often a sign that the UseCase could be split into two separate UseCases
     *  connected via @Produces+@Requires.
     */
    public final UseCase<T> using(final String propName, final Object propValue) {
        return keepProperty(usingProperties, propName, propValue);
    }

    /** Similar to given, but used for assertions in the verification step after the actual use-case.
     *  But all properties are available everywhere in templates.
     *  It would be a bit tricky to make the expected values available just for validations.
     */
    public final UseCase<T> expected(final String propName, final Object propValue) {
        return keepProperty(expectedProperties, propName, propValue);
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
        return withTitleAndRequestInfo(resolvableTitle, null, httpCall, extraInfo);
    }

    public HttpResponse withTitleAndRequestInfo(
            final String resolvableTitle,
            final String requestInfo,
            final Supplier<HttpResponse> httpCall,
            final String... extraInfo) {
        this.nextTitle = resolvableTitle;
        this.nextRequestInfo = requestInfo;
        final var response = httpCall.get();
        response.optionallyReportRequestAndResponse();
        Arrays.stream(extraInfo).forEach(testReport::printPara);
        this.nextTitle = null;
        this.nextRequestInfo = null;
        return response;
    }

    @SneakyThrows
    public final HttpResponse httpGet(
            final FakeLoginUser loginUser,
            final String uriPathWithPlaceholder,
            final Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomizer) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholder, DROP_COMMENTS);
        final var requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Authorization", loginUser.bearer())
                .header("X-Fake-Authorization", loginUser.reportableBearer());
        final var customizedRequestBuilder = requestCustomizer.apply(requestBuilder);
        final var request = customizedRequestBuilder.build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.GET, uriPath, null, response);
    }

    public final HttpResponse httpGet(
            final FakeLoginUser loginUser,
            final String uriPathWithPlaceholder) {
        return httpGet(loginUser, uriPathWithPlaceholder, requestBuilder -> requestBuilder);
    }

    @SneakyThrows
    public final HttpResponse httpPost(
            final FakeLoginUser loginUser, final String uriPathWithPlaceholders,
            final JsonTemplate bodyJsonTemplate) {
        return httpPost(loginUser, uriPathWithPlaceholders, bodyJsonTemplate, requestBuilder -> requestBuilder);
    }

    @SneakyThrows
    public final HttpResponse httpPost(
            final FakeLoginUser loginUser, final String uriPathWithPlaceholders,
            final JsonTemplate bodyJsonTemplate,
            final Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomizer) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var requestBuilder = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", loginUser.bearer())
                .header("X-Fake-Authorization", loginUser.reportableBearer())
                .timeout(seconds(HTTP_TIMEOUT_SECONDS));
        final var request = requestCustomizer.apply(requestBuilder).build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.POST, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpPut(
            final FakeLoginUser loginUser, final String uriPathWithPlaceholders,
            final JsonTemplate bodyJsonTemplate
    ) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .PUT(BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", loginUser.bearer())
                .header("X-Fake-Authorization", loginUser.reportableBearer())
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.PUT, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpPatch(
            final FakeLoginUser loginUser, final String uriPathWithPlaceholders,
            final JsonTemplate bodyJsonTemplate
    ) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var requestBody = bodyJsonTemplate.resolvePlaceholders();
        final var request = HttpRequest.newBuilder()
                .method(HttpMethod.PATCH.toString(), BodyPublishers.ofString(requestBody))
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", loginUser.bearer())
                .header("X-Fake-Authorization", loginUser.reportableBearer())
                .timeout(seconds(HTTP_TIMEOUT_SECONDS))
                .build();
        final var response = client.send(request, BodyHandlers.ofString());
        return new HttpResponse(HttpMethod.PATCH, uriPath, requestBody, response);
    }

    @SneakyThrows
    public final HttpResponse httpDelete(final FakeLoginUser loginUser, final String uriPathWithPlaceholders) {
        final var uriPath = ScenarioTest.resolve(uriPathWithPlaceholders, DROP_COMMENTS);
        final var request = HttpRequest.newBuilder()
                .DELETE()
                .uri(new URI("http://localhost:" + testSuite.port + uriPath))
                .header("Content-Type", "application/json")
                .header("Authorization", loginUser.bearer())
                .header("X-Fake-Authorization", loginUser.reportableBearer())
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
        return encodeQueryParameterValue(ScenarioTest.resolve(text, DROP_COMMENTS));
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

    private UseCase<T> keepProperty(final Map<String, Object> usingProperties, final String propName, final Object propValue) {
        usingProperties.put(
                propName,
                ScenarioTest.resolve(propValue == null ? null : propValue.toString(), TemplateResolver.Resolver.KEEP_COMMENTS));
        ScenarioTest.putProperty(propName, propValue);
        return this;
    }

    private static Duration seconds(final int secondsIfNoDebuggerAttached) {
        return isDebuggerAttached() ? Duration.ofHours(1) : Duration.ofSeconds(secondsIfNoDebuggerAttached);
    }

    private static int httpTimeoutSeconds(final int defaultSeconds) {
        final var configuredTimeout = System.getenv(HTTP_TIMEOUT_SECONDS_ENV_VAR);
        if (isBlank(configuredTimeout)) {
            return defaultSeconds;
        }
        try {
            final var timeoutSeconds = Integer.parseInt(configuredTimeout.trim());
            if (timeoutSeconds > 0) {
                return timeoutSeconds;
            }
        } catch (final NumberFormatException exc) {
            throw new IllegalArgumentException(HTTP_TIMEOUT_SECONDS_ENV_VAR + " must be a positive integer", exc);
        }
        throw new IllegalArgumentException(HTTP_TIMEOUT_SECONDS_ENV_VAR + " must be a positive integer");
    }

    // Scenario reports hide the real JWT and show the readable fake-auth header as Authorization.
    static String reportableRequestHeaderName(final String headerName) {
        return FAKE_AUTH_HEADER_KEY.equalsIgnoreCase(headerName) ? AUTH_HEADER_KEY : headerName;
    }

    static List<Map.Entry<String, List<String>>> reportableRequestHeaders(
            final Map<String, List<String>> requestHeaders,
            final boolean requestHasBody) {
        return requestHeaders.entrySet().stream()
                // the Authorization header with the long Bearer token is of no value here
                .filter(entry -> !AUTH_HEADER_KEY.equalsIgnoreCase(entry.getKey()))
                // instead, use the X-Fake-Authorization header as if it was the real Authorization header
                .map(entry -> Map.entry(reportableRequestHeaderName(entry.getKey()), entry.getValue()))
                .sorted((left, right) -> {
                    final var rankComparison = Integer.compare(
                            reportableRequestHeaderRank(left.getKey(), requestHasBody),
                            reportableRequestHeaderRank(right.getKey(), requestHasBody));
                    return rankComparison != 0
                            ? rankComparison
                            : String.CASE_INSENSITIVE_ORDER.compare(left.getKey(), right.getKey());
                })
                .toList();
    }

    private static int reportableRequestHeaderRank(final String headerName, final boolean requestHasBody) {
        if (AUTH_HEADER_KEY.equalsIgnoreCase(headerName)) {
            return 0;
        }
        if (ASSUMED_ROLES_HEADER_KEY.equalsIgnoreCase(headerName)) {
            return 1;
        }
        if (requestHasBody && CONTENT_TYPE_HEADER_KEY.equalsIgnoreCase(headerName)) {
            return 3;
        }
        return 2;
    }

    // an unquoted here-document delimiter, thus values can be replaced by shell variables after copy+paste
    private static final String HERE_DOCUMENT_DELIMITER = "EOF";

    // not defined anywhere in this repo,
    // just a placehoder to make reported requests copy+paste-able into a shell
    // where the user has an HTTP alias and HSADMINNG_API_BASE_URL defined
    private static final String HSADMINNG_API_BASE_URL_ENV_VAR = "$HSADMINNG_API_BASE_URL";

    // also not defined in this repo; the fake JWT claims are rendered as ignorable `# ...` pseudo-arguments,
    // thus the user can paste the command with a real HSADMINNG_JWT_BEARER defined in their environment
    private static final String HSADMINNG_JWT_BEARER_ENV_VAR = "$HSADMINNG_JWT_BEARER";

    private static final String BEARER_JWT_PREFIX = "Bearer JWT ";

    @SneakyThrows
    static String requestBodyArgument(final String requestBody) {
        final var json = REPORT_OBJECT_MAPPER.readTree(requestBody);
        final var prettyJson = REPORT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        // --data-binary (not -d) keeps the newlines, thus trailing `// alias` comments cannot swallow the rest of the body
        return "--data-binary @- <<" + HERE_DOCUMENT_DELIMITER + "\n"
                + escapedForHereDocument(prettyJson) + "\n"
                + HERE_DOCUMENT_DELIMITER;
    }

    static String escapedForHereDocument(final String text) {
        return text.replace("\\", "\\\\").replace("$", "\\$").replace("`", "\\`");
    }

    static String reportableRequestHeaderValue(final String headerName, final String headerValue) {
        if (AUTH_HEADER_KEY.equalsIgnoreCase(headerName) && headerValue.startsWith(BEARER_JWT_PREFIX)) {
            // the fake JWT claims are rendered as `# ...` pseudo-arguments right after this header
            return "Bearer " + HSADMINNG_JWT_BEARER_ENV_VAR;
        }
        return headerValue;
    }

    /** @return the fake JWT claims as one {@code `# ...`} pseudo-argument per line, or empty for real JWTs;
     *          each is a command substitution just containing a shell comment and thus expands to nothing,
     *          therefore the shell drops these arguments when the pasted command gets executed */
    @SneakyThrows
    static List<String> jwtClaimsCommentArguments(final String authHeaderValue) {
        if (!authHeaderValue.startsWith(BEARER_JWT_PREFIX)) {
            return List.of();
        }
        final var jwtClaims = REPORT_OBJECT_MAPPER.readTree(authHeaderValue.substring(BEARER_JWT_PREFIX.length()));
        final var prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        final var prettyJson = REPORT_OBJECT_MAPPER.writer(prettyPrinter).writeValueAsString(jwtClaims);
        return escapedForBackticks(prettyJson).lines()
                .map(line -> "`# " + line + "`")
                .toList();
    }

    // within backticks, `\` and closing backticks need to be escaped to not terminate the substitution early
    static String escapedForBackticks(final String text) {
        return text.replace("\\", "\\\\").replace("`", "\\`");
    }

    private void resetProperties() {
        Stream.of(givenProperties, usingProperties, expectedProperties)
                .flatMap(properties -> properties.keySet().stream())
                .forEach(ScenarioTest::removeProperty);
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
        private Integer reportedResultLimit;

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

        /** Limits a JSON-array response body in the report to the given number of elements,
         *  appending a "..." indicator if further elements got truncated.
         *  Does not affect verification, which always sees the full response. */
        public HttpResponse limitReportedResultTo(final int maxArrayElements) {
            if (reportGenerated) {
                throw new IllegalStateException(
                        "report already generated, call limitReportedResultTo(...) before expecting(...)/keep(...)");
            }
            this.reportedResultLimit = maxArrayElements;
            return this;
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

        public String getBody() {
            return response.body();
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
            if (nextRequestInfo != null) {
                testReport.printPara(ScenarioTest.resolve(nextRequestInfo, KEEP_COMMENTS));
            }

            // the request
            testReport.printLine("```");
            printRequest();

            // the response
            testReport.printLine("=> status: " + status + " " + (locationUuid != null ? locationUuid : ""));
            if (unconditionallyWithResponse || httpMethod == HttpMethod.GET || status.isError()) {
                if (hasReportableResponseBody(response.body())) {
                    printReportableResponseBody();
                    this.reportGeneratedWithResponse = true;
                }
            }
            testReport.printLine("```");
            testReport.printLine("");
            this.reportGenerated = true;
            return this;
        }

        private void printRequest() {
            final var requestArguments = requestArguments();
            // double quotes, thus $HSADMINNG_API_BASE_URL gets expanded by the shell after copy+paste
            testReport.printLine(
                    "HTTP " + httpMethod.name() + " \"" + HSADMINNG_API_BASE_URL_ENV_VAR + uri + "\""
                            + (requestArguments.isEmpty() ? "" : " \\"));
            for (int i = 0; i < requestArguments.size(); ++i) {
                printRequestArgument(requestArguments.get(i), i < requestArguments.size() - 1);
            }
            testReport.printLine("");
        }

        private List<String> requestArguments() {
            final var request = response.request();
            if (request == null) {
                return List.of();
            }
            final var arguments = reportableRequestHeaders(request.headers().map(), hasRequestBody()).stream()
                    .flatMap(entry -> Stream.concat(
                            Stream.of(headerArgument(entry.getKey(), headerValue(entry))),
                            jwtClaimsCommentArguments(entry).stream()))
                    .toList();
            if (!hasRequestBody()) {
                return arguments;
            }
            return Stream.concat(arguments.stream(), Stream.of(requestBodyArgument(requestBody))).toList();
        }

        private void printRequestArgument(final String requestArgument, final boolean continued) {
            final var lines = requestArgument.split("\\R", -1);
            // the content and delimiter of a here-document must not be indented
            final var isHereDocument = lines[0].endsWith("<<" + HERE_DOCUMENT_DELIMITER);
            for (int i = 0; i < lines.length; ++i) {
                final var indent = (i == 0 || !isHereDocument) ? "  " : "";
                testReport.printLine(indent + lines[i] + (continued && i == lines.length - 1 ? " \\" : ""));
            }
        }

        private boolean hasRequestBody() {
            return requestBody != null && !requestBody.isBlank();
        }

        static boolean hasReportableResponseBody(final String responseBody) {
            return isNotBlank(responseBody) && !"null".equals(responseBody.trim());
        }

        private void printReportableResponseBody() {
            final var truncatedBody = reportedResultLimit == null
                    ? Optional.<String>empty()
                    : limitJsonArrayElements(response.body(), reportedResultLimit);
            if (truncatedBody.isPresent()) {
                testReport.printLine(truncatedBody.get());
            } else {
                testReport.printJson(response.body());
            }
        }

        /** @return the pretty-printed JSON array truncated to maxArrayElements with a "..." indicator on its own line,
         *          or empty if the JSON is no array or already within the limit */
        @SneakyThrows
        static Optional<String> limitJsonArrayElements(final String json, final int maxArrayElements) {
            final var rootNode = REPORT_OBJECT_MAPPER.readTree(json);
            if (!rootNode.isArray() || rootNode.size() <= maxArrayElements) {
                return Optional.empty();
            }
            final var limitedArray = REPORT_OBJECT_MAPPER.createArrayNode();
            for (int i = 0; i < maxArrayElements; ++i) {
                limitedArray.add(rootNode.get(i));
            }
            limitedArray.add("...");
            final var prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
            return Optional.of(REPORT_OBJECT_MAPPER.writer(prettyPrinter).writeValueAsString(limitedArray));
        }

        private String headerValue(final Map.Entry<String, List<String>> entry) {
            return reportableRequestHeaderValue(entry.getKey(), String.join(", ", entry.getValue()));
        }

        private String headerArgument(final String headerName, final String headerValue) {
            // double quotes if the value contains a shell variable like $HSADMINNG_JWT_BEARER, thus it gets expanded after copy+paste
            final var quote = headerValue.contains("$") ? "\"" : "'";
            return "-H " + quote + headerName + ": " + headerValue + quote;
        }

        private List<String> jwtClaimsCommentArguments(final Map.Entry<String, List<String>> entry) {
            if (!AUTH_HEADER_KEY.equalsIgnoreCase(entry.getKey())) {
                return List.of();
            }
            return UseCase.jwtClaimsCommentArguments(String.join(", ", entry.getValue()));
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
            // This marker tag should not appear in the source-code, as there is nothing to fix.
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
