// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.service.dto.UserRoleAssignmentUnitTest;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

// Currently this class tests mostly special 'bad paths'
// which make little sense to test in *ResourceIntTest.
public class UserRoleAssignmentResourceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private UserRoleAssignmentResource userRoleAssignmentResource;

    @Test
    public void createUserRoleAssignmentWithoutIdThrowsBadRequestException() {

        // given
        final UserRoleAssignment givenEntity = UserRoleAssignmentUnitTest.createSomeUserRoleAssignment(null);

        // when
        final Throwable actual = catchThrowable(() -> userRoleAssignmentResource.updateUserRoleAssignment(givenEntity));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idnull");
            assertThat(bre.getParam()).isEqualTo("userRoleAssignment");
        });
    }

    @Test
    public void createUserRoleAssignmentWithIdThrowsBadRequestException() {

        // given
        final UserRoleAssignment givenEntity = UserRoleAssignmentUnitTest.createSomeUserRoleAssignment(1L);

        // when
        final Throwable actual = catchThrowable(() -> userRoleAssignmentResource.createUserRoleAssignment(givenEntity));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idexists");
            assertThat(bre.getParam()).isEqualTo("userRoleAssignment");
        });
    }
}
