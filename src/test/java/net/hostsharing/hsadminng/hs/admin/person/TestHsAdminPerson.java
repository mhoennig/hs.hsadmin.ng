package net.hostsharing.hsadminng.hs.admin.person;

import java.util.UUID;

public class TestHsAdminPerson {

    public static final HsAdminPersonEntity somePerson = hsAdminPerson("some person");

    static public HsAdminPersonEntity hsAdminPerson(final String tradeName) {
        return HsAdminPersonEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsAdminPersonType.NATURAL)
                .tradeName(tradeName)
                .build();
    }
}
