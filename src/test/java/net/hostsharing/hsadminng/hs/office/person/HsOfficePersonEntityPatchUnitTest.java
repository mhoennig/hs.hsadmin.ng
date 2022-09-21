package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: there must be an easier way to test such patch classes
class HsOfficePersonEntityPatchUnitTest {

    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();
    final HsOfficePersonEntity givenPerson = new HsOfficePersonEntity();
    final HsOfficePersonPatchResource patchResource = new HsOfficePersonPatchResource();

    private final HsOfficePersonEntityPatch hsOfficePersonEntityPatch =
            new HsOfficePersonEntityPatch(givenPerson);

    {
        givenPerson.setUuid(INITIAL_PERSON_UUID);
        givenPerson.setPersonType(HsOfficePersonType.LEGAL);
        givenPerson.setTradeName("initial@example.org");
        givenPerson.setFamilyName("initial postal address");
        givenPerson.setGivenName("+01 100 123456789");
    }

    @Test
    void willPatchAllProperties() {
        // given
        patchResource.setPersonType(HsOfficePersonTypeResource.NATURAL);
        patchResource.setTradeName(JsonNullable.of("patched@example.org"));
        patchResource.setFamilyName(JsonNullable.of("patched postal address"));
        patchResource.setGivenName(JsonNullable.of("+01 200 987654321"));

        // when
        hsOfficePersonEntityPatch.apply(patchResource);

        // then
        new HsOfficePersonEntityMatcher()
                .withPatchedPersonType(HsOfficePersonType.NATURAL)
                .withPatchedTradeName("patched@example.org")
                .withPatchedFamilyName("patched postal address")
                .withPatchedGivenName("+01 200 987654321")
                .matches(givenPerson);
    }

    @ParameterizedTest
    @EnumSource(HsOfficePersonTypeResource.class)
    void willPatchOnlyPersonTypeProperty(final HsOfficePersonTypeResource patchedValue) {
        // given
        patchResource.setPersonType(patchedValue);

        // when
        hsOfficePersonEntityPatch.apply(patchResource);

        // then
        new HsOfficePersonEntityMatcher()
                .withPatchedPersonType(HsOfficePersonType.valueOf(patchedValue.getValue()))
                .matches(givenPerson);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched@example.org" })
    @NullSource
    void willPatchOnlyTradeNameProperty(final String patchedValue) {
        // given
        patchResource.setTradeName(JsonNullable.of(patchedValue));

        // when
        hsOfficePersonEntityPatch.apply(patchResource);

        // then
        new HsOfficePersonEntityMatcher()
                .withPatchedTradeName(patchedValue)
                .matches(givenPerson);
    }

    @ParameterizedTest
    @ValueSource(strings = { "patched postal address" })
    @NullSource
    void willPatchOnlyFamilyNameProperty(final String patchedValue) {
        // given
        patchResource.setFamilyName(JsonNullable.of(patchedValue));

        // when
        hsOfficePersonEntityPatch.apply(patchResource);

        // then
        new HsOfficePersonEntityMatcher()
                .withPatchedFamilyName(patchedValue)
                .matches(givenPerson);
    }

    @ParameterizedTest
    @ValueSource(strings = { "+01 200 987654321" })
    @NullSource
    void willPatchOnlyGivenNameProperty(final String patchedValue) {
        // given
        patchResource.setGivenName(JsonNullable.of(patchedValue));

        // when
        hsOfficePersonEntityPatch.apply(patchResource);

        // then
        new HsOfficePersonEntityMatcher()
                .withPatchedGivenName(patchedValue)
                .matches(givenPerson);
    }

    private static class HsOfficePersonEntityMatcher {

        private HsOfficePersonType expectedPersonType = HsOfficePersonType.LEGAL;
        private String expectedTradeName = "initial@example.org";
        private String expectedFamilyName = "initial postal address";

        private String expectedGivenName = "+01 100 123456789";

        HsOfficePersonEntityMatcher withPatchedPersonType(final HsOfficePersonType patchedPersonType) {
            expectedPersonType = patchedPersonType;
            return this;
        }

        HsOfficePersonEntityMatcher withPatchedTradeName(final String patchedTradeName) {
            expectedTradeName = patchedTradeName;
            return this;
        }

        HsOfficePersonEntityMatcher withPatchedFamilyName(final String patchedFamilyName) {
            expectedFamilyName = patchedFamilyName;
            return this;
        }

        HsOfficePersonEntityMatcher withPatchedGivenName(final String patchedGivenName) {
            expectedGivenName = patchedGivenName;
            return this;
        }

        void matches(final HsOfficePersonEntity givenPerson) {

            assertThat(givenPerson.getPersonType()).isEqualTo(expectedPersonType);
            assertThat(givenPerson.getTradeName()).isEqualTo(expectedTradeName);
            assertThat(givenPerson.getFamilyName()).isEqualTo(expectedFamilyName);
            assertThat(givenPerson.getGivenName()).isEqualTo(expectedGivenName);
        }
    }
}
