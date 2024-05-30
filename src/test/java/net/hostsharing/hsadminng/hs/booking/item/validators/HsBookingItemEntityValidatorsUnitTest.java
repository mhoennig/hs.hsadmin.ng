package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidators.valid;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsBookingItemEntityValidatorsUnitTest {

    @Test
    void validThrowsException() {
        // given
        final var cloudServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(CLOUD_SERVER)
                .build();

        // when
        final var result = catchThrowable( ()-> valid(cloudServerBookingItemEntity) );

        // then
        assertThat(result).isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "'resources.CPUs' is required but missing",
                        "'resources.RAM' is required but missing",
                        "'resources.SSD' is required but missing",
                        "'resources.Traffic' is required but missing");
    }

    @Test
    void listsTypes() {
        // when
        final var result = HsBookingItemEntityValidators.types();

        // then
        assertThat(result).containsExactlyInAnyOrder(CLOUD_SERVER, MANAGED_SERVER, MANAGED_WEBSPACE);
    }
}
