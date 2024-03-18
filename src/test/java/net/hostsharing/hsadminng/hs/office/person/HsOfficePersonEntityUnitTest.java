package net.hostsharing.hsadminng.hs.office.person;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePersonEntityUnitTest {

    @Test
    void getDisplayReturnsTradeNameIfAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("some trade name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("LP some trade name");
    }

    @Test
    void getDisplayReturnsFamilyAndGivenNameIfNoTradeNameAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithTradeNameReturnsTradeName() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("some trade name")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("LP some trade name");
    }

    @Test
    void toShortStringWithoutTradeNameReturnsFamilyAndGivenName() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithSalutationAndTitleReturnsSalutationAndTitle() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .salutation("Frau")
            .title("Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP Frau Dr. some family name, some given name");
    }

    @Test
    void toShortStringWithSalutationAndWithoutTitleReturnsSalutation() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .salutation("Frau")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP Frau some family name, some given name");
    }

    @Test
    void toShortStringWithoutSalutationAndWithTitleReturnsTitle() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .title("Dr. Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP Dr. Dr. some family name, some given name");
    }

    @Test
    void toStringWithAllFieldsReturnsAllButUuid() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .tradeName("some trade name")
                .title("Dr.")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(personType='NP', tradeName='some trade name', title='Dr.', familyName='some family name', givenName='some given name')");
    }

    @Test
    void toStringSkipsNullFields() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(familyName='some family name', givenName='some given name')");
    }
    @Test
    void toStringWithSalutationAndTitleRetursSalutationAndTitle() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .salutation("Herr")
            .title("Prof. Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(salutation='Herr', title='Prof. Dr.', familyName='some family name', givenName='some given name')");
    }
    @Test
    void toStringWithSalutationAndWithoutTitleSkipsTitle() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .salutation("Herr")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(salutation='Herr', familyName='some family name', givenName='some given name')");
    }
    @Test
    void toStringWithoutSalutationAndWithTitleSkipsSalutation() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
            .title("some title")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(title='some title', familyName='some family name', givenName='some given name')");
    }

}
