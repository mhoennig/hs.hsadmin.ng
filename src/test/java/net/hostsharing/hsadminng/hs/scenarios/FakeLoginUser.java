package net.hostsharing.hsadminng.hs.scenarios;

import lombok.AllArgsConstructor;
import lombok.val;
import net.hostsharing.hsadminng.config.JwtFakeBearer;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

@AllArgsConstructor
public class FakeLoginUser {
    final static String GLOBAL_AGENT = "superuser-alex@hostsharing.net"; // TODO.test: use global:AGENT when implemented

    private String name;

    public static FakeLoginUser asSubject(final String name) {
        val resolvedName = ScenarioTest.resolve( name, DROP_COMMENTS);
        return new FakeLoginUser(resolvedName);
    }

    public static FakeLoginUser asGlobalAgent() {
        return new FakeLoginUser(GLOBAL_AGENT);
    }

    public String bearer() {
        return JwtFakeBearer.bearer(name);
    }

    String name() {
        return name;
    }
}
