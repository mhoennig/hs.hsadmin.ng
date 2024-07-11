package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;

class HsDomainSmtpSetupHostingAssetValidator extends HostingAssetEntityValidator {

    public static final String IDENTIFIER_SUFFIX = "|SMTP";

    HsDomainSmtpSetupHostingAssetValidator() {
        super(
                DOMAIN_SMTP_SETUP,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return  Pattern.compile("^" + Pattern.quote(assetEntity.getParentAsset().getIdentifier() + IDENTIFIER_SUFFIX) + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAssetEntity entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(pa.getIdentifier() + IDENTIFIER_SUFFIX));
        }
    }
}
