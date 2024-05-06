package net.hostsharing.hsadminng.hs.office.contact;

import java.util.Map;

public class TestHsOfficeContact {

    public static final HsOfficeContactEntity TEST_CONTACT = hsOfficeContact("some contact", "some-contact@example.com");

    static public HsOfficeContactEntity hsOfficeContact(final String caption, final String emailAddr) {
        return HsOfficeContactEntity.builder()
                .caption(caption)
                .postalAddress("address of " + caption)
                .emailAddresses(Map.of("main", emailAddr))
                .build();
    }
}
