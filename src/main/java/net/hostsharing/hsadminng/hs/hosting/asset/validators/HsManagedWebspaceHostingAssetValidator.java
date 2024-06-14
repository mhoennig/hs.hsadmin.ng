package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class HsManagedWebspaceHostingAssetValidator extends HsHostingAssetEntityValidator {
    public HsManagedWebspaceHostingAssetValidator() {
    }

    @Override
    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        return Stream.of(validateIdentifierPattern(assetEntity), super.validate(assetEntity))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<String> validateIdentifierPattern(final HsHostingAssetEntity assetEntity) {
        final var expectedIdentifierPattern = "^" + assetEntity.getParentAsset().getBookingItem().getProject().getDebitor().getDefaultPrefix() + "[0-9][0-9]$";
        if ( !assetEntity.getIdentifier().matches(expectedIdentifierPattern)) {
            return List.of("'identifier' expected to match '"+expectedIdentifierPattern+"', but is '" + assetEntity.getIdentifier() + "'");
        }
        return Collections.emptyList();
    }
}
