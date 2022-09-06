package net.hostsharing.hsadminng.hs.admin.contact;

import java.util.UUID;

public class TestHsAdminContact {

    public static final HsAdminContactEntity someContact = hsAdminContact("some contact", "some-contact@example.com");

    static public HsAdminContactEntity hsAdminContact(final String label, final String emailAddr) {
        return HsAdminContactEntity.builder()
                .uuid(UUID.randomUUID())
                .label(label)
                .postalAddress("address of " + label)
                .emailAddresses(emailAddr)
                .build();
    }
}
