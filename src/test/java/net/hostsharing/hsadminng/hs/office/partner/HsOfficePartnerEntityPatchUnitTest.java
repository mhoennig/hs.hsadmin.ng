package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.PatchUnitTestBase;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class HsOfficePartnerEntityPatchUnitTest extends PatchUnitTestBase<
        HsOfficePartnerPatchResource,
        HsOfficePartnerEntity
        > {

    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();
    private static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();
    private static final UUID PATCHED_PERSON_UUID = UUID.randomUUID();

    private static final LocalDate INITIAL_BIRTHDAY = LocalDate.parse("1900-01-01");
    private static final LocalDate PATCHED_BIRTHDAY = LocalDate.parse("1990-12-31");

    private static final LocalDate INITIAL_DAY_OF_DEATH = LocalDate.parse("2000-01-01");
    private static final LocalDate PATCHED_DATE_OF_DEATH = LocalDate.parse("2022-08-31");

    private final HsOfficePersonEntity givenInitialPerson = HsOfficePersonEntity.builder()
            .uuid(INITIAL_PERSON_UUID)
            .build();
    private final HsOfficeContactEntity givenInitialContact = HsOfficeContactEntity.builder()
            .uuid(INITIAL_CONTACT_UUID)
            .build();

    @Override
    protected HsOfficePartnerEntity newInitialEntity() {
        final var entity = new HsOfficePartnerEntity();
        entity.setUuid(INITIAL_PARTNER_UUID);
        entity.setPerson(givenInitialPerson);
        entity.setContact(givenInitialContact);
        entity.setRegistrationOffice("initial Reg-Office");
        entity.setRegistrationNumber("initial Reg-Number");
        entity.setBirthday(INITIAL_BIRTHDAY);
        entity.setBirthName("initial birth name");
        entity.setDateOfDeath(INITIAL_DAY_OF_DEATH);
        return entity;
    }

    @Override
    protected HsOfficePartnerPatchResource newPatchResource() {
        return new HsOfficePartnerPatchResource();
    }

    @Override
    protected HsOfficePartnerEntityPatch createPatcher(final HsOfficePartnerEntity partner) {
        return new HsOfficePartnerEntityPatch(
                partner,
                uuid -> uuid == PATCHED_CONTACT_UUID
                        ? Optional.of(newContact(uuid))
                        : Optional.empty(),
                uuid -> uuid == PATCHED_PERSON_UUID
                        ? Optional.of(newPerson(uuid))
                        : Optional.empty());
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "contact",
                        HsOfficePartnerPatchResource::setContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficePartnerEntity::setContact,
                        newContact(PATCHED_CONTACT_UUID))
                        .notNullable()
                        .resolvesUuid(),
                new JsonNullableProperty<>(
                        "person",
                        HsOfficePartnerPatchResource::setPersonUuid,
                        PATCHED_PERSON_UUID,
                        HsOfficePartnerEntity::setPerson,
                        newPerson(PATCHED_PERSON_UUID))
                        .notNullable()
                        .resolvesUuid(),
                new JsonNullableProperty<>(
                        "registrationOffice",
                        HsOfficePartnerPatchResource::setRegistrationOffice,
                        "patched Reg-Office",
                        HsOfficePartnerEntity::setRegistrationOffice),
                new JsonNullableProperty<>(
                        "birthday",
                        HsOfficePartnerPatchResource::setBirthday,
                        PATCHED_BIRTHDAY,
                        HsOfficePartnerEntity::setBirthday),
                new JsonNullableProperty<>(
                        "dayOfDeath",
                        HsOfficePartnerPatchResource::setDateOfDeath,
                        PATCHED_DATE_OF_DEATH,
                        HsOfficePartnerEntity::setDateOfDeath)
        );
    }

    private static HsOfficeContactEntity newContact(final UUID uuid) {
        final var newContact = new HsOfficeContactEntity();
        newContact.setUuid(uuid);
        return newContact;
    }

    private HsOfficePersonEntity newPerson(final UUID uuid) {
        final var newPerson = new HsOfficePersonEntity();
        newPerson.setUuid(uuid);
        return newPerson;
    }
}
