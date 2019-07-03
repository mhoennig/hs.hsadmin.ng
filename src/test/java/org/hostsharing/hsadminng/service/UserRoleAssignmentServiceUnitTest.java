// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerFinancialContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerTechnicalContact;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextFake;

import com.google.common.base.VerifyException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Set;

public class UserRoleAssignmentServiceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;

    @InjectMocks
    private UserRoleAssignmentService userRoleAssignmentService;

    @Test
    public void getEffectiveRoleOfCurrentUserReturnsEmptySetIfUserNotAuthenticated() {
        // when
        final Set<Role> actual = userRoleAssignmentService.getEffectiveRoleOfCurrentUser("test.Something", 1L);

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    public void getEffectiveRoleOfCurrentUserReturnsEmptySetIfUserAuthenticatedButNoRolesAssigned() {
        // given
        SecurityContextFake.havingAuthenticatedUser();

        // when
        final Set<Role> actual = userRoleAssignmentService.getEffectiveRoleOfCurrentUser("test.Something", 1L);

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    public void getEffectiveRoleOfCurrentUserReturnsExactlyAssignedRoles() {
        // given
        final String givenUserLogin = "someUser";
        SecurityContextFake.havingAuthenticatedUser(givenUserLogin);
        final long givenEntityObjectId = 2L;
        final String givenEntityTypeId = "test.Something";
        given(userRoleAssignmentRepository.findByLogin(givenUserLogin)).willReturn(
                Arrays.asList(
                        new UserRoleAssignment().entityTypeId("test.SomethingElse")
                                .entityObjectId(givenEntityObjectId)
                                .assignedRole(CustomerContractualContact.ROLE),
                        new UserRoleAssignment().entityTypeId(givenEntityTypeId)
                                .entityObjectId(givenEntityObjectId)
                                .assignedRole(CustomerFinancialContact.ROLE),
                        new UserRoleAssignment().entityTypeId(givenEntityTypeId)
                                .entityObjectId(givenEntityObjectId)
                                .assignedRole(CustomerTechnicalContact.ROLE),
                        new UserRoleAssignment().entityTypeId(givenEntityTypeId)
                                .entityObjectId(3L)
                                .assignedRole(CustomerContractualContact.ROLE)));

        // when
        final Set<Role> actual = userRoleAssignmentService
                .getEffectiveRoleOfCurrentUser(givenEntityTypeId, givenEntityObjectId);

        // then
        assertThat(actual)
                .containsExactlyInAnyOrder(Role.of(CustomerFinancialContact.class), Role.of(CustomerTechnicalContact.class));
    }

    @Test
    public void getEffectiveRoleOfCurrentUserThrowsExceptionIfEntityTypeIdIsMissing() {
        // when
        final Throwable actual = catchThrowable(() -> userRoleAssignmentService.getEffectiveRoleOfCurrentUser(null, 1L));

        // then
        assertThat(actual).isInstanceOf(VerifyException.class);
    }
}
