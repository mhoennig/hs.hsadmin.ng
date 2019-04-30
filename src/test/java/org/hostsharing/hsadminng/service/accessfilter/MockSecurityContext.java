// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assumptions.assumeThat;

import org.hostsharing.hsadminng.security.SecurityUtils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class MockSecurityContext {

    public static void givenAuthenticatedUser() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("dummyUser", "dummyPassword"));
        SecurityContextHolder.setContext(securityContext);
        SecurityUtils.clearUserRoles();

        assumeThat(SecurityUtils.getCurrentUserLogin()).hasValue("dummyUser");
    }

    public static void givenUserHavingRole(final Class<?> onClass, final Long onId, final Role role) {
        if ((onClass == null || onId == null) && !role.isIndependent()) {
            throw new IllegalArgumentException("dependent role " + role + " needs DtoClass and ID");
        }
        SecurityUtils.addUserRole(onClass, onId, role);
    }

    public static void givenUserHavingRole(final Role role) {
        givenUserHavingRole(null, null, role);
    }

}
