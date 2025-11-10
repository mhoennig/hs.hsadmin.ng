package net.hostsharing.hsadminng.hs.scenarios;

import lombok.AllArgsConstructor;
import net.hostsharing.hsadminng.config.JwtFakeBearer;

@AllArgsConstructor
public class FakeLoginUser {

    final static String GLOBAL_AGENT = "superuser-alex@hostsharing.net"; // TODO.test: use global:AGENT when implemented

    private String name;

    public static FakeLoginUser as(final String name) {
        return new FakeLoginUser(name);
    }

    public static FakeLoginUser asGlobalAgent() {
        return new FakeLoginUser(GLOBAL_AGENT);
    }

    public String bearer() {
        return JwtFakeBearer.bearer(name);
    }
}
