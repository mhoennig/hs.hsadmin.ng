package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;

class HsPostgreSqlDbInstanceHostingAssetValidator extends HostingAssetEntityValidator {

    final static String DEFAULT_INSTANCE_IDENTIFIER_SUFFIX = "|PgSql.default"; // TODO.spec: specify instance naming

    public HsPostgreSqlDbInstanceHostingAssetValidator() {
        super(
                PGSQL_DATABASE,
                AlarmContact.isOptional(),

                // TODO.spec: PostgreSQL extensions in database and here? also decide which. Free selection or booleans/checkboxes?
                NO_EXTRA_PROPERTIES); // TODO.spec: specify instance properties, e.g. installed extensions
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return Pattern.compile(
                "^" + Pattern.quote(assetEntity.getParentAsset().getIdentifier()
                        + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX)
                        + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAsset entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(
                    pa.getIdentifier() + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX));
        }
    }
}
