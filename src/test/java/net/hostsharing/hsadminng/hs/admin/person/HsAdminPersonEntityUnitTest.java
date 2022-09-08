package net.hostsharing.hsadminng.hs.admin.person;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsAdminPersonEntityUnitTest {

    @Test
    void getDisplayReturnsTradeNameIfAvailable() {
        final var givenPersonEntity = HsAdminPersonEntity.builder()
                .tradeName("some trade name")
                .build();

        final var actualDisplay = givenPersonEntity.getDisplayName();

        assertThat(actualDisplay).isEqualTo("some trade name");
    }

    @Test
    void getDisplayReturnsFamilyAndGivenNameIfNoTradeNameAvailable() {
        final var givenPersonEntity = HsAdminPersonEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.getDisplayName();

        assertThat(actualDisplay).isEqualTo("some family name, some given name");
    }

}
