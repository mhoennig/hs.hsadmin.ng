package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.regex.Pattern;

class HsUnixUserHostingAssetValidator extends HsHostingAssetEntityValidator {

    HsUnixUserHostingAssetValidator() {
        super(BookingItem.mustBeNull(),
              ParentAsset.mustBeOfType(HsHostingAssetType.MANAGED_WEBSPACE),
              AssignedToAsset.mustBeNull(),
              AlarmContact.isOptional(), // TODO.spec: for quota notifications
              NO_EXTRA_PROPERTIES); // TODO.spec: yet to be specified
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile("^"+webspaceIdentifier+"$|^"+webspaceIdentifier+"-[a-z0-9]+$");
    }
}
