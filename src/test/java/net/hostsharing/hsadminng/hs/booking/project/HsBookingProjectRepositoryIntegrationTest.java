package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.Array;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsBookingProjectRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsBookingProjectRepository bookingProjectRepo;

    @Autowired
    HsBookingProjectRepository projectRepo;

    @Autowired
    HsBookingDebitorRepository debitorRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateBookingProject {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewBookingProject() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = bookingProjectRepo.count();
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).get(0);

            // when
            final var result = attempt(em, () -> {
                final var newBookingProject = HsBookingProjectEntity.builder()
                        .debitor(givenDebitor)
                        .caption("some new booking project")
                        .build();
                return toCleanup(bookingProjectRepo.save(newBookingProject));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsBookingProjectEntity::getUuid).isNotNull();
            assertThatBookingProjectIsPersisted(result.returnedValue());
            assertThat(bookingProjectRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).get(0);
                final var newBookingProject = HsBookingProjectEntity.builder()
                        .debitor(givenDebitor)
                        .caption("some new booking project")
                        .build();
                return toCleanup(bookingProjectRepo.save(newBookingProject));
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_booking_project#D-1000111-somenewbookingproject:ADMIN",
                    "hs_booking_project#D-1000111-somenewbookingproject:AGENT",
                    "hs_booking_project#D-1000111-somenewbookingproject:OWNER",
                    "hs_booking_project#D-1000111-somenewbookingproject:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(fromFormatted(
                            initialGrantNames,

                            // global-admin
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:DELETE to role:global#global:ADMIN by system and assume }",

                            // owner
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN to role:hs_booking_project#D-1000111-somenewbookingproject:OWNER by system and assume }",

                            // admin
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:AGENT to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:UPDATE to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:INSERT>hs_booking_item to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",

                            // agent
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:OWNER to role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT by system and assume }",
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:TENANT to role:hs_booking_project#D-1000111-somenewbookingproject:AGENT by system and assume }",

                            // tenant
                            "{ grant role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:TENANT to role:hs_booking_project#D-1000111-somenewbookingproject:TENANT by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:SELECT to role:hs_booking_project#D-1000111-somenewbookingproject:TENANT by system and assume }",

                            null));
        }

        private void assertThatBookingProjectIsPersisted(final HsBookingProjectEntity saved) {
            final var found = bookingProjectRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().map(HsBookingProjectEntity::toString).get().isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindByDebitorUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllBookingProjectsOfArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var debitorUuid = debitorRepo.findByDebitorNumber(1000212).stream()
                    .findAny().orElseThrow().getUuid();

            // when
            final var result = bookingProjectRepo.findAllByDebitorUuid(debitorUuid);

            // then
            allTheseBookingProjectsAreReturned(
                    result,
                    "HsBookingProjectEntity(D-1000212, D-1000212 default project)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedBookingProjects() {
            // given:
            context("person-FirbySusan@example.com");
            final var debitorUuid = debitorRepo.findByDebitorNumber(1000111).stream()
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = bookingProjectRepo.findAllByDebitorUuid(debitorUuid);

            // then:
            exactlyTheseBookingProjectsAreReturned(
                    result,
                    "HsBookingProjectEntity(D-1000111, D-1000111 default project)");
        }
    }

    @Nested
    class UpdateBookingProject {

        @Test
        public void hostsharingAdmin_canUpdateArbitraryBookingProject() {
            // given
            final var givenBookingProjectUuid = givenSomeTemporaryBookingProject(1000111).getUuid();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                final var foundBookingProject = em.find(HsBookingProjectEntity.class, givenBookingProjectUuid);
                return toCleanup(bookingProjectRepo.save(foundBookingProject));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assertThatBookingProjectActuallyInDatabase(result.returnedValue());
            }).assertSuccessful();
        }

        private void assertThatBookingProjectActuallyInDatabase(final HsBookingProjectEntity saved) {
            final var found = bookingProjectRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyBookingProject() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                bookingProjectRepo.deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return bookingProjectRepo.findByUuid(givenBookingProject.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedBookingProject() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com");
                assertThat(bookingProjectRepo.findByUuid(givenBookingProject.getUuid())).isPresent();

                bookingProjectRepo.deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_booking_project");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return bookingProjectRepo.findByUuid(givenBookingProject.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingABookingProjectAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return bookingProjectRepo.deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_booking_project';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating booking-project test-data 1000111, hs_booking_project, INSERT]",
                "[creating booking-project test-data 1000212, hs_booking_project, INSERT]",
                "[creating booking-project test-data 1000313, hs_booking_project, INSERT]");
    }

    private HsBookingProjectEntity givenSomeTemporaryBookingProject(final int debitorNumber) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findByDebitorNumber(debitorNumber).get(0);
            final var newBookingProject = HsBookingProjectEntity.builder()
                    .debitor(givenDebitor)
                    .caption("some temp project")
                    .build();

            return toCleanup(bookingProjectRepo.save(newBookingProject));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseBookingProjectsAreReturned(
            final List<HsBookingProjectEntity> actualResult,
            final String... bookingProjectNames) {
        assertThat(actualResult)
                .extracting(HsBookingProjectEntity::toString)
                .containsExactlyInAnyOrder(bookingProjectNames);
    }

    void allTheseBookingProjectsAreReturned(
            final List<HsBookingProjectEntity> actualResult,
            final String... bookingProjectNames) {
        assertThat(actualResult)
                .extracting(HsBookingProjectEntity::toString)
                .contains(bookingProjectNames);
    }
}
