package net.hostsharing.hsadminng.hs.office.sepamandate;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandatePatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

public class HsOfficeSepaMandateEntityPatcher implements EntityPatcher<HsOfficeSepaMandatePatchResource> {

    private final HsOfficeSepaMandateEntity entity;

    public HsOfficeSepaMandateEntityPatcher(final HsOfficeSepaMandateEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeSepaMandatePatchResource resource) {
        OptionalFromJson.of(resource.getReference()).ifPresent(
                entity::setReference);
        OptionalFromJson.of(resource.getAgreement()).ifPresent(
                entity::setAgreement);
        OptionalFromJson.of(resource.getValidFrom()).ifPresent(
                entity::setValidFrom);
        OptionalFromJson.of(resource.getValidTo()).ifPresent(
                entity::setValidTo);
    }
}
