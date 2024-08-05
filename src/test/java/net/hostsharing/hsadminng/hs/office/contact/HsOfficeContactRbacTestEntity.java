package net.hostsharing.hsadminng.hs.office.contact;

import java.util.Map;

public class HsOfficeContactRbacTestEntity {

    public static final HsOfficeContactRbacEntity TEST_RBAC_CONTACT = hsOfficeContact("some contact", "some-contact@example.com");

    static public HsOfficeContactRbacEntity hsOfficeContact(final String caption, final String emailAddr) {
        return HsOfficeContactRbacEntity.builder()
                .caption(caption)
                .postalAddress("address of " + caption)
                .emailAddresses(Map.of("main", emailAddr))
                .build();
    }
}
