package net.hostsharing.hsadminng.hs.validation;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IntegerPropertyUnitTest {

    final IntegerProperty<?> partialIntegerProperty = integerProperty("test")
        .min(1)
        .max(9);

    @Test
    void returnsConfiguredSettings() {
        final var IntegerProperty = partialIntegerProperty;
        assertThat(IntegerProperty.propertyName()).isEqualTo("test");
        assertThat(IntegerProperty.unit()).isNull();
        assertThat(IntegerProperty.min()).isEqualTo(1);
        assertThat(IntegerProperty.max()).isEqualTo(9);
    }

    @Test
    void detectsIncompleteConfiguration() {
        final var IntegerProperty = partialIntegerProperty;
        final var exception = catchThrowable(() ->
            IntegerProperty.verifyConsistency(Map.entry(HsBookingItemType.CLOUD_SERVER, "val"))
        );
        assertThat(exception).isNotNull().isInstanceOf(IllegalStateException.class).hasMessageContaining(
            "CLOUD_SERVER[test] not fully initialized, please call either .readOnly(), .required(), .optional(), .withDefault(...), .requiresAtLeastOneOf(...) or .requiresAtMaxOneOf(...)"
        );
    }

    @Test
    void initializerCompletesProperty() {
        // given
        final var IntegerProperty = partialIntegerProperty
                .initializedBy((entityManager, propertiesProvider) -> 7);

        // then
        isCompleted(IntegerProperty);
        assertThat(IntegerProperty.isComputed(ValidatableProperty.ComputeMode.IN_INIT)).isTrue();
        assertThat(IntegerProperty.compute(null, null)).isEqualTo(7);
    }

    @Test
    void displaysNullValueAsNull() {
        final var IntegerProperty = partialIntegerProperty.optional();
        assertThat(IntegerProperty.display(null)).isNull();
    }

    @Test
    void displayQuotesValue() {
        final var IntegerProperty = partialIntegerProperty.optional();
        assertThat(IntegerProperty.display(3)).isEqualTo("3");
    }

    private static void isCompleted(IntegerProperty<? extends IntegerProperty<?>> IntegerProperty) {
        IntegerProperty.verifyConsistency(Map.entry(HsBookingItemType.CLOUD_SERVER, "val"));
    }
}
