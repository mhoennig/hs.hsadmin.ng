// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

public class AssetValidatorUnitTest {

    private AssetValidator assetValidator = new AssetValidator();

    @Test
    public void shouldAcceptValidIncreasingTransaction() {
        for (AssetAction action : ImmutableList.of(AssetAction.PAYMENT, AssetAction.ADOPTION)) {
            new GivenAssetValidationTestCase()
                    .withAnyValidDateValues()
                    .withAction(action)
                    .withAmount("64.00")
                    .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                    .thenActualException()
                    .isNull();
        }
    }

    @Test
    public void shouldAcceptValidDecreasingTransaction() {
        for (AssetAction action : ImmutableList
                .of(AssetAction.PAYBACK, AssetAction.HANDOVER, AssetAction.CLEARING, AssetAction.LOSS)) {
            new GivenAssetValidationTestCase()
                    .withAnyValidDateValues()
                    .withAction(action)
                    .withAmount("-64.00")
                    .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                    .thenActualException()
                    .isNull();
        }
    }

    @Test
    public void shouldAcceptIfDocumentDateEqualsValueDate() {
        new GivenAssetValidationTestCase()
                .withDocumentDate("2019-04-11")
                .withValueDate("2019-04-11")
                .withAction(AssetAction.PAYMENT)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isNull();
    }

    @Test
    public void shouldRejectUpdates() {
        new GivenAssetValidationTestCase()
                .withId(RandomUtils.nextLong())
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset transactions are immutable",
                                "asset",
                                "assetTransactionImmutable"));
    }

    @Test
    public void shouldRejectIfDocumentDateAfterValueDate() {
        new GivenAssetValidationTestCase()
                .withDocumentDate("2019-04-13")
                .withValueDate("2019-04-12")
                .withAction(AssetAction.PAYMENT)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Document date may not be after value date",
                                "asset",
                                "documentDateMayNotBeAfterValueDate"));
    }

    @Test
    public void shouldRejectIfPaymentWithNegativeAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.PAYMENT)
                .withAmount("-64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset payments require a positive amount",
                                "asset",
                                "assetPaymentsPositiveAmount"));
    }

    @Test
    public void shouldRejectIfPaymentWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.PAYMENT)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset payments require a positive amount",
                                "asset",
                                "assetPaymentsPositiveAmount"));
    }

    @Test
    public void shouldRejectIfAdoptionWithNegativeAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.ADOPTION)
                .withAmount("-64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset adoptions require a positive amount",
                                "asset",
                                "assetAdoptionsPositiveAmount"));
    }

    @Test
    public void shouldRejectIfAdoptionWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.ADOPTION)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset adoptions require a positive amount",
                                "asset",
                                "assetAdoptionsPositiveAmount"));
    }

    @Test
    public void shouldRejectIfPaybackWithPositiveAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.PAYBACK)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset paybacks require a negative amount",
                                "asset",
                                "assetPaybacksNegativeAmount"));
    }

    @Test
    public void shouldRejectIfPaybackWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.PAYBACK)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset paybacks require a negative amount",
                                "asset",
                                "assetPaybacksNegativeAmount"));
    }

    @Test
    public void shouldRejectIfHandoverWithPositiveAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.HANDOVER)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset handovers require a negative amount",
                                "asset",
                                "assetHandoversNegativeAmount"));
    }

    @Test
    public void shouldRejectIfHandoverWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.HANDOVER)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset handovers require a negative amount",
                                "asset",
                                "assetHandoversNegativeAmount"));
    }

    @Test
    public void shouldRejectIfLossWithPositiveAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.LOSS)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset losses require a negative amount",
                                "asset",
                                "assetLossesNegativeAmount"));
    }

    @Test
    public void shouldRejectIfLossWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.LOSS)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset losses require a negative amount",
                                "asset",
                                "assetLossesNegativeAmount"));
    }

    @Test
    public void shouldRejectIfClearingWithPositiveAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.CLEARING)
                .withAmount("64.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset clearings require a negative amount",
                                "asset",
                                "assetClearingsNegativeAmount"));
    }

    @Test
    public void shouldRejectIfClearingWithZeroAmount() {
        new GivenAssetValidationTestCase()
                .withAnyValidDateValues()
                .withAction(AssetAction.CLEARING)
                .withAmount("0.00")
                .when((AssetDTO assetDto) -> assetValidator.validate(assetDto))
                .thenActualException()
                .isEqualToComparingFieldByField(
                        new BadRequestAlertException(
                                "Asset clearings require a negative amount",
                                "asset",
                                "assetClearingsNegativeAmount"));
    }

    // -- only test fixture below ---

    private class GivenAssetValidationTestCase {

        private final AssetDTO assetDto = new AssetDTO();
        private BadRequestAlertException actualException;

        public GivenAssetValidationTestCase withId(long id) {
            assetDto.setId(id);
            return this;
        }

        GivenAssetValidationTestCase withDocumentDate(String documentDate) {
            assetDto.setDocumentDate(LocalDate.parse(documentDate));
            return this;
        }

        GivenAssetValidationTestCase withValueDate(String valueDate) {
            assetDto.setValueDate(LocalDate.parse(valueDate));
            return this;
        }

        public GivenAssetValidationTestCase withAnyValidDateValues() {
            return withDocumentDate("2019-04-11").withValueDate("2019-04-12");
        }

        GivenAssetValidationTestCase withAction(AssetAction assetAction) {
            assetDto.setAction(assetAction);
            return this;
        }

        GivenAssetValidationTestCase withAmount(String amount) {
            assetDto.setAmount(new BigDecimal(amount));
            return this;
        }

        GivenAssetValidationTestCase when(final Consumer<AssetDTO> statement) {
            actualException = catchThrowableOfType(() -> assetValidator.validate(assetDto), BadRequestAlertException.class);
            return this;
        }

        public AbstractThrowableAssert<?, ? extends Throwable> thenActualException() {
            return assertThat(actualException);
        }
    }
}
