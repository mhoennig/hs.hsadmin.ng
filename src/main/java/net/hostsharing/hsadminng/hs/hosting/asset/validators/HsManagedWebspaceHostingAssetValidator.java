package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;

class HsManagedWebspaceHostingAssetValidator extends HsHostingAssetEntityValidator {
    public HsManagedWebspaceHostingAssetValidator() {
        super(
                MANAGED_WEBSPACE,
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
