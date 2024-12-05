package net.hostsharing.hsadminng.hs.office.person;


public class HsOfficePersonRealTestEntity {

    public static final HsOfficePersonRealEntity somePerson = hsOfficePersonRealEntity("some person");

    public static HsOfficePersonRealEntity hsOfficePersonRealEntity(final String tradeName) {
        return HsOfficePersonRealEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .tradeName(tradeName)
                .build();
    }
}
