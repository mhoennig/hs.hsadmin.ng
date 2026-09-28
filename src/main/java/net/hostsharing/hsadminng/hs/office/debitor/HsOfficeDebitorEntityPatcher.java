package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacRepository;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.NoSuchElementException;
import java.util.Optional;

class HsOfficeDebitorEntityPatcher implements EntityPatcher<HsOfficeDebitorPatchResource> {

    private final HsOfficeRelationRbacRepository rbacRelationRepo;
    private final HsOfficeDebitorEntity entity;

    HsOfficeDebitorEntityPatcher(
            final HsOfficeRelationRbacRepository rbacRelationRepo,
            final HsOfficeDebitorEntity entity) {
        this.rbacRelationRepo = rbacRelationRepo;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeDebitorPatchResource resource) {
        OptionalFromJson.of(resource.getDebitorRelUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "debitorRel");
            entity.setDebitorRel(rbacRelationRepo.findVisibleByUuid(newValue).orElseThrow(
                    () -> new NoSuchElementException("cannot find Relation by debitorRel.uuid: " + newValue)));
        });
        Optional.ofNullable(resource.getBillable()).ifPresent(entity::setBillable);
        OptionalFromJson.of(resource.getVatId()).ifPresent(entity::setVatId);
        OptionalFromJson.of(resource.getVatCountryCode()).ifPresent(entity::setVatCountryCode);
        Optional.ofNullable(resource.getVatBusiness()).ifPresent(entity::setVatBusiness);
        Optional.ofNullable(resource.getVatReverseCharge()).ifPresent(entity::setVatReverseCharge);
        OptionalFromJson.of(resource.getDefaultPrefix()).ifPresent(newValue -> {
            verifyNotNull(newValue, "defaultPrefix");
            entity.setDefaultPrefix(newValue);
        });
    }

    private void verifyNotNull(final Object newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
