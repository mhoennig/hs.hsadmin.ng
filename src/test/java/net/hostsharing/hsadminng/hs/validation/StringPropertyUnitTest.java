package net.hostsharing.hsadminng.hs.validation;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class StringPropertyUnitTest {

    final StringProperty<?> partialStringProperty = stringProperty("test")
        .minLength(1)
        .maxLength(9)
        .provided("one", "two", "three");

    @Test
    void returnsConfiguredSettings() {
        final var stringProperty = partialStringProperty;
        assertThat(stringProperty.propertyName()).isEqualTo("test");
        assertThat(stringProperty.unit()).isNull();
        assertThat(stringProperty.minLength()).isEqualTo(1);
        assertThat(stringProperty.maxLength()).isEqualTo(9);
        assertThat(stringProperty.provided()).isEqualTo(Array.of("one", "two", "three"));
    }

    @Test
    void detectsIncompleteConfiguration() {
        final var stringProperty = partialStringProperty;
        final var exception = catchThrowable(() ->
            stringProperty.verifyConsistency(Map.entry(HsBookingItemType.CLOUD_SERVER, "val"))
        );
        assertThat(exception).isNotNull().isInstanceOf(IllegalStateException.class).hasMessageContaining(
            "CLOUD_SERVER[test] not fully initialized, please call either .readOnly(), .required(), .optional(), .withDefault(...), .requiresAtLeastOneOf(...) or .requiresAtMaxOneOf(...)"
        );
    }

    @Test
    void initializerCompletesProperty() {
        // given
        final var stringProperty = partialStringProperty
                .initializedBy((entityManager, propertiesProvider) -> "init-value");

        // then
        isCompleted(stringProperty);
        assertThat(stringProperty.isComputed(ValidatableProperty.ComputeMode.IN_INIT)).isTrue();
        assertThat(stringProperty.compute(null, null)).isEqualTo("init-value");
    }

    @Test
    void displaysNullValueAsNull() {
        final var stringProperty = partialStringProperty.optional();
        assertThat(stringProperty.display(null)).isNull();
    }


    @Test
    void displayQuotesValue() {
        final var stringProperty = partialStringProperty.optional();
        assertThat(stringProperty.display("some value")).isEqualTo("'some value'");
    }

    private static void isCompleted(StringProperty<? extends StringProperty<?>> stringProperty) {
        stringProperty.verifyConsistency(Map.entry(HsBookingItemType.CLOUD_SERVER, "val"));
    }
}
