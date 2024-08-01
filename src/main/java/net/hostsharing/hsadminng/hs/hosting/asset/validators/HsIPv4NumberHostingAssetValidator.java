package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV4_NUMBER;

class HsIPv4NumberHostingAssetValidator extends HostingAssetEntityValidator {

    private static final Pattern IPV4_REGEX = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    HsIPv4NumberHostingAssetValidator() {
        super(
                IPV4_NUMBER,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES
        );
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return IPV4_REGEX;
    }
}
