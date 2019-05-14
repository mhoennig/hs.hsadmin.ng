// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.dto.MembershipDTOUnitTest;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

// Currently this class tests mostly special 'bad paths'
// which make little sense to test in *ResourceIntTest.
public class MembershipResourceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MembershipResource membershipResource;

    @Test
    public void createSepaMandateWithoutIdThrowsBadRequestException() {

        // given
        final MembershipDTO givenDto = MembershipDTOUnitTest.createRandomDTO(null, 1L);

        // when
        final Throwable actual = catchThrowable(() -> membershipResource.updateMembership(givenDto));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idnull");
            assertThat(bre.getParam()).isEqualTo("membership");
        });
    }

    @Test
    public void createSepaMandateWithIdThrowsBadRequestException() {

        // given
        final MembershipDTO givenDto = MembershipDTOUnitTest.createRandomDTO(2L, 1L);

        // when
        final Throwable actual = catchThrowable(() -> membershipResource.createMembership(givenDto));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idexists");
            assertThat(bre.getParam()).isEqualTo("membership");
        });
    }
}
