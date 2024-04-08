package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.Optional;

public class HsOfficeMembershipEntityPatcher implements EntityPatcher<HsOfficeMembershipPatchResource> {

    private final Mapper mapper;
    private final HsOfficeMembershipEntity entity;

    public HsOfficeMembershipEntityPatcher(
            final Mapper mapper,
            final HsOfficeMembershipEntity entity) {
        this.mapper = mapper;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeMembershipPatchResource resource) {
        OptionalFromJson.of(resource.getValidTo()).ifPresent(
                entity::setValidTo);
        Optional.ofNullable(resource.getReasonForTermination())
                .map(v -> mapper.map(v, HsOfficeReasonForTermination.class))
                .ifPresent(entity::setReasonForTermination);
        OptionalFromJson.of(resource.getMembershipFeeBillable()).ifPresent(
                entity::setMembershipFeeBillable);
    }
}
