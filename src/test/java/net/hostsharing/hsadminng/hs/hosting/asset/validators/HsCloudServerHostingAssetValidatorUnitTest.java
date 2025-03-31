package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsCloudServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var cloudServerHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .config(Map.ofEntries(
                        entry("RAM", 2000)
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(cloudServerHostingAssetEntity.getType());


        // when
        final var result = validator.validateEntity(cloudServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'CLOUD_SERVER:vm1234.bookingItem' must be of type CLOUD_SERVER but is null",
                "'CLOUD_SERVER:vm1234.config.RAM' is not expected but is set to '2000'");
    }

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var cloudServerHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("xyz99")
                .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(cloudServerHostingAssetEntity.getType());


        // when
        final var result = validator.validateEntity(cloudServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^vm[0-9][0-9][0-9][0-9]$' but is 'xyz99'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }

    @Test
    void validatesBookingItemType() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("xyz00")
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:xyz00.bookingItem' must be of type MANAGED_SERVER but is of type CLOUD_SERVER");
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(MANAGED_SERVER).build())
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(CLOUD_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'CLOUD_SERVER:vm1234.parentAsset' must be null but is of type MANAGED_SERVER",
                "'CLOUD_SERVER:vm1234.assignedToAsset' must be null but is of type CLOUD_SERVER");
    }
}
