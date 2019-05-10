// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;

import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;

public class SecurityContextMock extends SecurityContextDouble<SecurityContextMock> {

    private final UserRoleAssignmentService userRoleAssignmentService;

    public static SecurityContextMock usingMock(final UserRoleAssignmentService userRoleAssignmentService) {
        return new SecurityContextMock(userRoleAssignmentService);
    }

    public SecurityContextMock(final UserRoleAssignmentService userRoleAssignmentService) {
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    public SecurityContextMock havingAuthenticatedUser() {
        return havingAuthenticatedUser("dummyUser");
    }

    public SecurityContextMock havingAuthenticatedUser(final String login) {
        super.withAuthenticatedUser(login);
        Mockito.reset(userRoleAssignmentService);
        return this;
    }

    public SecurityContextMock withRole(final Class<?> onClass, final long onId, final Role... roles) {
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
}
