package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: there must be an easier way to test such patch classes
class HsOfficeContactEntityPatchUnitTest {

    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    final HsOfficeContactEntity givenContact = new HsOfficeContactEntity();
    final HsOfficeContactPatchResource patchResource = new HsOfficeContactPatchResource();

    private final HsOfficeContactEntityPatch hsOfficeContactEntityPatch =
            new HsOfficeContactEntityPatch(givenContact);

    {
        givenContact.setUuid(INITIAL_CONTACT_UUID);
        givenContact.setLabel("initial label");
        givenContact.setEmailAddresses("initial@example.org");
        givenContact.setPostalAddress("initial postal address");
        givenContact.setPhoneNumbers("+01 100 123456789");
    }

    @Test
    void willPatchAllProperties() {
        // given
        patchResource.setLabel(JsonNullable.of("patched label"));
        patchResource.setEmailAddresses(JsonNullable.of("patched@example.org"));
        patchResource.setPostalAddress(JsonNullable.of("patched postal address"));
        patchResource.setPhoneNumbers(JsonNullable.of("+01 200 987654321"));

        // when
        hsOfficeContactEntityPatch.apply(patchResource);

        // then
        new HsOfficeContactEntityMatcher()
                .withPatchedLabel("patched label")
                .withPatchedEmailAddresses("patched@example.org")
                .withPatchedPostalAddress("patched postal address")
                .withPatchedPhoneNumbers("+01 200 987654321")
                .matches(givenContact);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched label" })
    @NullSource
    void willPatchOnlyLabelProperty(final String patchedValue) {
        // given
        patchResource.setLabel(JsonNullable.of(patchedValue));

        // when
        hsOfficeContactEntityPatch.apply(patchResource);

        // then
        new HsOfficeContactEntityMatcher()
                .withPatchedLabel(patchedValue)
                .matches(givenContact);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched@example.org" })
    @NullSource
    void willPatchOnlyEmailAddressesProperty(final String patchedValue) {
        // given
        patchResource.setEmailAddresses(JsonNullable.of(patchedValue));

        // when
        hsOfficeContactEntityPatch.apply(patchResource);

        // then
        new HsOfficeContactEntityMatcher()
                .withPatchedEmailAddresses(patchedValue)
                .matches(givenContact);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched postal address" })
    @NullSource
    void willPatchOnlyPostalAddressProperty(final String patchedValue) {
        // given
        patchResource.setPostalAddress(JsonNullable.of(patchedValue));

        // when
        hsOfficeContactEntityPatch.apply(patchResource);

        // then
        new HsOfficeContactEntityMatcher()
                .withPatchedPostalAddress(patchedValue)
                .matches(givenContact);
    }

    @ParameterizedTest
    @ValueSource(strings = { "+01 200 987654321" })
    @NullSource
    void willPatchOnlyPhoneNumbersProperty(final String patchedValue) {
        // given
        patchResource.setPhoneNumbers(JsonNullable.of(patchedValue));

        // when
        hsOfficeContactEntityPatch.apply(patchResource);

        // then
        new HsOfficeContactEntityMatcher()
                .withPatchedPhoneNumbers(patchedValue)
                .matches(givenContact);
    }

    private static class HsOfficeContactEntityMatcher {

        private String expectedLabel = "initial label";
        private String expectedEmailAddresses = "initial@example.org";
        private String expectedPostalAddress = "initial postal address";

        private String expectedPhoneNumbers = "+01 100 123456789";

        HsOfficeContactEntityMatcher withPatchedLabel(final String patchedLabel) {
            expectedLabel = patchedLabel;
            return this;
        }

        HsOfficeContactEntityMatcher withPatchedEmailAddresses(final String patchedEmailAddresses) {
            expectedEmailAddresses = patchedEmailAddresses;
            return this;
        }

        HsOfficeContactEntityMatcher withPatchedPostalAddress(final String patchedPostalAddress) {
            expectedPostalAddress = patchedPostalAddress;
            return this;
        }

        HsOfficeContactEntityMatcher withPatchedPhoneNumbers(final String patchedPhoneNumbers) {
            expectedPhoneNumbers = patchedPhoneNumbers;
            return this;
        }

        void matches(final HsOfficeContactEntity givenContact) {

            assertThat(givenContact.getLabel()).isEqualTo(expectedLabel);
            assertThat(givenContact.getEmailAddresses()).isEqualTo(expectedEmailAddresses);
            assertThat(givenContact.getPostalAddress()).isEqualTo(expectedPostalAddress);
            assertThat(givenContact.getPhoneNumbers()).isEqualTo(expectedPhoneNumbers);
        }
    }
}
