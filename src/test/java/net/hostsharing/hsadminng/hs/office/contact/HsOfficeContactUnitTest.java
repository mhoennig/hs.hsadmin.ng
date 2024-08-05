package net.hostsharing.hsadminng.hs.office.contact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeContactUnitTest {

    @Test
    void toStringReturnsNullForNullContact() {
        final HsOfficeContactRbacEntity givenContact = null;
      assertThat("" + givenContact).isEqualTo("null");
    }

    @Test
    void toStringReturnsCaption() {
        final var givenContact = HsOfficeContactRbacEntity.builder().caption("given caption").build();
        assertThat("" + givenContact).isEqualTo("contact(caption='given caption')");
    }

}
