package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.EntityPatcher;
import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import javax.persistence.EntityManager;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;


class HsOfficePartnerEntityPatcher implements EntityPatcher<HsOfficePartnerPatchResource> {
    private final EntityManager em;
    private final HsOfficePartnerEntity entity;
    HsOfficePartnerEntityPatcher(
            final EntityManager em,
            final HsOfficePartnerEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final HsOfficePartnerPatchResource resource) {
        OptionalFromJson.of(resource.getContactUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "contact");
            entity.setContact(em.getReference(HsOfficeContactEntity.class, newValue));
        });
        OptionalFromJson.of(resource.getPersonUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "person");
            entity.setPerson(em.getReference(HsOfficePersonEntity.class, newValue));
        });

        new HsOfficePartnerDetailsEntityPatcher(em, entity.getDetails()).apply(resource.getDetails());
    }

    private void verifyNotNull(final UUID newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
