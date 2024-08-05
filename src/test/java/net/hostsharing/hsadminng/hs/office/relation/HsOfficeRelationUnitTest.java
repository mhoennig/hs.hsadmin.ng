package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeRelationUnitTest {

    private HsOfficePersonEntity anchor = HsOfficePersonEntity.builder()
            .personType(HsOfficePersonType.LEGAL_PERSON)
            .tradeName("some trade name")
            .build();
    private HsOfficePersonEntity holder = HsOfficePersonEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .familyName("Meier")
            .givenName("Mellie")
            .build();

    @Test
    void toStringReturnsAllProperties() {
        final var given = HsOfficeRelationRbacEntity.builder()
                .type(HsOfficeRelationType.SUBSCRIBER)
                .mark("members-announce")
                .anchor(anchor)
                .holder(holder)
                .build();

        assertThat(given.toString()).isEqualTo("rel(anchor='LP some trade name', type='SUBSCRIBER', mark='members-announce', holder='NP Meier, Mellie')");
    }

    @Test
    void toShortString() {
        final var given = HsOfficeRelationRbacEntity.builder()
                .type(HsOfficeRelationType.REPRESENTATIVE)
                .anchor(anchor)
                .holder(holder)
                .build();

        assertThat(given.toShortString()).isEqualTo("rel(anchor='LP some trade name', type='REPRESENTATIVE', holder='NP Meier, Mellie')");
    }
}
