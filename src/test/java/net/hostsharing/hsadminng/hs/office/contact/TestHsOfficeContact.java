package net.hostsharing.hsadminng.hs.office.contact;

import java.util.UUID;

public class TestHsOfficeContact {

    public static final HsOfficeContactEntity TEST_CONTACT = hsOfficeContact("some contact", "some-contact@example.com");

    static public HsOfficeContactEntity hsOfficeContact(final String label, final String emailAddr) {
        return HsOfficeContactEntity.builder()
                .label(label)
                .postalAddress("address of " + label)
                .emailAddresses(emailAddr)
                .build();
    }
}
