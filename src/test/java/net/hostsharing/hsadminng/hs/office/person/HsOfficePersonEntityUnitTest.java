package net.hostsharing.hsadminng.hs.office.person;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePersonEntityUnitTest {

    @Test
    void getDisplayReturnsTradeNameIfAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .tradeName("some trade name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("some trade name");
    }

    @Test
    void getDisplayReturnsFamilyAndGivenNameIfNoTradeNameAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("some family name, some given name");
    }

    @Test
    void toShortStringWithTradeNameReturnsTradeName() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .tradeName("some trade name")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("some trade name");
    }

    @Test
    void toShortStringWithoutTradeNameReturnsFamilyAndGivenName() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("some family name, some given name");
    }

    @Test
    void toStringWithAllFieldsReturnsAllButUuid() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsOfficePersonType.NATURAL)
                .tradeName("some trade name")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(personType='NATURAL', tradeName='some trade name', familyName='some family name', givenName='some given name')");
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
}
