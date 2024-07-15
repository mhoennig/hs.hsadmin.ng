package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsPostgreSqlDatabaseHostingAssetValidator extends HostingAssetEntityValidator {

    public HsPostgreSqlDatabaseHostingAssetValidator() {
        super(
                PGSQL_DATABASE,
                AlarmContact.isOptional(),

                stringProperty("encoding").matchesRegEx("[A-Z0-9_]+").maxLength(24).provided("LATIN1", "UTF8").withDefault("UTF8")

                // TODO.spec: PostgreSQL extensions in instance and here? also decide which. Free selection or booleans/checkboxes?
        );
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getParentAsset().getIdentifier();
        return Pattern.compile("^"+webspaceIdentifier+"$|^"+webspaceIdentifier+"_[a-z0-9_]+$");
    }
}
