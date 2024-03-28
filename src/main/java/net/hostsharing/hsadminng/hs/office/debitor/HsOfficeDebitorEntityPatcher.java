package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;
import java.util.Optional;

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
        OptionalFromJson.of(resource.getDebitorRelUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "debitorRel");
            entity.setDebitorRel(em.getReference(HsOfficeRelationEntity.class, newValue));
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
        OptionalFromJson.of(resource.getRefundBankAccountUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "refundBankAccount");
            entity.setRefundBankAccount(em.getReference(HsOfficeBankAccountEntity.class, newValue));
        });
    }

    private void verifyNotNull(final Object newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
