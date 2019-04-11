package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class MembershipValidatorUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private MembershipValidator membershipValidator;

    @Test
    public void shouldRejectIfUntilDateIsNotAfterSinceDate() {
        // given
        final MembershipDTO membershipDTO = new MembershipDTO();
        membershipDTO.setSinceDate(LocalDate.parse("2019-04-11"));
        membershipDTO.setUntilDate(LocalDate.parse("2019-04-11"));

        // when
        final Throwable throwException = catchThrowableOfType(() -> membershipValidator.validate(membershipDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isNotNull();

    }
}
