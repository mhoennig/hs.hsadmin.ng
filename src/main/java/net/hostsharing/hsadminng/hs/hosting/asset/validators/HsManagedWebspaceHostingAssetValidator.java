package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import java.util.List;

import static net.hostsharing.hsadminng.hs.validation.IntegerPropertyValidator.integerProperty;

class HsManagedWebspaceHostingAssetValidator extends HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> {
    public HsManagedWebspaceHostingAssetValidator() {
        super(
            integerProperty("SSD").unit("GB").min(1).max(100).step(1).required(),
            integerProperty("HDD").unit("GB").min(0).max(250).step(10).optional(),
            integerProperty("Traffic").unit("GB").min(10).max(1000).step(10).required()
        );
    }

    @Override
    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        final var result = super.validate(assetEntity);
        validateIdentifierPattern(result, assetEntity);

        return result;
    }

    private static void validateIdentifierPattern(final List<String> result, final HsHostingAssetEntity assetEntity) {
        final var expectedIdentifierPattern = "^" + assetEntity.getParentAsset().getBookingItem().getDebitor().getDefaultPrefix() + "[0-9][0-9]$";
        if ( !assetEntity.getIdentifier().matches(expectedIdentifierPattern)) {
            result.add("'identifier' expected to match '"+expectedIdentifierPattern+"', but is '" + assetEntity.getIdentifier() + "'");
        }
    }
}
