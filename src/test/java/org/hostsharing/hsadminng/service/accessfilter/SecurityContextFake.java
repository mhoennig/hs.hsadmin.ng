// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

public class SecurityContextFake extends SecurityContextDouble<SecurityContextFake> {

    public static SecurityContextFake havingUnauthenticatedUser() {
        final SecurityContextFake securityContext = new SecurityContextFake();
        return securityContext;
    }

    public static SecurityContextFake havingAuthenticatedUser() {
        return havingAuthenticatedUser("dummyUser");
    }

    public static SecurityContextFake havingAuthenticatedUser(final String login) {
        final SecurityContextFake securityContext = new SecurityContextFake();
        securityContext.withAuthenticatedUser(login);
        return securityContext;
    }

    protected SecurityContextFake() {
    }
}
