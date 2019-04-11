package org.hostsharing.hsadminng.service;

import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.mapper.MembershipMapper;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class MembershipServiceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipMapper membershipMapper;

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
}
