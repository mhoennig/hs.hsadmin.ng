package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.REPRESENTATIVE;
import static net.hostsharing.hsadminng.rbac.test.EntityList.one;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
@Tag("officeIntegrationTest")
class HsOfficeRealRelationRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeRelationRealRepository realRelationRepo;

    @Autowired
    HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    HsOfficeContactRealRepository realContactRepo;

    @PersistenceContext
    EntityManager em;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class FindRelations {

        @Test
        public void canFindAllRelationsOfGivenPerson() {
            // given
            final var personUuid = determinePersonUuid(NATURAL_PERSON, "Smith");

            // when
            final var result = realRelationRepo.findRelationRelatedToPersonUuid(personUuid);

            // then
            context("superuser-alex@hostsharing.net"); // just to be able to access RBAc-entities persons+contact
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')",
                    "rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='NP Smith, Peter', type='DEBITOR', holder='NP Smith, Peter', contact='third contact')",
                    "rel(anchor='IF Third OHG', type='SUBSCRIBER', mark='members-announce', holder='NP Smith, Peter', contact='third contact')"
            );
        }

        @Test
        public void canFindAllRelationsOfGivenPersonAndType() {
            // given:
            final var personUuid = determinePersonUuid(NATURAL_PERSON, "Smith");

            // when:
            final var result = realRelationRepo.findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(personUuid, REPRESENTATIVE, null, null, null);

            // then:
            context("superuser-alex@hostsharing.net"); // just to be able to access RBAc-entities persons+contact
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')"
            );
        }
    }

    @Nested
    class CreateRelation {

        @Test
        public void canNotAddDebitorRelationWithAnchorThatIsNotAPartner() {
            // given
            context("superuser-alex@hostsharing.net");

            final var givenPartnerPerson = hsOfficePersonRealEntity("Non-Partner Person");
            final var givenContact = one(realContactRepo.findContactByOptionalCaptionLike("eleventh contact"));

            // when
            final var result = attempt(em, () -> {
                final var newRelation = HsOfficeRelationRealEntity.builder()
                    .type(HsOfficeRelationType.DEBITOR)
                    .anchor(givenPartnerPerson)
                    .holder(givenPartnerPerson)
                    .contact(givenContact)
                    .build();
                final var entity = realRelationRepo.save(newRelation);
                return toCleanup(entity.load());
            });

            // then
            result.assertExceptionWithRootCauseMessage(org.postgresql.util.PSQLException.class, "ERROR: [400] invalid debitor relation: anchor person must have a PARTNER relation");
        }
    }

    private UUID determinePersonUuid(final HsOfficePersonType type, final String familyName) {
        return (UUID) em.createNativeQuery("""
                    SELECT uuid FROM hs_office.person p
                    WHERE p.personType = cast(:type as hs_office.PersonType) AND p.familyName = :familyName
                    """, UUID.class)
                .setParameter("familyName", familyName)
                .setParameter("type", type.toString())
                .getSingleResult();

    }

    private void exactlyTheseRelationsAreReturned(
            final List<HsOfficeRelationRealEntity> actualResult,
            final String... relationNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelation::toString)
                .containsExactlyInAnyOrder(relationNames);
    }

    public HsOfficePersonRealEntity hsOfficePersonRealEntity(final String tradeName) {
        return realPersonRepo.save(HsOfficePersonRealEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .tradeName(tradeName)
            .build());
    }

}
