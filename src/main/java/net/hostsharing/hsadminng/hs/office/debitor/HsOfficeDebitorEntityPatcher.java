package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;

class HsOfficeDebitorEntityPatcher implements EntityPatcher<HsOfficeDebitorPatchResource> {

    private final EntityManager em;
    private final HsOfficeDebitorEntity entity;

    HsOfficeDebitorEntityPatcher(
            final EntityManager em,
            final HsOfficeDebitorEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeDebitorPatchResource resource) {
        OptionalFromJson.of(resource.getBillingContactUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "billingContact");
            entity.setBillingContact(em.getReference(HsOfficeContactEntity.class, newValue));
        });
        OptionalFromJson.of(resource.getVatId()).ifPresent(entity::setVatId);
        OptionalFromJson.of(resource.getVatCountryCode()).ifPresent(entity::setVatCountryCode);
        OptionalFromJson.of(resource.getVatBusiness()).ifPresent(newValue -> {
            verifyNotNull(newValue, "vatBusiness");
            entity.setVatBusiness(newValue);
        });
    }

    private void verifyNotNull(final Object newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
