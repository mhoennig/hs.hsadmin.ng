package net.hostsharing.hsadminng.hs.office.person;

import java.util.UUID;

public class TestHsOfficePerson {

    public static final HsOfficePersonEntity somePerson = hsOfficePerson("some person");

    static public HsOfficePersonEntity hsOfficePerson(final String tradeName) {
        return HsOfficePersonEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsOfficePersonType.NATURAL)
                .tradeName(tradeName)
                .build();
    }
}
