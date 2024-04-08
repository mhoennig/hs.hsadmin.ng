package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;

import java.util.Optional;

class HsOfficePersonEntityPatcher implements EntityPatcher<HsOfficePersonPatchResource> {

    private final HsOfficePersonEntity entity;

    HsOfficePersonEntityPatcher(final HsOfficePersonEntity entity) {
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficePersonPatchResource resource) {
        Optional.ofNullable(resource.getPersonType())
                .map(HsOfficePersonTypeResource::getValue)
                .map(HsOfficePersonType::valueOf)
                .ifPresent(entity::setPersonType);
        OptionalFromJson.of(resource.getTradeName()).ifPresent(entity::setTradeName);
        OptionalFromJson.of(resource.getSalutation()).ifPresent(entity::setSalutation);
        OptionalFromJson.of(resource.getTitle()).ifPresent(entity::setTitle);
        OptionalFromJson.of(resource.getFamilyName()).ifPresent(entity::setFamilyName);
        OptionalFromJson.of(resource.getGivenName()).ifPresent(entity::setGivenName);
    }
}
