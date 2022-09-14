package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

class HsOfficePartnerEntityPatch {

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

    void apply(final HsOfficePartnerPatchResource resource) {
        OptionalFromJson.of(resource.getContactUuid()).ifPresent(newValue -> {
            entity.setContact(fetchContact.apply(newValue).orElseThrow(
                    () -> new NoSuchElementException("cannot find contact uuid " + newValue)
            ));
        });
        OptionalFromJson.of(resource.getPersonUuid()).ifPresent(newValue -> {
            entity.setPerson(fetchPerson.apply(newValue).orElseThrow(
                    () -> new NoSuchElementException("cannot find person uuid " + newValue)
            ));
        });
        OptionalFromJson.of(resource.getRegistrationOffice()).ifPresent(entity::setRegistrationOffice);
        OptionalFromJson.of(resource.getRegistrationNumber()).ifPresent(entity::setRegistrationNumber);
        OptionalFromJson.of(resource.getBirthday()).ifPresent(entity::setBirthday);
        OptionalFromJson.of(resource.getBirthName()).ifPresent(entity::setBirthName);
        OptionalFromJson.of(resource.getDateOfDeath()).ifPresent(entity::setDateOfDeath);
    }
}
