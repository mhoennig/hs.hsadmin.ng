package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;

import jakarta.persistence.EntityManager;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedWebspaceHostingAssetValidator extends HostingAssetEntityValidator {
    public HsManagedWebspaceHostingAssetValidator() {
        super(
                MANAGED_WEBSPACE,
                AlarmContact.isOptional(),
                integerProperty("groupid").readOnly()
        );
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        final var prefixPattern =
                !assetEntity.isLoaded()
                        ? assetEntity.getRelatedProject().getDebitor().getDefaultPrefix()
                        : "[a-z][a-z0-9][a-z0-9]";
        return Pattern.compile("^" + prefixPattern + "[0-9][0-9]$");
    }

    @Override
    public void postPersist(final EntityManager em, final HsHostingAsset webspaceAsset) {
        if (!webspaceAsset.isLoaded()) {
            final var unixUserAsset = HsHostingAssetRealEntity.builder()
                    .type(UNIX_USER)
                    .parentAsset(em.find(HsHostingAssetRealEntity.class, webspaceAsset.getUuid()))
                    .identifier(webspaceAsset.getIdentifier())
                    .caption(webspaceAsset.getIdentifier() + " webspace user")
                    .build();
            webspaceAsset.getSubHostingAssets().add(unixUserAsset);
            new HostingAssetEntitySaveProcessor(em, unixUserAsset)
                    .preprocessEntity()
                    .validateEntity()
                    .prepareForSave()
                    .save()
                    .validateContext();
            webspaceAsset.getConfig().put("groupid", unixUserAsset.getConfig().get("userid"));
        }
    }
}
