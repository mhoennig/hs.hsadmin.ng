package org.hostsharing.hsadminng.service;

import org.assertj.core.api.AbstractThrowableAssert;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

public class MembershipValidatorUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private MembershipValidator membershipValidator;

    @Before
    public void initMocks() {
        given(membershipRepository.hasUncancelledMembershipForCustomer(anyLong())).willReturn(false);
    }

    @Test
    public void shouldAcceptNewMembershipIfUntilDateAfterSinceDate() {
        new GivenMembershipValidationTestCase()
            .withNewMembershipForCustomer(1L).since("2019-04-11").until("2019-04-12")
            .when((MembershipDTO membershipDto) -> membershipValidator.validate(membershipDto))
            .thenActualException().isNull();
    }

    @Test
    public void shouldRejectNewMembershipIfUntilDateEqualToSinceDate() {
        new GivenMembershipValidationTestCase()
            .withNewMembershipForCustomer(1L).since("2019-04-11").until("2019-04-11")
            .when((MembershipDTO membershipDto) -> membershipValidator.validate(membershipDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Invalid untilDate", "membership", "untilDateMustBeAfterSinceDate"));
    }

    @Test
    public void shouldRejectNewMembershipIfUntilDateAfterSinceDate() {
        new GivenMembershipValidationTestCase()
            .withNewMembershipForCustomer(1L).since("2019-04-12").until("2019-04-11")
            .when((MembershipDTO membershipDto) -> membershipValidator.validate(membershipDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Invalid untilDate", "membership", "untilDateMustBeAfterSinceDate"));
    }

    @Test
    public void shouldAcceptNewUncancelledMembershipIfNoUncancelledMembershipExistsForSameCustomer() {
        new GivenMembershipValidationTestCase()
            .withUncancelledMembershipForCustomer(1L, false)
            .withNewMembershipForCustomer(1L).since("2019-04-12")
            .when((MembershipDTO membershipDto) -> membershipValidator.validate(membershipDto))
            .thenActualException().isNull();
    }

    @Test
    public void shouldRejectNewMembershipIfAnyUncancelledMembershipExistsForSameCustomer() {

        new GivenMembershipValidationTestCase()
            .withUncancelledMembershipForCustomer(1L, true)
            .withNewMembershipForCustomer(1L).since("2019-04-12")
            .when((MembershipDTO membershipDto) -> membershipValidator.validate(membershipDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Another uncancelled membership exists", "membership", "anotherUncancelledMembershipExists"));
    }

    // -- only test fixture below ---

    private class GivenMembershipValidationTestCase {

        private final MembershipDTO membershipDto = new MembershipDTO();
        private BadRequestAlertException actualException;

        GivenMembershipValidationTestCase withUncancelledMembershipForCustomer(final long customerId, final boolean hasUncancelledMembership) {
            given(membershipRepository.hasUncancelledMembershipForCustomer(customerId)).willReturn(hasUncancelledMembership);
            return this;
        }

        GivenMembershipValidationTestCase withNewMembershipForCustomer(long customerId) {
            membershipDto.setCustomerId(1L);
            return this;
        }


        GivenMembershipValidationTestCase since(final String sinceDate) {
            membershipDto.setSinceDate(LocalDate.parse(sinceDate));
            return this;
        }

        public GivenMembershipValidationTestCase until(final String untilDate) {
            membershipDto.setUntilDate(LocalDate.parse(untilDate));
            return this;
        }

        GivenMembershipValidationTestCase when(final Consumer<MembershipDTO> statement) {
            actualException = catchThrowableOfType(() -> membershipValidator.validate(membershipDto), BadRequestAlertException.class);
            return this;
        }

        public AbstractThrowableAssert<?, ? extends Throwable> thenActualException() {
            return assertThat(actualException);
        }
    }
}
