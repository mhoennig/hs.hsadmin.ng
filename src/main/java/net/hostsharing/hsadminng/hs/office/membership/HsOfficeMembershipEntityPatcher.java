package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.Mapper.map;

public class HsOfficeMembershipEntityPatcher implements EntityPatcher<HsOfficeMembershipPatchResource> {

    private final EntityManager em;
    private final HsOfficeMembershipEntity entity;

    public HsOfficeMembershipEntityPatcher(
            final EntityManager em,
            final HsOfficeMembershipEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeMembershipPatchResource resource) {
        OptionalFromJson.of(resource.getMainDebitorUuid())
                .ifPresent(newValue -> {
                    verifyNotNull(newValue, "debitor");
                    entity.setMainDebitor(em.getReference(HsOfficeDebitorEntity.class, newValue));
                });
        OptionalFromJson.of(resource.getValidTo()).ifPresent(
                entity::setValidTo);
        Optional.ofNullable(resource.getReasonForTermination())
                .map(v -> map(v, HsOfficeReasonForTermination.class))
                .ifPresent(entity::setReasonForTermination);
    }

    private void verifyNotNull(final UUID newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
