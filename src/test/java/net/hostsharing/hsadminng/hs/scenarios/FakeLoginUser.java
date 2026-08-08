package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.config.JwtFakeBearer;

import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.KEEP_COMMENTS;

public class FakeLoginUser {
    final static String GLOBAL_AGENT = "hsh-alex_superuser"; // TODO.test: use global:AGENT when implemented

    private static final Supplier<FakeLoginUser> DEFAULT_GLOBAL_AGENT =
            () -> new FakeLoginUser(GLOBAL_AGENT, List.of(), null);
    private static Supplier<FakeLoginUser> globalAgent = DEFAULT_GLOBAL_AGENT;

    private static final ObjectMapper REPORT_OBJECT_MAPPER = new ObjectMapper();

    private final String name;
    private final List<String> groups;
    private final String comment;
    private final String apiKey; // API-key logins instead of JWT logins, name and groups are null

    private FakeLoginUser(final String name, final List<String> groups, final String comment) {
        this(name, groups, comment, null);
    }

    private FakeLoginUser(final String name, final List<String> groups, final String comment, final String apiKey) {
        this.name = name;
        this.groups = groups;
        this.comment = comment;
        this.apiKey = apiKey;
    }

    /** No authentication at all, e.g. for public endpoints called before any login. */
    public static FakeLoginUser asAnonymous() {
        return new FakeLoginUser(null, null, null);
    }

    public static FakeLoginUser asSubject(final String name) {
        val resolvedName = ScenarioTest.resolve( name, DROP_COMMENTS);
        return new FakeLoginUser(resolvedName, null, null);
    }

    public static FakeLoginUser asSubjectWithGroups(final String name, final String... groups) {
        return asSubject(name).withGroups(groups);
    }

    public static FakeLoginUser asGlobalAgent() {
        return globalAgent.get();
    }

    /**
     * Switches the identity behind asGlobalAgent() for scenarios which run without the test-data changesets,
     * where the subject hsh-alex_superuser does not exist; call resetGlobalAgent() afterwards.
     */
    public static void useAsGlobalAgent(final Supplier<FakeLoginUser> login) {
        globalAgent = login;
    }

    public static void resetGlobalAgent() {
        globalAgent = DEFAULT_GLOBAL_AGENT;
    }

    /** A login via a clear-text API-key (a real one, not fake) in the "Hostsharing-Api-Key" header, without any JWT. */
    public static FakeLoginUser withApiKey(final String resolvableApiKey) {
        return new FakeLoginUser(null, null, null, ScenarioTest.resolve(resolvableApiKey, DROP_COMMENTS));
    }

    public FakeLoginUser whichIs(final String comment) {
        return new FakeLoginUser(name, groups, ScenarioTest.resolve(comment, KEEP_COMMENTS), apiKey);
    }

    public FakeLoginUser withGroups(final String... groups) {
        val resolvedGroups = Arrays.stream(groups)
                .map(group -> ScenarioTest.resolve(group, DROP_COMMENTS))
                .toList();
        return new FakeLoginUser(name, resolvedGroups, comment, apiKey);
    }

    HttpRequest.Builder addAuthHeadersTo(final HttpRequest.Builder requestBuilder) {
        if (name == null && apiKey == null) {
            return requestBuilder;
        }
        if (apiKey != null) {
            // the real API-key must not leak into the reports, the fake header gets rendered instead
            return requestBuilder
                    .header(UseCase.API_KEY_HEADER_KEY, apiKey)
                    .header(UseCase.FAKE_API_KEY_HEADER_KEY, UseCase.API_KEY_ENV_VAR);
        }
        return requestBuilder
                .header(UseCase.AUTH_HEADER_KEY, bearer())
                .header(UseCase.FAKE_AUTH_HEADER_KEY, reportableBearer());
    }

    public String bearer() {
        return JwtFakeBearer.bearer(name, groups);
    }

    @SneakyThrows
    String reportableBearer() {
        final var claims = new LinkedHashMap<String, Object>();
        if (comment != null && !comment.isBlank()) {
            claims.put("comment", comment);
        }
        claims.put("sub", "uuid<" + name + ">");
        if (groups != null && !groups.isEmpty()) {
            claims.put("groups", groups);
        }
        return "Bearer JWT " + REPORT_OBJECT_MAPPER.writeValueAsString(claims);
    }

    String name() {
        return name;
    }
}
