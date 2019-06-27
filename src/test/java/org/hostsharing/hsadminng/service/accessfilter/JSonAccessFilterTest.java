// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class JSonAccessFilterTest {

    @Mock
    private UserRoleAssignmentService userRoleAssignmentService;

    @Test
    public void getLoginUserRoles() {
        SecurityContextFake.havingUnauthenticatedUser();
        new JSonAccessFilter<TestEntity>(null, userRoleAssignmentService, new TestEntity()) {

            {
                assertThat(this.getLoginUserRoles()).hasSize(0);
            }
        };
    }

    private static class TestEntity implements AccessMappings {

        @Override
        public Long getId() {
            return null;
        }
    }
}
