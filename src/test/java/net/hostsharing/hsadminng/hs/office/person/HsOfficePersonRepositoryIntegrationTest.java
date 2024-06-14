package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.person.TestHsOfficePerson.hsOfficePerson;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficePersonRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreatePerson {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewPerson() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = personRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(personRepo.save(
                    hsOfficePerson("a new person"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficePersonEntity::getUuid).isNotNull();
            assertThatPersonIsPersisted(result.returnedValue());
            assertThat(personRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewPerson() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var count = personRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(personRepo.save(
                    hsOfficePerson("another new person"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficePersonEntity::getUuid).isNotNull();
            assertThatPersonIsPersisted(result.returnedValue());
            assertThat(personRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> toCleanup(
                    personRepo.save(hsOfficePerson("another new person"))
            )).assumeSuccessful();

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(
                    Array.from(
                            initialRoleNames,
                            "hs_office_person#anothernewperson:OWNER",
                            "hs_office_person#anothernewperson:ADMIN",
                            "hs_office_person#anothernewperson:REFERRER"
                    ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(
                    Array.fromFormatted(
                            initialGrantNames,
                            "{ grant perm:hs_office_person#anothernewperson:INSERT>hs_office_relation to role:hs_office_person#anothernewperson:ADMIN by system and assume }",

                            "{ grant role:hs_office_person#anothernewperson:OWNER       to user:selfregistered-user-drew@hostsharing.org by hs_office_person#anothernewperson:OWNER and assume }",
                            "{ grant role:hs_office_person#anothernewperson:OWNER       to role:global#global:ADMIN by system and assume }",
                            "{ grant perm:hs_office_person#anothernewperson:UPDATE      to role:hs_office_person#anothernewperson:ADMIN by system and assume }",
                            "{ grant perm:hs_office_person#anothernewperson:DELETE      to role:hs_office_person#anothernewperson:OWNER by system and assume }",
                            "{ grant role:hs_office_person#anothernewperson:ADMIN       to role:hs_office_person#anothernewperson:OWNER by system and assume }",

                            "{ grant perm:hs_office_person#anothernewperson:SELECT      to role:hs_office_person#anothernewperson:REFERRER by system and assume }",
                            "{ grant role:hs_office_person#anothernewperson:REFERRER    to role:hs_office_person#anothernewperson:ADMIN by system and assume }"
                    ));
        }

        private void assertThatPersonIsPersisted(final HsOfficePersonEntity saved) {
            final var found = personRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllPersons {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPersons() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = personRepo.findPersonByOptionalNameLike(null);

            // then
            allThesePersonsAreReturned(
                    result,
                    "NP Smith, Peter",
                    "LP Second e.K.",
                    "IF Third OHG",
                    "UF Erben Bessler");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnPerson() {
            // given:
            final var givenPerson = givenSomeTemporaryPerson("pac-admin-zzz00@zzz.example.com");

            // when:
            context("pac-admin-zzz00@zzz.example.com");
            final var result = personRepo.findPersonByOptionalNameLike(null);

            // then:
            exactlyThesePersonsAreReturned(result, givenPerson.getTradeName());
        }
    }

    @Nested
    class FindByCaptionLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPersons() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = personRepo.findPersonByOptionalNameLike("Second");

            // then
            exactlyThesePersonsAreReturned(result, "Second e.K.");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnPerson() {
            // given:
            final var givenPerson = givenSomeTemporaryPerson("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = personRepo.findPersonByOptionalNameLike(givenPerson.getTradeName());

            // then:
            exactlyThesePersonsAreReturned(result, givenPerson.getTradeName());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyPerson() {
            // given
            final var givenPerson = givenSomeTemporaryPerson("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return personRepo.findPersonByOptionalNameLike(givenPerson.getTradeName());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canDeleteAPersonCreatedByItself() {
            // given
            final var givenPerson = givenSomeTemporaryPerson("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return personRepo.findPersonByOptionalNameLike(givenPerson.getTradeName());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        // TODO.test: can NOT delete test is missing

        @Test
        public void deletingAPersonAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("selfregistered-user-drew@hostsharing.org", null);
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());
            final var givenPerson = givenSomeTemporaryPerson("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(initialRoleNames));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(initialGrantNames));
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_office_person';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating person test-data First GmbH, hs_office_person, INSERT]",
                "[creating person test-data Second e.K., Smith, Peter, hs_office_person, INSERT]");
    }

    private HsOfficePersonEntity givenSomeTemporaryPerson(
            final String createdByUser,
            Supplier<HsOfficePersonEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return toCleanup(personRepo.save(entitySupplier.get()));
        }).assumeSuccessful().returnedValue();
    }

    private HsOfficePersonEntity givenSomeTemporaryPerson(final String createdByUser) {
        return givenSomeTemporaryPerson(createdByUser, () ->
                hsOfficePerson("some temporary person #" + RandomStringUtils.random(12)));
    }

    void exactlyThesePersonsAreReturned(final List<HsOfficePersonEntity> actualResult, final String... personCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficePersonEntity::getTradeName)
                .containsExactlyInAnyOrder(personCaptions);
    }

    void allThesePersonsAreReturned(final List<HsOfficePersonEntity> actualResult, final String... personCaptions) {
        assertThat(actualResult)
                .extracting(hsOfficePersonEntity -> hsOfficePersonEntity.toShortString())
                .contains(personCaptions);
    }
}
