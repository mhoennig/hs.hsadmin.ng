package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
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
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficePersonRepositoryIntegrationTest extends ContextBasedTest {

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

            final var result = attempt(em, () -> personRepo.save(
                    hsOfficePerson("a new person")));

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
            final var result = attempt(em, () -> personRepo.save(
                    hsOfficePerson("another new person")));

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
            final var count = personRepo.count();
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> personRepo.save(
                    hsOfficePerson("another new person"))
            ).assumeSuccessful();

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(
                    Array.from(
                            initialRoleNames,
                            "hs_office_person#anothernewperson.owner",
                            "hs_office_person#anothernewperson.admin",
                            "hs_office_person#anothernewperson.tenant",
                            "hs_office_person#anothernewperson.guest"
                    ));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(
                    Array.from(
                            initialGrantNames,
                            "{ grant role hs_office_person#anothernewperson.owner to role global#global.admin by system and assume }",
                            "{ grant perm edit on hs_office_person#anothernewperson to role hs_office_person#anothernewperson.admin by system and assume }",
                            "{ grant role hs_office_person#anothernewperson.tenant to role hs_office_person#anothernewperson.admin by system and assume }",
                            "{ grant perm * on hs_office_person#anothernewperson to role hs_office_person#anothernewperson.owner by system and assume }",
                            "{ grant role hs_office_person#anothernewperson.admin to role hs_office_person#anothernewperson.owner by system and assume }",
                            "{ grant perm view on hs_office_person#anothernewperson to role hs_office_person#anothernewperson.guest by system and assume }",
                            "{ grant role hs_office_person#anothernewperson.guest to role hs_office_person#anothernewperson.tenant by system and assume }",
                            "{ grant role hs_office_person#anothernewperson.owner to user selfregistered-user-drew@hostsharing.org by global#global.admin and assume }"
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
                    "NATURAL Smith, Peter",
                    "LEGAL Second e.K.",
                    "SOLE_REPRESENTATION Third OHG",
                    "JOINT_REPRESENTATION Erben Bessler");
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
    class FindByLabelLike {

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
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());
            final var givenPerson = givenSomeTemporaryPerson("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(initialRoleNames));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(initialGrantNames));
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select c.currenttask, j.targettable, j.targetop
                    from tx_journal j
                    join tx_context c on j.contextId = c.contextId
                    where targettable = 'hs_office_person';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating person test-data First GmbH, hs_office_person, INSERT]",
                "[creating person test-data Second e.K., Sandra, Miller, hs_office_person, INSERT]");
    }

    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        final var result = personRepo.findPersonByOptionalNameLike("some temporary person");
        result.forEach(tempPerson -> {
            System.out.println("DELETING temporary person: " + tempPerson.toShortString());
            personRepo.deleteByUuid(tempPerson.getUuid());
        });
    }

    private HsOfficePersonEntity givenSomeTemporaryPerson(
            final String createdByUser,
            Supplier<HsOfficePersonEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return personRepo.save(entitySupplier.get());
        }).assumeSuccessful().returnedValue();
    }

    private HsOfficePersonEntity givenSomeTemporaryPerson(final String createdByUser) {
        return givenSomeTemporaryPerson(createdByUser, () ->
                hsOfficePerson("some temporary person #" + RandomStringUtils.random(12)));
    }

    void exactlyThesePersonsAreReturned(final List<HsOfficePersonEntity> actualResult, final String... personLabels) {
        assertThat(actualResult)
                .extracting(HsOfficePersonEntity::getTradeName)
                .containsExactlyInAnyOrder(personLabels);
    }

    void allThesePersonsAreReturned(final List<HsOfficePersonEntity> actualResult, final String... personLabels) {
        assertThat(actualResult)
                .extracting(hsOfficePersonEntity -> hsOfficePersonEntity.toShortString())
                .contains(personLabels);
    }
}
