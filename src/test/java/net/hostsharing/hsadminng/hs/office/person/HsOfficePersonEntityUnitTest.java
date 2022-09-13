package net.hostsharing.hsadminng.hs.office.person;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePersonEntityUnitTest {

    @Test
    void getDisplayReturnsTradeNameIfAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .tradeName("some trade name")
                .build();

        final var actualDisplay = givenPersonEntity.getDisplayName();

        assertThat(actualDisplay).isEqualTo("some trade name");
    }

    @Test
    void getDisplayReturnsFamilyAndGivenNameIfNoTradeNameAvailable() {
        final var givenPersonEntity = HsOfficePersonEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.getDisplayName();

        assertThat(actualDisplay).isEqualTo("some family name, some given name");
    }

}
