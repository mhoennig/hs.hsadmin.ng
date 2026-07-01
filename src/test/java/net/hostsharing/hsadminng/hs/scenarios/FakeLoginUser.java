package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.config.JwtFakeBearer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.KEEP_COMMENTS;

public class FakeLoginUser {
    final static String GLOBAL_AGENT = "hsh-alex_superuser"; // TODO.test: use global:AGENT when implemented

    private static final ObjectMapper REPORT_OBJECT_MAPPER = new ObjectMapper();

    private final String name;
    private final List<String> groups;
    private final String comment;

    private FakeLoginUser(final String name, final List<String> groups, final String comment) {
        this.name = name;
        this.groups = groups;
        this.comment = comment;
    }

    public static FakeLoginUser asSubject(final String name) {
        val resolvedName = ScenarioTest.resolve( name, DROP_COMMENTS);
        return new FakeLoginUser(resolvedName, null, null);
    }

    public static FakeLoginUser asSubjectWithGroups(final String name, final String... groups) {
        return asSubject(name).withGroups(groups);
    }

    public static FakeLoginUser asGlobalAgent() {
        return new FakeLoginUser(GLOBAL_AGENT, List.of(), null);
    }

    public FakeLoginUser whichIs(final String comment) {
        return new FakeLoginUser(name, groups, ScenarioTest.resolve(comment, KEEP_COMMENTS));
    }

    public FakeLoginUser withGroups(final String... groups) {
        val resolvedGroups = Arrays.stream(groups)
                .map(group -> ScenarioTest.resolve(group, DROP_COMMENTS))
                .toList();
        return new FakeLoginUser(name, resolvedGroups, comment);
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
