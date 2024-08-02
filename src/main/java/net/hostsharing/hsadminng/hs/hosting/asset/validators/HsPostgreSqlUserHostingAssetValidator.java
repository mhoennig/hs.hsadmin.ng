package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hash.HashGenerator;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.hs.validation.PasswordProperty.passwordProperty;

class HsPostgreSqlUserHostingAssetValidator extends HostingAssetEntityValidator {

    final static String HEAD_REGEXP = "^PGU\\|";

    public HsPostgreSqlUserHostingAssetValidator() {
        super(
                PGSQL_USER,
                AlarmContact.isOptional(),

                // TODO.impl: we need to be able to suppress updating of fields etc., something like this:
                // withFieldValidation(
                //      referenceProperty(alarmContact).isOptional(),
                //      referenceProperty(parentAsset).isWriteOnce(),
                //      referenceProperty(assignedToAsset).isWriteOnce(),
                // );

                passwordProperty("password").minLength(8).maxLength(40).hashedUsing(HashGenerator.Algorithm.SCRAM_SHA256).writeOnly());
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        final var webspaceIdentifier = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile(HEAD_REGEXP+webspaceIdentifier+"$|"+HEAD_REGEXP+webspaceIdentifier+"_[a-zA-Z0-9_]+$");
    }
}
