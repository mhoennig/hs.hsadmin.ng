package net.hostsharing.hsadminng.hs.office.relationship;

import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class HsOfficeRelationshipEntityUnitTest {

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
        final var given = HsOfficeRelationshipEntity.builder()
                .relType(HsOfficeRelationshipType.SUBSCRIBER)
                .relMark("members-announce")
                .relAnchor(anchor)
                .relHolder(holder)
                .build();

        assertThat(given.toString()).isEqualTo("rel(relAnchor='LP some trade name', relType='SUBSCRIBER', relMark='members-announce', relHolder='NP Meier, Mellie')");
    }

    @Test
    void toShortString() {
        final var given = HsOfficeRelationshipEntity.builder()
                .relType(HsOfficeRelationshipType.REPRESENTATIVE)
                .relAnchor(anchor)
                .relHolder(holder)
                .build();

        assertThat(given.toShortString()).isEqualTo("rel(relAnchor='LP some trade name', relType='REPRESENTATIVE', relHolder='NP Meier, Mellie')");
    }
}
