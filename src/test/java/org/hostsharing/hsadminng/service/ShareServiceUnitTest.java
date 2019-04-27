package org.hostsharing.hsadminng.service;

import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


// HINT: In IntelliJ IDEA such unit test classes can be created with Shift-Ctrl-T.
// Do not forget to amend the class name (.e.g. ...UnitTest / ...IntTest)!
public class ShareServiceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EntityManager em;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private ShareValidator shareValidator; // needed for @InjectMocks shareService

    @Mock
    private ShareMapper shareMapper;

    @InjectMocks
    private ShareService shareService;

    // HINT: Click outside of any test method (e.g. here) and use Ctrl-Shift-F10
    // to run all tests from this test class. Use Ctrl-F5 to run the last execution again;
    // 'execution' here can also apply to running the application, whatever ran last.

    // HINT: In IntelliJ IDEA such test methods can be created with Alt-Insert.
    @Test
    public void deleteIsRejectedForShareTransactions() {
        // when
        final Throwable throwException = catchThrowableOfType(() -> shareService.delete(RandomUtils.nextLong()), BadRequestAlertException.class);

        // then
        // HINT: When using auto-import for assertions (e.g. via Alt-Enter in IntelliJ IDEA),
        // beware to use the correct candidate from org.assertj.core.api.Assertions.
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Share transactions are immutable", "share", "shareTransactionImmutable"));
    }

    @Test
    public void saveShouldPersistValidTransactions() {
        // given
        final ShareDTO givenShareDTO = givenShareDTO(null, ShareAction.SUBSCRIPTION, anyPositiveNumber());
        // HINT: given(...)...will...() can't be used for void methods, in that case use Mockito's do...() methods
        doNothing().when(shareValidator).validate(givenShareDTO);

        // when
        final ShareDTO returnedShareDto = shareService.save(givenShareDTO);

        // then
        verify(em).flush();
        verify(em).refresh(any(Share.class));
        assertThat(returnedShareDto).isEqualToIgnoringGivenFields(givenShareDTO, "id");
    }

    @Test
    public void saveShouldNotPersistInvalidTransactions() {
        // given
        final ShareDTO givenShareDTO = givenShareDTO(null, ShareAction.SUBSCRIPTION, anyNegativeNumber());
        doThrow(new BadRequestAlertException("Some Dummy Test Violation", "share", "shareInvalidTestDummy")).when(shareValidator).validate(givenShareDTO);

        // when
        final Throwable throwException = catchThrowableOfType(() -> shareService.save(givenShareDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Some Dummy Test Violation", "share", "shareInvalidTestDummy"));
    }

    @Test
    public void saveShouldUpdateValidTransactions() {
        // given
        final ShareDTO givenShareDTO = givenShareDTO(anyNonNullId(), ShareAction.SUBSCRIPTION, anyPositiveNumber());
        doNothing().when(shareValidator).validate(givenShareDTO);

        // when
        final ShareDTO returnedShareDto = shareService.save(givenShareDTO);

        // then
        verify(em).flush();
        verify(em).refresh(any(Share.class));
        assertThat(returnedShareDto).isEqualToIgnoringGivenFields(givenShareDTO, "id");
    }

    @Test
    public void saveShouldNotUpdateInvalidTransactions() {
        // given
        final ShareDTO givenShareDTO = givenShareDTO(anyNonNullId(), ShareAction.SUBSCRIPTION, anyNegativeNumber());
        // HINT: given(...) can't be used for void methods, in that case use Mockito's do...() methods
        doThrow(new BadRequestAlertException("Some Dummy Test Violation", "share", "shareInvalidTestDummy")).when(shareValidator).validate(givenShareDTO);

        // when
        final Throwable throwException = catchThrowableOfType(() -> shareService.save(givenShareDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Some Dummy Test Violation", "share", "shareInvalidTestDummy"));
    }

    // --- only test fixture code below ---

    private long anyNonNullId() {
        return RandomUtils.nextInt();
    }

    // HINT: This rather complicated setup indicates that the method ShareService::save breaks the single responsibility principle.
    private ShareDTO givenShareDTO(final Long id, final ShareAction givenAction, final int givenQuantity) {
        final ShareDTO givenShareDTO = createShareDTO(id, givenAction, givenQuantity);

        // dto -> entity
        final Share givenShareEntity = Mockito.mock(Share.class);
        given(shareMapper.toEntity(same(givenShareDTO))).willReturn(givenShareEntity);

        // shareRepository.save(entity);
        final Share persistedShareEntity = Mockito.mock(Share.class);
        given(shareRepository.save(same(givenShareEntity))).willReturn(persistedShareEntity);

        // entity -> dto
        ShareDTO persistedShareDTO = createShareDTO(id == null ? RandomUtils.nextLong() : id, givenAction, givenQuantity);
        given(shareMapper.toDto(same(persistedShareEntity))).willReturn(persistedShareDTO);

        return givenShareDTO;
    }

    private ShareDTO createShareDTO(Long id, ShareAction givenAction, int givenQuantity) {
        final ShareDTO givenShareDTO = new ShareDTO();
        givenShareDTO.setId(id);
        givenShareDTO.setAction(givenAction);
        givenShareDTO.setQuantity(givenQuantity);
        return givenShareDTO;
    }

    private int anyPositiveNumber() {
        return RandomUtils.nextInt(1, 1000);
    }

    private int anyNegativeNumber() {
        return -anyPositiveNumber();
    }
}
