package net.hostsharing.hsadminng.hs.office.contact;

import java.util.Map;

public class HsOfficeContactRealTestEntity {

    public static final HsOfficeContactRealEntity TEST_REAL_CONTACT = hsOfficeContact("some contact", "some-contact@example.com");

    static public HsOfficeContactRealEntity hsOfficeContact(final String caption, final String emailAddr) {
        return HsOfficeContactRealEntity.builder()
                .caption(caption)
                .postalAddress("address of " + caption)
                .emailAddresses(Map.of("main", emailAddr))
                .build();
    }
}
