package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import java.util.List;


class HsManagedWebspaceHostingAssetValidator extends HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> {
    public HsManagedWebspaceHostingAssetValidator() {
    }

    @Override
    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        final var result = super.validate(assetEntity);
        validateIdentifierPattern(result, assetEntity);

        return result;
    }

    private static void validateIdentifierPattern(final List<String> result, final HsHostingAssetEntity assetEntity) {
        final var expectedIdentifierPattern = "^" + assetEntity.getParentAsset().getBookingItem().getProject().getDebitor().getDefaultPrefix() + "[0-9][0-9]$";
        if ( !assetEntity.getIdentifier().matches(expectedIdentifierPattern)) {
            result.add("'identifier' expected to match '"+expectedIdentifierPattern+"', but is '" + assetEntity.getIdentifier() + "'");
        }
    }
}
