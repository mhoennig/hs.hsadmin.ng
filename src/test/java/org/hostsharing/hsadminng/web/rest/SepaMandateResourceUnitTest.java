package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTOUnitTest;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

// Currently this class tests mostly special 'bad paths'
// which make little sense to test in *ResourceIntTest.
public class SepaMandateResourceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SepaMandateResource sepaMandateResource;

    @Test
    public void createSepaMandateWithoutIdThrowsBadRequestException() {

        // given
        final SepaMandateDTO givenDto = SepaMandateDTOUnitTest.createRandomDTO(null, 1L);

        // when
        final Throwable actual = catchThrowable(() -> sepaMandateResource.updateSepaMandate(givenDto));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idnull");
            assertThat(bre.getParam()).isEqualTo("sepaMandate");
        });
    }

    @Test
    public void createSepaMandateWithIdThrowsBadRequestException() {

        // given
        final SepaMandateDTO givenDto = SepaMandateDTOUnitTest.createRandomDTO(2L, 1L);

        // when
        final Throwable actual = catchThrowable(() -> sepaMandateResource.createSepaMandate(givenDto));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre -> {
            assertThat(bre.getErrorKey()).isEqualTo("idexists");
            assertThat(bre.getParam()).isEqualTo("sepaMandate");
        });
    }
}
