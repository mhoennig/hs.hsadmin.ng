package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().build())
                .assignedToAsset(HsHostingAssetEntity.builder().build())
                .config(Map.ofEntries(
                        entry("monit_max_hdd_usage", "90"),
                        entry("monit_max_cpu_usage", 2),
                        entry("monit_max_ram_usage", 101)
                ))
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedWebspaceHostingAssetEntity.getType());

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:vm1234.parentAsset' must be null but is set to D-???????-?:null",
                "'MANAGED_SERVER:vm1234.assignedToAsset' must be null but is set to D-???????-?:null",
                "'MANAGED_SERVER:vm1234.config.monit_max_cpu_usage' is expected to be at least 10 but is 2",
                "'MANAGED_SERVER:vm1234.config.monit_max_ram_usage' is expected to be at most 100 but is 101",
                "'MANAGED_SERVER:vm1234.config.monit_max_hdd_usage' is expected to be of type class java.lang.Integer, but is of type 'String'");
    }

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("xyz00")
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_SERVER).build())
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validate(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^vm[0-9][0-9][0-9][0-9]$', but is 'xyz00'");
    }

    @Test
    void validatesParentAndAssignedToAssetMustNotBeSet() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("xyz00")
                .parentAsset(HsHostingAssetEntity.builder().build())
                .assignedToAsset(HsHostingAssetEntity.builder().build())
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validate(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:xyz00.bookingItem' must be of type MANAGED_SERVER but is of type CLOUD_SERVER",
                "'MANAGED_SERVER:xyz00.parentAsset' must be null but is set to D-???????-?:null",
                "'MANAGED_SERVER:xyz00.assignedToAsset' must be null but is set to D-???????-?:null");
    }
}
