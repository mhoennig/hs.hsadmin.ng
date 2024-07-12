package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_INSTANCE;

class HsMariaDbInstanceHostingAssetValidator extends HostingAssetEntityValidator {

    final static String DEFAULT_INSTANCE_IDENTIFIER_SUFFIX = "|MariaDB.default"; // TODO.spec: specify instance naming

    public HsMariaDbInstanceHostingAssetValidator() {
        super(
                MARIADB_INSTANCE,
                AlarmContact.isOptional(), // hostmaster alert address is implicitly added
                NO_EXTRA_PROPERTIES); // TODO.spec: specify instance properties, e.g. installed extensions
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return Pattern.compile(
                "^" + Pattern.quote(assetEntity.getParentAsset().getIdentifier()
                        + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX)
                        + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAssetEntity entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(
                    pa.getIdentifier() + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX));
        }
    }
}
