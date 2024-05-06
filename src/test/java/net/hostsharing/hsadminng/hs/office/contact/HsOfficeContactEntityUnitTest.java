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
    void toStringReturnsCaption() {
        final var givenContact = HsOfficeContactEntity.builder().caption("given caption").build();
        assertThat("" + givenContact).isEqualTo("contact(caption='given caption')");
    }

}
