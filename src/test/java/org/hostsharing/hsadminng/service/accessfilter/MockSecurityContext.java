// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;

import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class MockSecurityContext {

    private final UserRoleAssignmentService userRoleAssignmentService;
    private final Collection<GrantedAuthority> authorities;

    // TODO mhoennig: refactor this ctor to method withMock(...) returning a subclass to avoid null checks
    public MockSecurityContext(final UserRoleAssignmentService userRoleAssignmentService) {
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.authorities = new ArrayList<>();
    }

    public MockSecurityContext() {
        this(null);
    }

    public MockSecurityContext havingAuthenticatedUser() {
        return havingAuthenticatedUser("dummyUser");
    }

    public MockSecurityContext havingAuthenticatedUser(final String login) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(login, "dummyPassword") {

            @Override
            public Collection<GrantedAuthority> getAuthorities() {
                return authorities;
            }
        });
        SecurityContextHolder.setContext(securityContext);

        assumeThat(SecurityUtils.getCurrentUserLogin()).hasValue(login);
        if (userRoleAssignmentService != null) {
            Mockito.reset(userRoleAssignmentService);
        }
        authorities.clear();
        return this;
    }

    public MockSecurityContext withRole(final Class<?> onClass, final Long onId, final Role... roles) {
        if (userRoleAssignmentService == null) {
            throw new IllegalStateException("mock not registered for: " + UserRoleAssignmentService.class.getSimpleName());
        }
        final EntityTypeId entityTypeId = onClass.getAnnotation(EntityTypeId.class);
        assumeThat(entityTypeId).as("@" + EntityTypeId.class.getSimpleName() + " missing on class " + onClass.toString())
                .isNotNull();
        given(userRoleAssignmentService.getEffectiveRoleOfCurrentUser(entityTypeId.value(), onId))
                .willReturn(new HashSet(Arrays.asList(roles)));
        return this;
    }

    public MockSecurityContext withRole(final Role role) {
        authorities.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                return role.asAuthority();
            }
        });
        return this;
    }

    private static class FakePrincipal {

        private final String username;

        public FakePrincipal(final String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return username;
        }
    }
}
