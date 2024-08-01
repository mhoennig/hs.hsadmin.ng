package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;

class HsManagedWebspaceHostingAssetValidator extends HostingAssetEntityValidator {
    public HsManagedWebspaceHostingAssetValidator() {
        super(
                MANAGED_WEBSPACE,
                AlarmContact.isOptional(),
                NO_EXTRA_PROPERTIES); // TODO.impl: groupid missing, should be equal to main user
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        final var prefixPattern =
                !assetEntity.isLoaded()
                        ? assetEntity.getRelatedProject().getDebitor().getDefaultPrefix()
                        : "[a-z][a-z0-9][a-z0-9]";
        return Pattern.compile("^" + prefixPattern + "[0-9][0-9]$");
    }
}
