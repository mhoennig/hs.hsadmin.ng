package net.hostsharing.hsadminng.hs.hosting.asset;

import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_WEBSPACE_BOOKING_ITEM;

public class TestHsHostingAssetEntities {

    public static final HsHostingAssetEntity TEST_MANAGED_SERVER_HOSTING_ASSET = HsHostingAssetEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed server")
            .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
            .build();

    public static final HsHostingAssetEntity TEST_MANAGED_WEBSPACE_HOSTING_ASSET = HsHostingAssetEntity.builder()
            .type(HsHostingAssetType.MANAGED_WEBSPACE)
            .identifier("xyz00")
            .caption("some managed webspace")
            .bookingItem(TEST_MANAGED_WEBSPACE_BOOKING_ITEM)
            .build();

}
