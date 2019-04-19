package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.security.SecurityUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class MockSecurityContext {

    public static void givenLoginUserWithRole(final Role userRole) {
        final String fakeUserName = userRole.name();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(fakeUserName, "dummy"));
        SecurityContextHolder.setContext(securityContext);
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        assertThat(login).describedAs("precondition failed").contains(fakeUserName);
    }
}
