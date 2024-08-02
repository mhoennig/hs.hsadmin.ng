package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_DATABASE;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsMariaDbDatabaseHostingAssetValidator extends HostingAssetEntityValidator {

    final static String HEAD_REGEXP = "^MAD\\|";

    public HsMariaDbDatabaseHostingAssetValidator() {
        super(
                MARIADB_DATABASE,
                AlarmContact.isOptional(),

                stringProperty("encoding").matchesRegEx("[a-z0-9_]+").maxLength(24).provided("latin1", "utf8").withDefault("utf8"));
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getParentAsset().getIdentifier();
        return Pattern.compile(HEAD_REGEXP+webspaceIdentifier+"$|"+HEAD_REGEXP+webspaceIdentifier+"_[a-zA-Z0-9_]+$");
    }
}
