package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.EntityPatch;
import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

class HsOfficePartnerEntityPatch implements EntityPatch<HsOfficePartnerPatchResource> {

    private final HsOfficePartnerEntity entity;
    private final Function<UUID, Optional<HsOfficeContactEntity>> fetchContact;
    private final Function<UUID, Optional<HsOfficePersonEntity>> fetchPerson;

    HsOfficePartnerEntityPatch(
            final HsOfficePartnerEntity entity,
            final Function<UUID, Optional<HsOfficeContactEntity>> fetchContact,
            final Function<UUID, Optional<HsOfficePersonEntity>> fetchPerson) {
        this.entity = entity;
        this.fetchContact = fetchContact;
        this.fetchPerson = fetchPerson;
    }

    @Override
    public void apply(final HsOfficePartnerPatchResource resource) {
        OptionalFromJson.of(resource.getContactUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "contact");
            entity.setContact(fetchContact.apply(newValue)
                    .orElseThrow(noSuchElementException("contact", newValue)));
        });
        OptionalFromJson.of(resource.getPersonUuid()).ifPresent(newValue -> {
            verifyNotNull(newValue, "person");
            entity.setPerson(fetchPerson.apply(newValue)
                    .orElseThrow(noSuchElementException("person", newValue)));
        });
        OptionalFromJson.of(resource.getRegistrationOffice()).ifPresent(entity::setRegistrationOffice);
        OptionalFromJson.of(resource.getRegistrationNumber()).ifPresent(entity::setRegistrationNumber);
        OptionalFromJson.of(resource.getBirthday()).ifPresent(entity::setBirthday);
        OptionalFromJson.of(resource.getBirthName()).ifPresent(entity::setBirthName);
        OptionalFromJson.of(resource.getDateOfDeath()).ifPresent(entity::setDateOfDeath);
    }

    private Supplier<RuntimeException> noSuchElementException(final String propertyName, final UUID newValue) {
        return () -> new NoSuchElementException("cannot find '" + propertyName + "' uuid " + newValue);
    }

    private void verifyNotNull(final UUID newValue, final String propertyName) {
        if (newValue == null) {
            throw new IllegalArgumentException("property '" + propertyName + "' must not be null");
        }
    }
}
