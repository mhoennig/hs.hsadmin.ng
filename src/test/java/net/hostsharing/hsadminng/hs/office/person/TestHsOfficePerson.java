package net.hostsharing.hsadminng.hs.office.person;


public class TestHsOfficePerson {

    public static final HsOfficePersonEntity somePerson = hsOfficePerson("some person");

    static public HsOfficePersonEntity hsOfficePerson(final String tradeName) {
        return HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .tradeName(tradeName)
                .build();
    }
}
