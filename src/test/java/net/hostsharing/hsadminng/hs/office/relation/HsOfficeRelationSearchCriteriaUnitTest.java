package net.hostsharing.hsadminng.hs.office.relation;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeRelationSearchCriteriaUnitTest {

    @Test
    void builderAndAccessors() {
        final var personUuid = UUID.randomUUID();
        final var anchorPersonUuid = UUID.randomUUID();
        final var holderPersonUuid = UUID.randomUUID();
        final var contactUuid = UUID.randomUUID();
        final var relationType = HsOfficeRelationType.PARTNER;
        final var mark = "testMark";
        final var personData = "personData";
        final var contactData = "contactData";

        final var criteria = HsOfficeRelationSearchCriteria.builder()
                .personUuid(personUuid)
                .anchorPersonUuid(anchorPersonUuid)
                .holderPersonUuid(holderPersonUuid)
                .relationType(relationType)
                .mark(mark)
                .personData(personData)
                .contactData(contactData)
                .contactUuid(contactUuid)
                .build();

        assertThat(criteria.personUuid()).isEqualTo(personUuid);
        assertThat(criteria.anchorPersonUuid()).isEqualTo(anchorPersonUuid);
        assertThat(criteria.holderPersonUuid()).isEqualTo(holderPersonUuid);
        assertThat(criteria.relationType()).isEqualTo(relationType);
        assertThat(criteria.mark()).isEqualTo(mark);
        assertThat(criteria.personData()).isEqualTo(personData);
        assertThat(criteria.contactData()).isEqualTo(contactData);
        assertThat(criteria.contactUuid()).isEqualTo(contactUuid);
    }

    @Test
    void getRelationTypeString() {
        assertThat(HsOfficeRelationSearchCriteria.builder().relationType(HsOfficeRelationType.PARTNER).build().getRelationTypeString())
                .isEqualTo("PARTNER");
        assertThat(HsOfficeRelationSearchCriteria.builder().relationType(null).build().getRelationTypeString())
                .isNull();
    }

    @Test
    void getPersonDataPattern() {
        assertThat(HsOfficeRelationSearchCriteria.builder().personData("John").build().getPersonDataPattern())
                .isEqualTo("%john%");
        assertThat(HsOfficeRelationSearchCriteria.builder().personData(null).build().getPersonDataPattern())
                .isNull();
    }

    @Test
    void getMarkPattern() {
        assertThat(HsOfficeRelationSearchCriteria.builder().mark("ABC").build().getMarkPattern())
                .isEqualTo("%abc%");
        assertThat(HsOfficeRelationSearchCriteria.builder().mark(null).build().getMarkPattern())
                .isNull();
    }

    @Test
    void getContactDataPattern() {
        assertThat(HsOfficeRelationSearchCriteria.builder().contactData("Example.Com").build().getContactDataPattern())
                .isEqualTo("%example.com%");
        assertThat(HsOfficeRelationSearchCriteria.builder().contactData(null).build().getContactDataPattern())
                .isNull();
    }
}
