package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.mapper.StrictMapper;

import java.util.Optional;

public class HsOfficeMembershipEntityPatcher implements EntityPatcher<HsOfficeMembershipPatchResource> {

    private final StrictMapper mapper;
    private final HsOfficeMembershipEntity entity;

    public HsOfficeMembershipEntityPatcher(
            final StrictMapper mapper,
            final HsOfficeMembershipEntity entity) {
        this.mapper = mapper;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeMembershipPatchResource resource) {
        OptionalFromJson.of(resource.getValidTo()).ifPresent(
                entity::setValidTo);
        Optional.ofNullable(resource.getStatus())
                .map(v -> mapper.map(v, HsOfficeMembershipStatus.class))
                .ifPresent(entity::setStatus);
        OptionalFromJson.of(resource.getMembershipFeeBillable()).ifPresent(
                entity::setMembershipFeeBillable);
    }
}
