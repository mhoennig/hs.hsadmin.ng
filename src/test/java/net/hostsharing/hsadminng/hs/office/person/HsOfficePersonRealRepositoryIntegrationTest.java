package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealTestEntity.hsOfficePersonRealEntity;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficePersonRealRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficePersonRealRepository personRealRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
    HttpServletRequest request;
    @Autowired
    private HsOfficePersonRealRepository hsOfficePersonRealRepository;

    @Nested
    class CreatePerson {

        @Test
        public void arbitraryUser_canCreateNewPerson() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var count = personRealRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(personRealRepo.save(
                    hsOfficePersonRealEntity("another new person"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficePersonRealEntity::getUuid).isNotNull();
            assertThatPersonIsPersisted(result.returnedValue());
            assertThat(personRealRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> toCleanup(
                    personRealRepo.save(hsOfficePersonRealEntity("another new person"))
            )).assumeSuccessful();

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(
                    Array.from(
                            initialRoleNames,
                            "hs_office.person#anothernewperson:OWNER",
                            "hs_office.person#anothernewperson:ADMIN",
                            "hs_office.person#anothernewperson:REFERRER"
                    ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(
                    Array.fromFormatted(
                            initialGrantNames,
                            "{ grant perm:hs_office.person#anothernewperson:INSERT>hs_office.relation to role:hs_office.person#anothernewperson:ADMIN by system and assume }",

                            "{ grant role:hs_office.person#anothernewperson:OWNER       to user:selfregistered-user-drew@hostsharing.org by hs_office.person#anothernewperson:OWNER and assume }",
                            "{ grant role:hs_office.person#anothernewperson:OWNER       to role:rbac.global#global:ADMIN by system and assume }",
                            "{ grant perm:hs_office.person#anothernewperson:UPDATE      to role:hs_office.person#anothernewperson:ADMIN by system and assume }",
                            "{ grant perm:hs_office.person#anothernewperson:DELETE      to role:hs_office.person#anothernewperson:OWNER by system and assume }",
                            "{ grant role:hs_office.person#anothernewperson:ADMIN       to role:hs_office.person#anothernewperson:OWNER by system and assume }",

                            "{ grant perm:hs_office.person#anothernewperson:SELECT      to role:hs_office.person#anothernewperson:REFERRER by system and assume }",
                            "{ grant role:hs_office.person#anothernewperson:REFERRER    to role:hs_office.person#anothernewperson:ADMIN by system and assume }"
                    ));
        }

        private void assertThatPersonIsPersisted(final HsOfficePersonRealEntity saved) {
            final var found = personRealRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindAllPersons {

        @Test
        public void arbitraryUser_canViewAllPersons() {
            // given
            context("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = personRealRepo.findPersonByOptionalNameLike(null);

            // then
            allThesePersonsAreReturned(
                    result,
                    "NP Smith, Peter",
                    "LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.",
                    "IF Third OHG",
                    "UF Erben Bessler");
        }
    }

    @Nested
    class FindByCaptionLike {

        @Test
        public void arbitraryUser_canViewAllPersons() {
            // given
            context("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = personRealRepo.findPersonByOptionalNameLike("Peter Smith - The Second Hand%");

            // then
            exactlyThesePersonsAreReturned(result, "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.");
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp, targetdelta->>'tradename', targetdelta->>'lastname'
                    from base.tx_journal_v
                    where targettable = 'hs_office.person';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating person test-data, hs_office.person, INSERT, Hostsharing eG, null]",
                "[creating person test-data, hs_office.person, INSERT, First GmbH, null]",
                "[creating person test-data, hs_office.person, INSERT, Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K., null]",
                "[creating person test-data, hs_office.person, INSERT, Third OHG, null]");
    }

    void exactlyThesePersonsAreReturned(final List<HsOfficePersonRealEntity> actualResult, final String... personCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficePersonRealEntity::getTradeName)
                .containsExactlyInAnyOrder(personCaptions);
    }

    void allThesePersonsAreReturned(final List<HsOfficePersonRealEntity> actualResult, final String... personCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficePerson::toShortString)
                .contains(personCaptions);
    }
}
