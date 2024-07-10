package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hash.LinuxEtcShadowHashGenerator;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static net.hostsharing.hsadminng.hs.validation.PasswordProperty.passwordProperty;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsUnixUserHostingAssetValidator extends HostingAssetEntityValidator {

    private static final int DASH_LENGTH = "-".length();

    HsUnixUserHostingAssetValidator() {
        super(
                HsHostingAssetType.UNIX_USER,
                AlarmContact.isOptional(),

                integerProperty("SSD hard quota").unit("GB").maxFrom("SSD").optional(),
                integerProperty("SSD soft quota").unit("GB").maxFrom("SSD hard quota").optional(),
                integerProperty("HDD hard quota").unit("GB").maxFrom("HDD").optional(),
                integerProperty("HDD soft quota").unit("GB").maxFrom("HDD hard quota").optional(),
                enumerationProperty("shell")
                        .values("/bin/false", "/bin/bash", "/bin/csh", "/bin/dash", "/usr/bin/tcsh", "/usr/bin/zsh", "/usr/bin/passwd")
                        .withDefault("/bin/false"),
                stringProperty("homedir").readOnly().computedBy(HsUnixUserHostingAssetValidator::computeHomedir),
                stringProperty("totpKey").matchesRegEx("^0x([0-9A-Fa-f]{2})+$").minLength(20).maxLength(256).undisclosed().writeOnly().optional(),
                passwordProperty("password").minLength(8).maxLength(40).hashedUsing(LinuxEtcShadowHashGenerator.Algorithm.SHA512).writeOnly());
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile("^"+webspaceIdentifier+"$|^"+webspaceIdentifier+"-[a-z0-9]+$");
    }

    private static String computeHomedir(final PropertiesProvider propertiesProvider) {
        final var entity = (HsHostingAssetEntity) propertiesProvider;
        final var webspaceName = entity.getParentAsset().getIdentifier();
        return "/home/pacs/" + webspaceName
                + "/users/" + entity.getIdentifier().substring(webspaceName.length()+DASH_LENGTH);
    }
}
