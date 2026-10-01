package net.hostsharing.hsadminng.hs.scenarios;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import lombok.val;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

/**
 * External systems the backend calls on its own, stubbed so that scenarios can show what they provide.
 * The backend gets the stub URL through the configuration property the production system is configured with.
 */
public enum ExternalSystem {

    BUNDESBANK("Bundesbank", "hsadminng.bank-directory.import.url", "/blz-aktuell.csv");

    private static final Pattern CHARSET = Pattern.compile("charset=([^;\\s]+)");

    private final String name;
    private final String property;
    private final String entryPath;
    private final WireMockServer server = new WireMockServer(options().dynamicPort());

    ExternalSystem(final String name, final String property, final String entryPath) {
        this.name = name;
        this.property = property;
        this.entryPath = entryPath;
        server.start();
    }

    static void registerProperties(final DynamicPropertyRegistry registry) {
        for (val system : values()) {
            registry.add(system.property, () -> system.server.baseUrl() + system.entryPath);
        }
    }

    public String getName() {
        return name;
    }

    int requestCount() {
        return server.getAllServeEvents().size();
    }

    /** The requests received since the given count, oldest first, as "METHOD path". */
    List<String> requestsSince(final int count) {
        val requests = server.getAllServeEvents().reversed().stream()
                .map(event -> event.getRequest())
                .map(ExternalSystem::requestLine)
                .toList();
        return requests.subList(count, requests.size());
    }

    private static String requestLine(final LoggedRequest request) {
        return request.getMethod().getName() + " " + request.getUrl();
    }

    /** Fluent stub definitions which also print themselves into the scenario report. */
    public class Stub {

        private final TestReport testReport;

        Stub(final TestReport testReport) {
            this.testReport = testReport;
        }

        public Stub redirects(final String resolvableFromPath, final String resolvableToPath) {
            val fromPath = ScenarioTest.resolve(resolvableFromPath, DROP_COMMENTS);
            val toPath = ScenarioTest.resolve(resolvableToPath, DROP_COMMENTS);
            server.stubFor(any(urlEqualTo(fromPath)).willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location", server.baseUrl() + toPath)));
            testReport.printLine("```");
            testReport.printLine(name + ": " + fromPath + " => 302 Location: " + toPath);
            testReport.printLine("```");
            testReport.printLine("");
            return this;
        }

        public Stub fails(final String resolvablePath, final int httpStatus) {
            val path = ScenarioTest.resolve(resolvablePath, DROP_COMMENTS);
            server.stubFor(any(urlEqualTo(path)).willReturn(aResponse().withStatus(httpStatus)));
            testReport.printLine("```");
            testReport.printLine(name + ": " + path + " => " + httpStatus);
            testReport.printLine("```");
            testReport.printLine("");
            return this;
        }

        public Stub serves(final String resolvablePath, final String contentType, final String resolvableBody) {
            val path = ScenarioTest.resolve(resolvablePath, DROP_COMMENTS);
            val body = ScenarioTest.resolve(resolvableBody, DROP_COMMENTS);
            server.stubFor(get(urlEqualTo(path)).willReturn(aResponse()
                    .withHeader("Content-Type", contentType)
                    .withBody(body.getBytes(charsetOf(contentType)))));
            testReport.printLine("```");
            testReport.printLine(name + ": GET " + path + " => 200 " + contentType);
            testReport.printLine(body.strip());
            testReport.printLine("```");
            testReport.printLine("");
            return this;
        }

        private static Charset charsetOf(final String contentType) {
            val matcher = CHARSET.matcher(contentType);
            return matcher.find() ? Charset.forName(matcher.group(1)) : StandardCharsets.UTF_8;
        }
    }
}
