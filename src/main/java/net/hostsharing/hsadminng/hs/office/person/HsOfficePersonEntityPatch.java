package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;

import java.util.Optional;

class HsOfficePersonEntityPatch {

    private final HsOfficePersonEntity entity;

    HsOfficePersonEntityPatch(final HsOfficePersonEntity entity) {
        this.entity = entity;
    }

    void apply(final HsOfficePersonPatchResource resource) {
        Optional.ofNullable(resource.getPersonType())
                .map(HsOfficePersonTypeResource::getValue)
                .map(HsOfficePersonType::valueOf)
                .ifPresent(entity::setPersonType);
        OptionalFromJson.of(resource.getTradeName()).ifPresent(entity::setTradeName);
        OptionalFromJson.of(resource.getFamilyName()).ifPresent(entity::setFamilyName);
        OptionalFromJson.of(resource.getGivenName()).ifPresent(entity::setGivenName);
    }
}
