package net.hostsharing.hsadminng.hs.office.contact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeContactEntityUnitTest {

    @Test
    void toStringReturnsNullForNullContact() {
        final HsOfficeContactEntity givenContact = null;
      assertThat("" + givenContact).isEqualTo("null");
    }

    @Test
    void toStringReturnsLabel() {
        final var givenContact = HsOfficeContactEntity.builder().label("given label").build();
        assertThat("" + givenContact).isEqualTo("contact(label='given label')");
    }

}
