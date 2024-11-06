package net.hostsharing.hsadminng.hs.office.contact;

import java.util.Map;

import static java.util.Map.entry;

public class HsOfficeContactRbacTestEntity {

    public static final HsOfficeContactRbacEntity TEST_RBAC_CONTACT = hsOfficeContact("some contact", "some-contact@example.com");

    static public HsOfficeContactRbacEntity hsOfficeContact(final String caption, final String emailAddr) {
        return HsOfficeContactRbacEntity.builder()
                .caption(caption)
                .postalAddress(Map.ofEntries(
                        entry("name", "M. Meyer"),
                        entry("street", "Teststraße 11"),
                        entry("zipcode", "D-12345"),
                        entry("city", "Berlin")
                ))
                .emailAddresses(Map.of("main", emailAddr))
                .build();
    }
}
