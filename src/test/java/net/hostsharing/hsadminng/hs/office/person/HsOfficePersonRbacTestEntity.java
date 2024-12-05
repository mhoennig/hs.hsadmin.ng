package net.hostsharing.hsadminng.hs.office.person;


public class HsOfficePersonRbacTestEntity {

    public static final HsOfficePersonRbacEntity somePerson = hsOfficePersonRbacEntity("some person");

    public static HsOfficePersonRbacEntity hsOfficePersonRbacEntity(final String tradeName) {
        return HsOfficePersonRbacEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .tradeName(tradeName)
                .build();
    }
}
