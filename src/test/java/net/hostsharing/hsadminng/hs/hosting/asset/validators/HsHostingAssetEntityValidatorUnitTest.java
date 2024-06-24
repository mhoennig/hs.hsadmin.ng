package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;


import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsHostingAssetEntityValidatorUnitTest {

    @Test
    void validThrowsException() {
        // given
        final var managedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_SERVER).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(MANAGED_SERVER).build())
                .build();

        // when
        final var result = catchThrowable( ()-> HsHostingAssetEntityValidatorRegistry.validated(managedServerHostingAssetEntity));

        // then
        assertThat(result.getMessage()).contains(
                "'MANAGED_SERVER:vm1234.parentAsset' must be null but is set to D-???????-?:null",
                "'MANAGED_SERVER:vm1234.assignedToAsset' must be null but is set to D-???????-?:null"
        );
    }
}
