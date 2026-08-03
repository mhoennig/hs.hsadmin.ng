package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationContactPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.NoSuchElementException;
import java.util.UUID;

public class HsOfficeRelationEntityContactPatcher implements EntityPatcher<HsOfficeRelationContactPatchResource> {

    private final HsOfficeContactRbacRepository rbacContactRepo;
    private final HsOfficeRelation entity;

    public HsOfficeRelationEntityContactPatcher(
            final HsOfficeContactRbacRepository rbacContactRepo,
            final HsOfficeRelation entity) {
        this.rbacContactRepo = rbacContactRepo;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficeRelationContactPatchResource resource) {
        OptionalFromJson.of(resource.getContactUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "contact");
            entity.setContact(rbacContactRepo.findVisibleByUuid(newValue).orElseThrow(
                    () -> new NoSuchElementException("cannot find Contact by contact.uuid: " + newValue)
            ));
        });
    }

    private void verifyNotNull(final UUID newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
