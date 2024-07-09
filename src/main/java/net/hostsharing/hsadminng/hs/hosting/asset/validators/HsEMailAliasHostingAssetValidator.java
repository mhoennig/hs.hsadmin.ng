package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.validation.ArrayProperty.arrayOf;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsEMailAliasHostingAssetValidator extends HsHostingAssetEntityValidator {

    private static final String UNIX_USER_REGEX = "^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9]+)?$"; // also accepts legacy pac-names
    private static final String EMAIL_ADDRESS_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"; // RFC 5322
    public static final int EMAIL_ADDRESS_MAX_LENGTH = 320; // according to RFC 5321 and RFC 5322

    HsEMailAliasHostingAssetValidator() {
        super(  HsHostingAssetType.EMAIL_ALIAS,
                AlarmContact.isOptional(),

                arrayOf(
                        stringProperty("target").maxLength(EMAIL_ADDRESS_MAX_LENGTH).matchesRegEx(UNIX_USER_REGEX, EMAIL_ADDRESS_REGEX)
                ).required().minLength(1));
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile("^"+webspaceIdentifier+"$|^"+webspaceIdentifier+"-[a-z0-9]+$");
    }
}
