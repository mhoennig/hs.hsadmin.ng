package org.hostsharing.hsadminng.service;

import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.repository.AssetRepository;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.service.mapper.AssetMapper;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


// HINT: In IntelliJ IDEA such unit test classes can be created with Shift-Ctrl-T.
// Do not forget to amend the class name (.e.g. ...UnitTest / ...IntTest)!
public class AssetServiceUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EntityManager em;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetValidator assetValidator; // needed for @InjectMocks assetService

    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetService assetService;

    // HINT: Click outside of any test method (e.g. here) and use Ctrl-Shift-F10
    // to run all tests from this test class. Use Ctrl-F5 to run the last execution again;
    // 'execution' here can also apply to running the application, whatever ran last.

    // HINT: In IntelliJ IDEA such test methods can be created with Alt-Insert.
    @Test
    public void deleteIsRejectedForAssetTransactions() {
        // when
        final Throwable throwException = catchThrowableOfType(() -> assetService.delete(RandomUtils.nextLong()), BadRequestAlertException.class);

        // then
        // HINT: When using auto-import for assertions (e.g. via Alt-Enter in IntelliJ IDEA),
        // beware to use the correct candidate from org.assertj.core.api.Assertions.
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Asset transactions are immutable", "asset", "assetTransactionImmutable"));
    }

    @Test
    public void saveShouldPersistValidTransactions() {
        // given
        final AssetDTO givenAssetDTO = givenAssetDTO(null, AssetAction.PAYMENT, anyPositiveAmout());
        // HINT: given(...)...will...() can't be used for void methods, in that case use Mockito's do...() methods
        doNothing().when(assetValidator).validate(givenAssetDTO);

        // when
        final AssetDTO returnedAssetDto = assetService.save(givenAssetDTO);

        // then
        verify(em).flush();
        verify(em).refresh(any(Asset.class));
        assertThat(returnedAssetDto).isEqualToIgnoringGivenFields(givenAssetDTO, "id");
    }

    @Test
    public void saveShouldNotPersistInvalidTransactions() {
        // given
        final AssetDTO givenAssetDTO = givenAssetDTO(null, AssetAction.PAYMENT, anyNegativeAmount());
        doThrow(new BadRequestAlertException("Some Dummy Test Violation", "asset", "assetInvalidTestDummy")).when(assetValidator).validate(givenAssetDTO);

        // when
        final Throwable throwException = catchThrowableOfType(() -> assetService.save(givenAssetDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Some Dummy Test Violation", "asset", "assetInvalidTestDummy"));
    }

    @Test
    public void saveShouldUpdateValidTransactions() {
        // given
        final AssetDTO givenAssetDTO = givenAssetDTO(anyNonNullId(), AssetAction.PAYMENT, anyPositiveAmout());
        doNothing().when(assetValidator).validate(givenAssetDTO);

        // when
        final AssetDTO returnedAssetDto = assetService.save(givenAssetDTO);

        // then
        verify(em).flush();
        verify(em).refresh(any(Asset.class));
        assertThat(returnedAssetDto).isEqualToIgnoringGivenFields(givenAssetDTO, "id");
    }

    @Test
    public void saveShouldNotUpdateInvalidTransactions() {
        // given
        final AssetDTO givenAssetDTO = givenAssetDTO(anyNonNullId(), AssetAction.PAYMENT, anyNegativeAmount());
        // HINT: given(...) can't be used for void methods, in that case use Mockito's do...() methods
        doThrow(new BadRequestAlertException("Some Dummy Test Violation", "asset", "assetInvalidTestDummy")).when(assetValidator).validate(givenAssetDTO);

        // when
        final Throwable throwException = catchThrowableOfType(() -> assetService.save(givenAssetDTO), BadRequestAlertException.class);

        // then
        assertThat(throwException).isEqualToComparingFieldByField(
            new BadRequestAlertException("Some Dummy Test Violation", "asset", "assetInvalidTestDummy"));
    }

    // --- only test fixture code below ---

    private long anyNonNullId() {
        return RandomUtils.nextInt();
    }

    // HINT: This rather complicated setup indicates that the method AssetService::save breaks the single responsibility principle.
    private AssetDTO givenAssetDTO(final Long id, final AssetAction givenAction, final BigDecimal givenQuantity) {
        final AssetDTO givenAssetDTO = createAssetDTO(id, givenAction, givenQuantity);

        // dto -> entity
        final Asset givenAssetEntity = Mockito.mock(Asset.class);
        given(assetMapper.toEntity(same(givenAssetDTO))).willReturn(givenAssetEntity);

        // assetRepository.save(entity);
        final Asset persistedAssetEntity = Mockito.mock(Asset.class);
        given(assetRepository.save(same(givenAssetEntity))).willReturn(persistedAssetEntity);

        // entity -> dto
        AssetDTO persistedAssetDTO = createAssetDTO(id == null ? RandomUtils.nextLong() : id, givenAction, givenQuantity);
        given(assetMapper.toDto(same(persistedAssetEntity))).willReturn(persistedAssetDTO);

        return givenAssetDTO;
    }

    private AssetDTO createAssetDTO(Long id, AssetAction givenAction, BigDecimal givenAmount) {
        final AssetDTO givenAssetDTO = new AssetDTO();
        givenAssetDTO.setId(id);
        givenAssetDTO.setAction(givenAction);
        givenAssetDTO.setAmount(givenAmount);
        return givenAssetDTO;
    }

    private BigDecimal anyPositiveAmout() {
        return BigDecimal.valueOf(RandomUtils.nextInt()).add(new BigDecimal("0.1"));
    }

    private BigDecimal anyNegativeAmount() {
        return anyPositiveAmout().negate();
    }
}
