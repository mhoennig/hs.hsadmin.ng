package net.hostsharing.hsadminng.hs.hosting.asset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_BOOKING_ITEM;
import static org.assertj.core.api.Assertions.assertThat;

class HsHostingAssetEntityUnitTest {

    final HsHostingAssetEntity givenParentAsset = HsHostingAssetEntity.builder()
            .bookingItem(TEST_BOOKING_ITEM)
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed asset")
            .config(Map.ofEntries(
                    entry("CPUs", 2),
                    entry("SSD-storage", 512),
                    entry("HDD-storage", 2048)))
            .build();
    final HsHostingAssetEntity givenServer = HsHostingAssetEntity.builder()
            .bookingItem(TEST_BOOKING_ITEM)
            .type(HsHostingAssetType.MANAGED_WEBSPACE)
            .parentAsset(givenParentAsset)
            .identifier("xyz00")
            .caption("some managed webspace")
            .config(Map.ofEntries(
                    entry("CPUs", 2),
                    entry("SSD-storage", 512),
                    entry("HDD-storage", 2048)))
            .build();

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {
        final var result = givenServer.toString();

        assertThat(result).isEqualTo(
                "HsHostingAssetEntity(MANAGED_WEBSPACE, xyz00, some managed webspace, MANAGED_SERVER:vm1234, D-1000100:test booking item, { CPUs: 2, HDD-storage: 2048, SSD-storage: 512 })");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {
        final var result = givenServer.toShortString();

        assertThat(result).isEqualTo("MANAGED_WEBSPACE:xyz00");
    }
}
