package net.hostsharing.hsadminng.hs.hosting.asset;

import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.MANAGED_WEBSPACE_BOOKING_ITEM_REAL_ENTITY;

public class HsHostingAssetTestEntities {

    public static final HsHostingAssetRbacEntity MANAGED_SERVER_HOSTING_ASSET_RBAC_TEST_ENTITY = HsHostingAssetRbacEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed server")
            .bookingItem(MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY)
            .build();

    public static final HsHostingAssetRealEntity MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed server")
            .bookingItem(MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY)
            .build();

    public static final HsHostingAssetRbacEntity MANAGED_WEBSPACE_HOSTING_ASSET_RBAC_TEST_ENTITY = HsHostingAssetRbacEntity.builder()
            .type(HsHostingAssetType.MANAGED_WEBSPACE)
            .identifier("xyz00")
            .caption("some managed webspace")
            .bookingItem(MANAGED_WEBSPACE_BOOKING_ITEM_REAL_ENTITY)
            .build();

    public static final HsHostingAssetRealEntity MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.MANAGED_WEBSPACE)
            .identifier("xyz00")
            .caption("some managed webspace")
            .bookingItem(MANAGED_WEBSPACE_BOOKING_ITEM_REAL_ENTITY)
            .build();

}
