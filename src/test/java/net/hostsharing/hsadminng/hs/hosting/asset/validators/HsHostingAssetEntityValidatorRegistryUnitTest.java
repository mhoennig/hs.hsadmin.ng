package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

class HsHostingAssetEntityValidatorRegistryUnitTest {

    @Test
    void forTypeWithUnknownTypeThrowsException() {
        // when
        final var thrown = catchThrowable(() -> {
            HsHostingAssetEntityValidatorRegistry.forType(null);
        });

        // then
        assertThat(thrown).hasMessage("no validator found for type null");
    }

    @Test
    void typesReturnsAllImplementedTypes() {
        // when
        final var types = HsHostingAssetEntityValidatorRegistry.types();

        // then
        // TODO.test: when all types are implemented, replace with set of all types:
        // assertThat(types).isEqualTo(EnumSet.allOf(HsHostingAssetType.class));
        // also remove "Implemented" from the test method name.
        assertThat(types).containsExactlyInAnyOrder(
                HsHostingAssetType.CLOUD_SERVER,
                HsHostingAssetType.MANAGED_SERVER,
                HsHostingAssetType.MANAGED_WEBSPACE,
                HsHostingAssetType.UNIX_USER
        );
    }

    @Test
    void validatedDoesNotThrowAnExceptionForValidEntity() {
        final var givenBookingItem = HsBookingItemEntity.builder()
                .type(HsBookingItemType.CLOUD_SERVER)
                .resources(Map.ofEntries(
                        entry("CPUs", 4),
                        entry("RAM", 20),
                        entry("SSD", 50),
                        entry("Traffic", 250)
                ))
                .build();
        final var validEntity = HsHostingAssetEntity.builder()
                .type(HsHostingAssetType.CLOUD_SERVER)
                .bookingItem(givenBookingItem)
                .identifier("vm1234")
                .caption("some valid cloud server")
                .build();
        HsHostingAssetEntityValidatorRegistry.validated(validEntity);
    }
}
