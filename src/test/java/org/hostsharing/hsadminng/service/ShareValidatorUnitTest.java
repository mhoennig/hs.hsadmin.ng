package org.hostsharing.hsadminng.service;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.AbstractThrowableAssert;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Test;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class ShareValidatorUnitTest {

    private ShareValidator shareValidator = new ShareValidator();

    @Test
    public void shouldAcceptValidSubscription() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.SUBSCRIPTION).withQuantity(1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isNull();
    }

    @Test
    public void shouldAcceptValidCancellation() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.CANCELLATION).withQuantity(-1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isNull();
    }

    @Test
    public void shouldAcceptIfDocumentDateEqualsValueDate() {
        new GivenShareValidationTestCase()
            .withDocumentDate("2019-04-11").withValueDate("2019-04-11")
            .withAction(ShareAction.SUBSCRIPTION).withQuantity(1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isNull();
    }

    @Test
    public void shouldRejectUpdates() {
        new GivenShareValidationTestCase()
            .withId(RandomUtils.nextLong())
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Share transactions are immutable", "share", "shareTransactionImmutable"));
    }

    @Test
    public void shouldRejectIfDocumentDateAfterValueDate() {
        new GivenShareValidationTestCase()
            .withDocumentDate("2019-04-13").withValueDate("2019-04-12")
            .withAction(ShareAction.SUBSCRIPTION).withQuantity(1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Document date may not be after value date", "share", "documentDateMayNotBeAfterValueDate"));
    }

    @Test
    public void shouldRejectIfSubscriptionWithNegativeQuantity() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.SUBSCRIPTION).withQuantity(-1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Share subscriptions require a positive quantity", "share", "shareSubscriptionPositiveQuantity"));
    }

    @Test
    public void shouldRejectIfSubscriptionWithZeroQuantity() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.SUBSCRIPTION).withQuantity(0)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Share subscriptions require a positive quantity", "share", "shareSubscriptionPositiveQuantity"));
    }

    @Test
    public void shouldRejectIfCancellationWithPositiveQuantity() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.CANCELLATION).withQuantity(1)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Share cancellations require a negative quantity", "share", "shareCancellationNegativeQuantity"));
    }

    @Test
    public void shouldRejectIfCancellationWithZeroQuantity() {
        new GivenShareValidationTestCase()
            .withAnyValidDateValues()
            .withAction(ShareAction.CANCELLATION).withQuantity(0)
            .when((ShareDTO shareDto) -> shareValidator.validate(shareDto))
            .thenActualException().isEqualToComparingFieldByField(new BadRequestAlertException(
            "Share cancellations require a negative quantity", "share", "shareCancellationNegativeQuantity"));
    }


    // -- only test fixture below ---

    private class GivenShareValidationTestCase {

        private final ShareDTO shareDto = new ShareDTO();
        private BadRequestAlertException actualException;

        public GivenShareValidationTestCase withId(long id) {
            shareDto.setId(id);
            return this;
        }

        GivenShareValidationTestCase withDocumentDate(String documentDate) {
            shareDto.setDocumentDate(LocalDate.parse(documentDate));
            return this;
        }

        GivenShareValidationTestCase withValueDate(String valueDate) {
            shareDto.setValueDate(LocalDate.parse(valueDate));
            return this;
        }

        public GivenShareValidationTestCase withAnyValidDateValues() {
            return withDocumentDate("2019-04-11").withValueDate("2019-04-12");
        }

        GivenShareValidationTestCase withAction(ShareAction shareAction) {
            shareDto.setAction(shareAction);
            return this;
        }

        GivenShareValidationTestCase withQuantity(Integer quantity) {
            shareDto.setQuantity(quantity);
            return this;
        }

        GivenShareValidationTestCase  when(final Consumer<ShareDTO> statement) {
            actualException = catchThrowableOfType(() -> shareValidator.validate(shareDto), BadRequestAlertException.class);
            return this;
        }

        public AbstractThrowableAssert<?, ? extends Throwable> thenActualException() {
            return assertThat(actualException);
        }
    }
}


