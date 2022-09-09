package net.hostsharing.hsadminng.hs.admin.person;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.internal.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.admin.person.TestHsAdminPerson.hsAdminPerson;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantDisplayExtractor.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleNameExtractor.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsAdminPersonRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsAdminPersonRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsAdminPersonRepository personRepo;

    @Autowired
    RbacRoleRepository roleRepo;

    @Autowired
    RbacGrantRepository grantRepo;

    @Autowired
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
            context("alex@hostsharing.net");
            final var count = personRepo.count();

            // when

            final var result = attempt(em, () -> personRepo.save(
                    hsAdminPerson("a new person")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminPersonEntity::getUuid).isNotNull();
            assertThatPersonIsPersisted(result.returnedValue());
            assertThat(personRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewPerson() {
            // given
            context("drew@hostsharing.org");
            final var count = personRepo.count();

            // when
            final var result = attempt(em, () -> personRepo.save(
                    hsAdminPerson("another new person")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminPersonEntity::getUuid).isNotNull();
            assertThatPersonIsPersisted(result.returnedValue());
            assertThat(personRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("drew@hostsharing.org");
            final var count = personRepo.count();
            final var initialRoleCount = roleRepo.findAll().size();
            final var initialGrantCount = grantRepo.findAll().size();

            // when
            attempt(em, () -> personRepo.save(
                    hsAdminPerson("another new person"))
            ).assumeSuccessful();

            // then
            final var roles = roleRepo.findAll();
            assertThat(roleNamesOf(roles)).containsAll(List.of(
                    "hs_admin_person#anothernewperson.owner",
                    "hs_admin_person#anothernewperson.tenant"));
            assertThat(roles.size()).as("invalid number of roles created")
                    .isEqualTo(initialRoleCount + 2);
            final var grants = grantRepo.findAll();
            assertThat(grantDisplaysOf(grants)).containsAll(List.of(
                    "{ grant assumed role hs_admin_person#anothernewperson.owner to user drew@hostsharing.org by role global#global.admin }"));
            assertThat(grants.size()).as("invalid number of grants created")
                    .isEqualTo(initialGrantCount + 1);
        }

        private void assertThatPersonIsPersisted(final HsAdminPersonEntity saved) {
            final var found = personRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllPersons {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPersons() {
            // given
            context("alex@hostsharing.net");

            // when
            final var result = personRepo.findPersonByOptionalNameLike(null);

            // then
            allThesePersonsAreReturned(
                    result,
                    "Peter, Smith",
                    "Rockshop e.K.",
                    "Ostfriesische Stahlhandel OHG",
                    "Erbengemeinschaft Bessler");
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
            context("alex@hostsharing.net", null);

            // when
            final var result = personRepo.findPersonByOptionalNameLike("Rockshop");

            // then
            exactlyThesePersonsAreReturned(result, "Rockshop e.K.");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnPerson() {
            // given:
            final var givenPerson = givenSomeTemporaryPerson("drew@hostsharing.org");

            // when:
            context("drew@hostsharing.org");
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
            final var givenPerson = givenSomeTemporaryPerson("drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                return personRepo.findPersonByOptionalNameLike(givenPerson.getTradeName());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canDeleteAPersonCreatedByItself() {
            // given
            final var givenPerson = givenSomeTemporaryPerson("drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("drew@hostsharing.org", null);
                personRepo.deleteByUuid(givenPerson.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                return personRepo.findPersonByOptionalNameLike(givenPerson.getTradeName());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void deletingAPersonAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("drew@hostsharing.org", null);
            final var initialRoleCount = roleRepo.findAll().size();
            final var initialGrantCount = grantRepo.findAll().size();
            final var givenPerson = givenSomeTemporaryPerson("drew@hostsharing.org");
            assumeThat(roleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleCount + 2);
            assumeThat(grantRepo.findAll().size()).as("unexpected number of grants created")
                    .isEqualTo(initialGrantCount + 1);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("drew@hostsharing.org", null);
                personRepo.deleteByUuid(givenPerson.getUuid());
            }).assumeSuccessful();

            // then
            assertThat(roleRepo.findAll().size()).as("invalid number of roles deleted")
                    .isEqualTo(initialRoleCount);
            assertThat(grantRepo.findAll().size()).as("invalid number of grants revoked")
                    .isEqualTo(initialGrantCount);
        }
    }

    @AfterEach
    void cleanup() {
        context("alex@hostsharing.net", null);
        final var result = personRepo.findPersonByOptionalNameLike("some temporary person");
        result.forEach(tempPerson -> {
            System.out.println("DELETING person: " + tempPerson.getDisplayName());
            personRepo.deleteByUuid(tempPerson.getUuid());
        });
    }

    private HsAdminPersonEntity givenSomeTemporaryPerson(
            final String createdByUser,
            Supplier<HsAdminPersonEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return personRepo.save(entitySupplier.get());
        }).assumeSuccessful().returnedValue();
    }

    private HsAdminPersonEntity givenSomeTemporaryPerson(final String createdByUser) {
        return givenSomeTemporaryPerson(createdByUser, () ->
                hsAdminPerson("some temporary person #" + RandomString.make(12)));
    }

    void exactlyThesePersonsAreReturned(final List<HsAdminPersonEntity> actualResult, final String... personLabels) {
        assertThat(actualResult)
                .extracting(HsAdminPersonEntity::getTradeName)
                .containsExactlyInAnyOrder(personLabels);
    }

    void allThesePersonsAreReturned(final List<HsAdminPersonEntity> actualResult, final String... personLabels) {
        assertThat(actualResult)
                .extracting(HsAdminPersonEntity::getDisplayName)
                .contains(personLabels);
    }
}
