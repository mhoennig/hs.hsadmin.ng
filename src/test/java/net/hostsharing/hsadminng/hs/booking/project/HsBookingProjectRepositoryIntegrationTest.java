package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsBookingProjectRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsBookingProjectRealRepository realProjectRepo;

    @Autowired
    HsBookingProjectRbacRepository rbacProjectRepo;

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

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp, targetdelta->>'caption'
                    from base.tx_journal_v
                    where targettable = 'hs_booking_project';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating booking-project test-data, hs_booking_project, INSERT, D-1000111 default project]",
                "[creating booking-project test-data, hs_booking_project, INSERT, D-1000212 default project]",
                "[creating booking-project test-data, hs_booking_project, INSERT, D-1000313 default project]");
    }

    @Test
    public void historizationIsAvailable() {
        // given
        final String nativeQuerySql = """
                select count(*)
                    from hs_booking_project_hv ha;
                """;

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countBefore = (Integer) query.getSingleResult();

        // then
        assertThat(countBefore).as("hs_booking_project_hv should not contain rows for a timestamp in the past").isEqualTo(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()));
        em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countAfter = (Integer) query.getSingleResult();

        // then
        assertThat(countAfter).as("hs_booking_project_hv should contain rows for a timestamp in the future").isGreaterThan(1);
    }

    @Nested
    class CreateBookingProject {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewBookingProject() {
            // given
            context("superuser-alex@hostsharing.net"); // TODO.test: remove once we have a realDebitorRepo
            final var count = realProjectRepo.count();
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).get(0);

            // when
            final var result = attempt(em, () -> {
                final var newBookingProject = HsBookingProjectRbacEntity.builder()
                        .debitor(givenDebitor)
                        .caption("some new booking project")
                        .build();
                return toCleanup(rbacProjectRepo.save(newBookingProject));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsBookingProject::getUuid).isNotNull();
            assertThatBookingProjectIsPersisted(result.returnedValue());
            assertThat(realProjectRepo.count()).isEqualTo(count + 1);
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
                final var newBookingProject = HsBookingProjectRbacEntity.builder()
                        .debitor(givenDebitor)
                        .caption("some new booking project")
                        .build();
                return toCleanup(rbacProjectRepo.save(newBookingProject));
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

                            // rbacgGlobal-admin
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:DELETE to role:rbac.global#global:ADMIN by system and assume }",

                            // owner
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN to role:hs_booking_project#D-1000111-somenewbookingproject:OWNER by system and assume }",

                            // admin
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:AGENT to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:UPDATE to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:INSERT>hs_booking_item to role:hs_booking_project#D-1000111-somenewbookingproject:ADMIN by system and assume }",

                            // agent
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:OWNER to role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT by system }",
                            "{ grant role:hs_booking_project#D-1000111-somenewbookingproject:TENANT to role:hs_booking_project#D-1000111-somenewbookingproject:AGENT by system and assume }",

                            // tenant
                            "{ grant role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:TENANT to role:hs_booking_project#D-1000111-somenewbookingproject:TENANT by system and assume }",
                            "{ grant perm:hs_booking_project#D-1000111-somenewbookingproject:SELECT to role:hs_booking_project#D-1000111-somenewbookingproject:TENANT by system and assume }",

                            null));
        }

        private void assertThatBookingProjectIsPersisted(final HsBookingProject saved) {
            final var found = rbacProjectRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().map(HsBookingProject::toString).get().isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindByDebitorUuid {

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void globalAdmin_withoutAssumedRole_canViewAllBookingProjectsOfArbitraryDebitor(final TestCase testCase) {
            // given
            context("superuser-alex@hostsharing.net");
            final var debitorUuid = debitorRepo.findByDebitorNumber(1000212).stream()
                    .findAny().orElseThrow().getUuid();

            // when
            final var result = repoUnderTest(testCase).findAllByDebitorUuid(debitorUuid);

            // then
            allTheseBookingProjectsAreReturned(
                    result,
                    "HsBookingProject(D-1000212, D-1000212 default project)");
        }

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void packetAgent_canViewOnlyRelatedBookingProjects(final TestCase testCase) {

            // given:
            context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var debitorUuid = debitorRepo.findByDebitorNumber(1000111).stream()
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = repoUnderTest(testCase).findAllByDebitorUuid(debitorUuid);

            // then:
            assertResult(testCase, result,
                    "HsBookingProject(D-1000111, D-1000111 default project)");
        }
    }

    @Nested
    class UpdateBookingProject {

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void bookingProjectAdmin_canUpdateArbitraryBookingProject(final TestCase testCase) {
            // given
            final var givenBookingProjectUuid = givenSomeTemporaryBookingProject(1000111).getUuid();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-sometempproject:ADMIN");
                final var foundBookingProject = em.find(HsBookingProjectRbacEntity.class, givenBookingProjectUuid);
                foundBookingProject.setCaption("updated caption");
                return toCleanup(repoUnderTest(testCase).save(foundBookingProject));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue().getCaption()).isEqualTo("updated caption");
            assertThatBookingProjectActuallyInDatabase(result.returnedValue());
        }

        private void assertThatBookingProjectActuallyInDatabase(final HsBookingProject saved) {
            jpaAttempt.transacted(() -> {
                final var found = realProjectRepo.findByUuid(saved.getUuid());
                assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                        .extracting(Object::toString).isEqualTo(saved.toString());
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void globalAdmin_withoutAssumedRole_canDeleteAnyBookingProject(final TestCase testCase) {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                repoUnderTest(testCase).deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return rbacProjectRepo.findByUuid(givenBookingProject.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedBookingProject() {
            // given
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-sometempproject:AGENT");
                assertThat(rbacProjectRepo.findByUuid(givenBookingProject.getUuid())).isPresent();

                repoUnderTest(TestCase.RBAC).deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_booking_project");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return rbacProjectRepo.findByUuid(givenBookingProject.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void deletingABookingProjectAlsoDeletesRelatedRolesAndGrants(final TestCase testCase) {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenBookingProject = givenSomeTemporaryBookingProject(1000111);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return repoUnderTest(testCase).deleteByUuid(givenBookingProject.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsBookingProjectRealEntity givenSomeTemporaryBookingProject(final int debitorNumber) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findByDebitorNumber(debitorNumber).get(0);
            final var newBookingProject = HsBookingProjectRealEntity.builder()
                    .debitor(givenDebitor)
                    .caption("some temp project")
                    .build();

            return toCleanup(realProjectRepo.save(newBookingProject));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseBookingProjectsAreReturned(
            final List<? extends HsBookingProject> actualResult,
            final String... bookingProjectNames) {
        assertThat(actualResult)
                .extracting(HsBookingProject::toString)
                .containsExactlyInAnyOrder(bookingProjectNames);
    }

    void allTheseBookingProjectsAreReturned(
            final List<? extends HsBookingProject> actualResult,
            final String... bookingProjectNames) {
        assertThat(actualResult)
                .extracting(HsBookingProject::toString)
                .contains(bookingProjectNames);
    }

    private HsBookingProjectRepository repoUnderTest(final TestCase testCase) {
        return testCase.repo(HsBookingProjectRepositoryIntegrationTest.this);
    }

    private void assertResult(
            final TestCase testCase,
            final List<? extends HsBookingProject> actualResult,
            final String... expectedProjects) {
        testCase.assertResult(HsBookingProjectRepositoryIntegrationTest.this, actualResult, expectedProjects);
    }

    enum TestCase {
        REAL {
            @Override
            HsBookingProjectRepository repo(final HsBookingProjectRepositoryIntegrationTest test) {
                return test.realProjectRepo;
            }

            @Override
            void assertResult(
                    final HsBookingProjectRepositoryIntegrationTest test,
                    final List<? extends HsBookingProject> result,
                    final String... expectedProjects) {
                test.allTheseBookingProjectsAreReturned(result, expectedProjects);
            }
        },
        RBAC {
            @Override
            HsBookingProjectRepository repo(final HsBookingProjectRepositoryIntegrationTest test) {
                return test.rbacProjectRepo;
            }

            @Override
            void assertResult(
                    final HsBookingProjectRepositoryIntegrationTest test,
                    final List<? extends HsBookingProject> result,
                    final String... expectedProjects) {
                test.exactlyTheseBookingProjectsAreReturned(result, expectedProjects);
            }
        };

        abstract HsBookingProjectRepository repo(final HsBookingProjectRepositoryIntegrationTest test);

        abstract void assertResult(final HsBookingProjectRepositoryIntegrationTest test, final List<? extends HsBookingProject> result, final String... expectedProjects);
    }
}
