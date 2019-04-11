package org.hostsharing.hsadminng.service;

import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MembershipServiceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipValidator membershipValidator;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    public void deleteIsRejectedForMembership() {
        // when
        final Throwable throwException = catchThrowableOfType(() -> membershipService.delete(RandomUtils.nextLong()), BadRequestAlertException.class);

        // then
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Membership cannot be deleted", "membership", "membershipNotDeletable"));
    }

    @Test
    public void saveRejectsInvalidMembershipDTO() {
        // given
        final MembershipDTO givenMembershipDTO = new MembershipDTO();
        final BadRequestAlertException givenBadRequestAlertException = new BadRequestAlertException("Invalid Membership", Membership.ENTITY_NAME, "invalidMembership");
        doThrow(givenBadRequestAlertException).when(membershipValidator).validate(givenMembershipDTO);

        // when
        final Throwable throwException = catchThrowableOfType(() -> membershipService.save(givenMembershipDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isSameAs(givenBadRequestAlertException);
        verify(membershipRepository, never()).save(any(Membership.class));
    }

}
