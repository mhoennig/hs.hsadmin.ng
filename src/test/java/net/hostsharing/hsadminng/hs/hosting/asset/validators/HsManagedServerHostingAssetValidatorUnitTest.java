package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_CLOUD_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
                .parentAsset(HsHostingAssetEntity.builder().type(CLOUD_SERVER).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(CLOUD_SERVER).build())
                .config(Map.ofEntries(
                        entry("monit_max_hdd_usage", "90"),
                        entry("monit_max_cpu_usage", 2),
                        entry("monit_max_ram_usage", 101)
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedWebspaceHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:vm1234.parentAsset' must be null but is of type CLOUD_SERVER",
                "'MANAGED_SERVER:vm1234.assignedToAsset' must be null but is of type CLOUD_SERVER",
                "'MANAGED_SERVER:vm1234.config.monit_max_cpu_usage' is expected to be at least 10 but is 2",
                "'MANAGED_SERVER:vm1234.config.monit_max_ram_usage' is expected to be at most 100 but is 101",
                "'MANAGED_SERVER:vm1234.config.monit_max_hdd_usage' is expected to be of type Integer, but is of type String");
    }

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("xyz00")
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^vm[0-9][0-9][0-9][0-9]$', but is 'xyz00'");
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("xyz00")
                .bookingItem(TEST_CLOUD_SERVER_BOOKING_ITEM)
                .parentAsset(HsHostingAssetEntity.builder().type(CLOUD_SERVER).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(MANAGED_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:xyz00.bookingItem' must be of type MANAGED_SERVER but is of type CLOUD_SERVER",
                "'MANAGED_SERVER:xyz00.parentAsset' must be null but is of type CLOUD_SERVER",
                "'MANAGED_SERVER:xyz00.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }
}
