package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hash.HashGenerator;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;

import jakarta.persistence.EntityManager;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static net.hostsharing.hsadminng.hs.validation.PasswordProperty.passwordProperty;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsUnixUserHostingAssetValidator extends HostingAssetEntityValidator {

    private static final int DASH_LENGTH = "-".length();

    HsUnixUserHostingAssetValidator() {
        super(
                HsHostingAssetType.UNIX_USER,
                AlarmContact.isOptional(),

                booleanProperty("locked").readOnly(),
                integerProperty("userid").readOnly().initializedBy(HsUnixUserHostingAssetValidator::computeUserId),

                integerProperty("SSD hard quota").unit("MB").maxFrom("SSD").withFactor(1024).optional(),
                integerProperty("SSD soft quota").unit("MB").maxFrom("SSD hard quota").optional(),
                integerProperty("HDD hard quota").unit("MB").maxFrom("HDD").withFactor(1024).optional(),
                integerProperty("HDD soft quota").unit("MB").maxFrom("HDD hard quota").optional(),
                stringProperty("shell")
                        // TODO.spec: do we want to change them all to /usr/bin/, also in import?
                        .provided("/bin/false", "/bin/bash", "/bin/csh", "/bin/dash", "/usr/bin/tcsh", "/usr/bin/zsh", "/usr/bin/passwd")
                        .withDefault("/bin/false"),
                stringProperty("homedir").readOnly().renderedBy(HsUnixUserHostingAssetValidator::computeHomedir),
                stringProperty("totpKey").matchesRegEx("^0x([0-9A-Fa-f]{2})+$").minLength(20).maxLength(256).undisclosed().writeOnly().optional(),
                passwordProperty("password").minLength(8).maxLength(40).hashedUsing(HashGenerator.Algorithm.LINUX_SHA512).writeOnly());
                // TODO.spec: public SSH keys? (only if hsadmin-ng is only accessible with 2FA)
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile("^"+webspaceIdentifier+"$|^"+webspaceIdentifier+"-[a-z0-9\\._-]+$");
    }

    private static String computeHomedir(final EntityManager em, final PropertiesProvider propertiesProvider) {
        final var entity = (HsHostingAsset) propertiesProvider;
        final var webspaceName = entity.getParentAsset().getIdentifier();
        return "/home/pacs/" + webspaceName
                + "/users/" + entity.getIdentifier().substring(webspaceName.length()+DASH_LENGTH);
    }

    private static Integer computeUserId(final EntityManager em, final PropertiesProvider propertiesProvider) {
        final Object result = em.createNativeQuery("SELECT nextval('hs_hosting.asset_unixuser_system_id_seq')", Integer.class)
                .getSingleResult();
        return (Integer) result;
    }
}
