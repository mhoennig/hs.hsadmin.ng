package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

class HsCloudServerHostingAssetValidator extends HsHostingAssetEntityValidator {

    HsCloudServerHostingAssetValidator() {
        super(
                BookingItem.mustBeOfType(HsBookingItemType.CLOUD_SERVER),
                ParentAsset.mustBeNull(),
                AssignedToAsset.mustBeNull(),
                AlarmContact.isOptional(),
                NO_EXTRA_PROPERTIES);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return Pattern.compile("^vm[0-9][0-9][0-9][0-9]$");
    }
}
