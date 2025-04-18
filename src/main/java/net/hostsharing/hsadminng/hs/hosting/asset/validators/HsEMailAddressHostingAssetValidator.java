package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.validation.ArrayProperty.arrayOf;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsEMailAddressHostingAssetValidator extends HostingAssetEntityValidator {

    private static final String TARGET_MAILBOX_REGEX = "^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\.+_-]*)?$"; // also accepts legacy pac-names
    private static final String EMAIL_ADDRESS_LOCAL_PART_REGEX = "[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+"; // RFC 5322
    private static final String EMAIL_ADDRESS_DOMAIN_PART_REGEX = "[a-zA-Z0-9.-]+";
    private static final String EMAIL_ADDRESS_FULL_REGEX = "^(" + EMAIL_ADDRESS_LOCAL_PART_REGEX + ")?@" + EMAIL_ADDRESS_DOMAIN_PART_REGEX + "$";
    private static final String NOBODY_REGEX = "^nobody$";
    private static final String DEVNULL_REGEX = "^/dev/null$";
    public static final int EMAIL_ADDRESS_MAX_LENGTH = 320; // according to RFC 5321 and RFC 5322

    HsEMailAddressHostingAssetValidator() {
        super(  HsHostingAssetType.EMAIL_ADDRESS,
                AlarmContact.isOptional(),

                stringProperty("local-part").matchesRegEx("^" + EMAIL_ADDRESS_LOCAL_PART_REGEX + "$").writeOnce().optional(),
                stringProperty("sub-domain").matchesRegEx("^" + EMAIL_ADDRESS_LOCAL_PART_REGEX + "$").writeOnce().optional(),
                arrayOf(
                        stringProperty("target").maxLength(EMAIL_ADDRESS_MAX_LENGTH).matchesRegEx(TARGET_MAILBOX_REGEX, EMAIL_ADDRESS_FULL_REGEX, NOBODY_REGEX, DEVNULL_REGEX)
                ).required().minLength(1));
    }

    @Override
    public void preprocessEntity(final HsHostingAsset entity) {
        super.preprocessEntity(entity);
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            entity.setIdentifier(combineIdentifier(entity));
        }
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return Pattern.compile("^"+ Pattern.quote(combineIdentifier(assetEntity)) + "$");
    }

    private static String combineIdentifier(final HsHostingAsset emailAddressAssetEntity) {
        return ofNullable(emailAddressAssetEntity.getDirectValue("local-part", String.class)).orElse("")
                + "@"
                + ofNullable(emailAddressAssetEntity.getDirectValue("sub-domain", String.class)).map(s -> s + ".").orElse("")
                + emailAddressAssetEntity.getParentAsset().getParentAsset().getIdentifier();
    }
}
