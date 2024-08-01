package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;

class HsCloudServerHostingAssetValidator extends HostingAssetEntityValidator {

    HsCloudServerHostingAssetValidator() {
        super(
                CLOUD_SERVER,
                AlarmContact.isOptional(),
                NO_EXTRA_PROPERTIES);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return Pattern.compile("^vm[0-9][0-9][0-9][0-9]$");
    }
}
