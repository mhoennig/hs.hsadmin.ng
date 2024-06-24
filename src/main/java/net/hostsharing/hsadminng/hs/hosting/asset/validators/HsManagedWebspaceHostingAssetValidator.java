package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.regex.Pattern;

class HsManagedWebspaceHostingAssetValidator extends HsHostingAssetEntityValidator {
    public HsManagedWebspaceHostingAssetValidator() {
        super(BookingItem.mustBeOfType(HsBookingItemType.MANAGED_WEBSPACE),
                ParentAsset.mustBeOfType(HsHostingAssetType.MANAGED_SERVER), // the (shared or private) ManagedServer
                AssignedToAsset.mustBeNull(),
                AlarmContact.isOptional(), // hostmaster alert address is implicitly added
                NO_EXTRA_PROPERTIES);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        final var prefixPattern =
                !assetEntity.isLoaded()
                        ? assetEntity.getParentAsset().getBookingItem().getProject().getDebitor().getDefaultPrefix()
                        : "[a-z][a-z0-9][a-z0-9]";
        return Pattern.compile("^" + prefixPattern + "[0-9][0-9]$");
    }
}
