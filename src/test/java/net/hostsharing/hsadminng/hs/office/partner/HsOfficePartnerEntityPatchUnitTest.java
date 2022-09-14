package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

// TODO: there must be an easier way to test such patch classes
class HsOfficePartnerEntityPatchUnitTest {

    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();
    private static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();
    private static final UUID PATCHED_PERSON_UUID = UUID.randomUUID();

    private static final LocalDate INITIAL_BIRTHDAY = LocalDate.parse("1900-01-01");
    private static final LocalDate PATCHED_BIRTHDAY = LocalDate.parse("1990-12-31");

    private static final LocalDate INITIAL_DAY_OF_DEATH = LocalDate.parse("2000-01-01");
    private static final LocalDate PATCHED_DAY_OF_DEATH = LocalDate.parse("2022-08-31");

    final HsOfficePartnerEntity givenPartner = new HsOfficePartnerEntity();
    private final HsOfficePersonEntity givenInitialPerson = new HsOfficePersonEntity();
    private final HsOfficeContactEntity givenInitialContact = new HsOfficeContactEntity();

    final HsOfficePartnerPatchResource patchResource = new HsOfficePartnerPatchResource();

    private final HsOfficePartnerEntityPatch hsOfficePartnerEntityPatch = new HsOfficePartnerEntityPatch(
            givenPartner,
            uuid -> uuid == PATCHED_CONTACT_UUID
                    ? Optional.of(newContact(uuid))
                    : Optional.empty(),
            uuid -> uuid == PATCHED_PERSON_UUID
                    ? Optional.of(newPerson(uuid))
                    : Optional.empty());

    {
        givenInitialPerson.setUuid(INITIAL_PERSON_UUID);
        givenInitialContact.setUuid(INITIAL_CONTACT_UUID);

        givenPartner.setUuid(INITIAL_PARTNER_UUID);
        givenPartner.setPerson(givenInitialPerson);
        givenPartner.setContact(givenInitialContact);
        givenPartner.setRegistrationOffice("initial Reg-Office");
        givenPartner.setRegistrationNumber("initial Reg-Number");
        givenPartner.setBirthday(INITIAL_BIRTHDAY);
        givenPartner.setBirthName("initial birth name");
        givenPartner.setDateOfDeath(INITIAL_DAY_OF_DEATH);
    }

    @Test
    void willPatchAllProperties() {
        // given
        patchResource.setContactUuid(JsonNullable.of(PATCHED_CONTACT_UUID));
        patchResource.setPersonUuid(JsonNullable.of(PATCHED_PERSON_UUID));
        patchResource.setRegistrationNumber(JsonNullable.of("patched Reg-Number"));
        patchResource.setRegistrationOffice(JsonNullable.of("patched Reg-Office"));
        patchResource.setBirthday(JsonNullable.of(PATCHED_BIRTHDAY));
        patchResource.setBirthName(JsonNullable.of("patched birth name"));
        patchResource.setDateOfDeath(JsonNullable.of(PATCHED_DAY_OF_DEATH));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedContactUuid(PATCHED_CONTACT_UUID)
                .withPatchedPersonUuid(PATCHED_PERSON_UUID)
                .withPatchedRegistrationOffice("patched Reg-Office")
                .withPatchedRegistrationNumber("patched Reg-Number")
                .withPatchedBirthday(PATCHED_BIRTHDAY)
                .withPatchedBirthName("patched birth name")
                .withPatchedDateOfDeath(PATCHED_DAY_OF_DEATH)
                .matches(givenPartner);
    }

    @Test
    void willThrowIfNoContactFound() {
        // given
        patchResource.setContactUuid(JsonNullable.of(null));

        // when
        final var exception = catchThrowableOfType(() -> {
            hsOfficePartnerEntityPatch.apply(patchResource);
        }, NoSuchElementException.class);

        // then
        assertThat(exception.getMessage()).isEqualTo("cannot find contact uuid null");
    }

    @Test
    void willThrowIfNoPersonFound() {
        // given
        patchResource.setPersonUuid(JsonNullable.of(null));

        // when
        final var exception = catchThrowableOfType(() -> {
            hsOfficePartnerEntityPatch.apply(patchResource);
        }, NoSuchElementException.class);

        // then
        assertThat(exception.getMessage()).isEqualTo("cannot find person uuid null");
    }

    @Test
    void willPatchOnlyContactProperty() {
        // given
        patchResource.setContactUuid(JsonNullable.of(PATCHED_CONTACT_UUID));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedContactUuid(PATCHED_CONTACT_UUID)
                .matches(givenPartner);
    }

    @Test
    void willPatchOnlyPersonProperty() {
        // given
        patchResource.setPersonUuid(JsonNullable.of(PATCHED_PERSON_UUID));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedPersonUuid(PATCHED_PERSON_UUID)
                .matches(givenPartner);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched Reg-Office" })
    @NullSource
    void willPatchOnlyRegOfficeProperty(final String patchedValue) {
        // given
        patchResource.setRegistrationOffice(JsonNullable.of(patchedValue));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedRegistrationOffice(patchedValue)
                .matches(givenPartner);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched birth name" })
    @NullSource
    void willPatchOnlyRegNumberProperty(final String patchedValue) {
        // given
        patchResource.setRegistrationNumber(JsonNullable.of(patchedValue));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedRegistrationNumber(patchedValue)
                .matches(givenPartner);
    }

    @ParameterizedTest
    @EnumSource(LocalDatePatches.class)
    void willPatchOnlyBirthdayProperty(final LocalDatePatches patch) {
        // given
        patchResource.setBirthday(JsonNullable.of(patch.value));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedBirthday(patch.value)
                .matches(givenPartner);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched birth name" })
    @NullSource
    void willPatchOnlyBirthNameProperty(final String patchedValue) {
        // given
        patchResource.setBirthName(JsonNullable.of(patchedValue));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedBirthName(patchedValue)
                .matches(givenPartner);
    }

    @ParameterizedTest
    @EnumSource(LocalDatePatches.class)
    void willPatchOnlyDateOfDeathProperty(final LocalDatePatches patch) {
        // given
        patchResource.setDateOfDeath(JsonNullable.of(patch.value));

        // when
        hsOfficePartnerEntityPatch.apply(patchResource);

        // then
        new HsOfficePartnerEntityMatcher()
                .withPatchedDateOfDeath(patch.value)
                .matches(givenPartner);
    }

    private HsOfficeContactEntity newContact(final UUID uuid) {
        final var newContact = new HsOfficeContactEntity();
        newContact.setUuid(uuid);
        return newContact;
    }

    private HsOfficePersonEntity newPerson(final UUID uuid) {
        final var newPerson = new HsOfficePersonEntity();
        newPerson.setUuid(uuid);
        return newPerson;
    }

    private static class HsOfficePartnerEntityMatcher {

        private UUID expectedContactUuid = INITIAL_CONTACT_UUID;
        private UUID expectedPersonUuid = INITIAL_PERSON_UUID;
        private String expectedRegOffice = "initial Reg-Office";
        private String expectedRegNumber = "initial Reg-Number";
        private LocalDate expectedBirthday = INITIAL_BIRTHDAY;
        private String expectedBirthName = "initial birth name";
        private LocalDate expectedDateOfDeath = INITIAL_DAY_OF_DEATH;

        HsOfficePartnerEntityMatcher withPatchedContactUuid(final UUID patchedContactUuid) {
            expectedContactUuid = patchedContactUuid;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedPersonUuid(final UUID patchedPersonUuid) {
            expectedPersonUuid = patchedPersonUuid;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedRegistrationOffice(final String patchedRegOffice) {
            expectedRegOffice = patchedRegOffice;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedRegistrationNumber(final String patchedRegNumber) {
            expectedRegNumber = patchedRegNumber;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedBirthday(final LocalDate patchedBirthday) {
            expectedBirthday = patchedBirthday;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedBirthName(final String patchedBirthName) {
            expectedBirthName = patchedBirthName;
            return this;
        }

        HsOfficePartnerEntityMatcher withPatchedDateOfDeath(final LocalDate patchedDayOfDeath) {
            expectedDateOfDeath = patchedDayOfDeath;
            return this;
        }

        void matches(final HsOfficePartnerEntity givenPartner) {

            assertThat(givenPartner.getContact().getUuid()).isEqualTo(expectedContactUuid);
            assertThat(givenPartner.getPerson().getUuid()).isEqualTo(expectedPersonUuid);
            assertThat(givenPartner.getRegistrationOffice()).isEqualTo(expectedRegOffice);
            assertThat(givenPartner.getRegistrationNumber()).isEqualTo(expectedRegNumber);
            assertThat(givenPartner.getBirthday()).isEqualTo(expectedBirthday);
            assertThat(givenPartner.getBirthName()).isEqualTo(expectedBirthName);
            assertThat(givenPartner.getDateOfDeath()).isEqualTo(expectedDateOfDeath);

        }

    }

    enum LocalDatePatches {
        REAL_VALUE(LocalDate.now()),
        NULL_VALUE(null);

        final LocalDate value;

        LocalDatePatches(final LocalDate patchedBirthday) {
            value = patchedBirthday;
        }
    }

}
