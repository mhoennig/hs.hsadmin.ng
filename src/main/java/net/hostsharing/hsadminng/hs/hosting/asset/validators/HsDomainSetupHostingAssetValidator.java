package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

class HsDomainSetupHostingAssetValidator extends HsHostingAssetEntityValidator {

    public static final String DOMAIN_NAME_REGEX = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";

    private final Pattern identifierPattern;

    HsDomainSetupHostingAssetValidator() {
        super(  BookingItem.mustBeNull(),
                ParentAsset.mustBeNull(),
                AssignedToAsset.mustBeNull(),
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES);
        this.identifierPattern = Pattern.compile(DOMAIN_NAME_REGEX);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return identifierPattern;
    }
}
