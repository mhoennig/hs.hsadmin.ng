package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsBookingItemRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsBookingItemRepository bookingItemRepo;

    @Autowired
    HsBookingProjectRepository projectRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

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
    class CreateBookingItem {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewBookingItem() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = bookingItemRepo.count();
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenProject = projectRepo.findAllByDebitorUuid(givenDebitor.getUuid()).get(0);

            // when
            final var result = attempt(em, () -> {
                final var newBookingItem = HsBookingItemEntity.builder()
                        .project(givenProject)
                        .type(HsBookingItemType.CLOUD_SERVER)
                        .caption("some new booking item")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(bookingItemRepo.save(newBookingItem));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsBookingItemEntity::getUuid).isNotNull();
            assertThatBookingItemIsPersisted(result.returnedValue());
            assertThat(bookingItemRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> {
                final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
                final var givenProject = projectRepo.findAllByDebitorUuid(givenDebitor.getUuid()).get(0);
                final var newBookingItem = HsBookingItemEntity.builder()
                        .project(givenProject)
                        .type(MANAGED_WEBSPACE)
                        .caption("some new booking item")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(bookingItemRepo.save(newBookingItem));
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_booking_item#somenewbookingitem:ADMIN",
                    "hs_booking_item#somenewbookingitem:AGENT",
                    "hs_booking_item#somenewbookingitem:OWNER",
                    "hs_booking_item#somenewbookingitem:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .containsExactlyInAnyOrder(fromFormatted(
                            initialGrantNames,

                            // global-admin
                            "{ grant perm:hs_booking_item#somenewbookingitem:INSERT>hs_booking_item to role:hs_booking_item#somenewbookingitem:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_item#somenewbookingitem:DELETE to role:global#global:ADMIN by system and assume }",

                            // owner
                            "{ grant role:hs_booking_item#somenewbookingitem:OWNER to role:hs_booking_project#D-1000111-D-1000111defaultproject:AGENT by system and assume }",

                            // admin
                            "{ grant perm:hs_booking_item#somenewbookingitem:UPDATE to role:hs_booking_item#somenewbookingitem:ADMIN by system and assume }",
                            "{ grant role:hs_booking_item#somenewbookingitem:ADMIN to role:hs_booking_item#somenewbookingitem:OWNER by system and assume }",

                            // agent
                            "{ grant role:hs_booking_item#somenewbookingitem:AGENT to role:hs_booking_item#somenewbookingitem:ADMIN by system and assume }",

                            // tenant
                            "{ grant role:hs_booking_item#somenewbookingitem:TENANT to role:hs_booking_item#somenewbookingitem:AGENT by system and assume }",
                            "{ grant perm:hs_booking_item#somenewbookingitem:SELECT to role:hs_booking_item#somenewbookingitem:TENANT by system and assume }",
                            "{ grant role:hs_booking_project#D-1000111-D-1000111defaultproject:TENANT to role:hs_booking_item#somenewbookingitem:TENANT by system and assume }",
                            null));
        }

        private void assertThatBookingItemIsPersisted(final HsBookingItemEntity saved) {
            final var found = bookingItemRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().map(HsBookingItemEntity::toString).get().isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindByDebitorUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllBookingItemsOfArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var projectUuid = debitorRepo.findDebitorByDebitorNumber(1000212).stream()
                    .map(d -> projectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(List::stream)
                    .findAny().orElseThrow().getUuid();

            // when
            final var result = bookingItemRepo.findAllByProjectUuid(projectUuid);

            // then
            allTheseBookingItemsAreReturned(
                    result,
                    "HsBookingItemEntity(MANAGED_SERVER, separate ManagedServer, D-1000212:D-1000212 default project, [2022-10-01,), { CPU: 2, RAM: 8, SSD: 500, Traffic: 500 })",
                    "HsBookingItemEntity(MANAGED_WEBSPACE, separate ManagedWebspace, D-1000212:D-1000212 default project, [2022-10-01,), { Daemons: 0, Multi: 1, SSD: 100, Traffic: 50 })",
                    "HsBookingItemEntity(PRIVATE_CLOUD, some PrivateCloud, D-1000212:D-1000212 default project, [2024-04-01,), { CPU: 10, HDD: 10000, RAM: 32, SSD: 4000, Traffic: 2000 })");
             assertThat(result.stream().filter(bi -> bi.getRelatedHostingAsset()!=null).findAny())
                     .as("at least one relatedProject expected, but none found => fetching relatedProject does not work")
                     .isNotEmpty();
        }

        @Test
        public void normalUser_canViewOnlyRelatedBookingItems() {
            // given:
            context("person-FirbySusan@example.com");
            final var debitor = debitorRepo.findDebitorByDebitorNumber(1000111);
            context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:OWNER");
            final var projectUuid = debitor.stream()
                    .map(d -> projectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(List::stream)
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = bookingItemRepo.findAllByProjectUuid(projectUuid);

            // then:
            exactlyTheseBookingItemsAreReturned(
                    result,
                    "HsBookingItemEntity(MANAGED_SERVER, separate ManagedServer, D-1000111:D-1000111 default project, [2022-10-01,), { CPU : 2, RAM : 8, SSD : 500, Traffic : 500 })",
                    "HsBookingItemEntity(MANAGED_WEBSPACE, separate ManagedWebspace, D-1000111:D-1000111 default project, [2022-10-01,), { Daemons : 0, Multi : 1, SSD : 100, Traffic : 50 })",
                    "HsBookingItemEntity(PRIVATE_CLOUD, some PrivateCloud, D-1000111:D-1000111 default project, [2024-04-01,), { CPU : 10, HDD : 10000, RAM : 32, SSD : 4000, Traffic : 2000 })");
        }
    }

    @Nested
    class UpdateBookingItem {

        @Test
        public void hostsharingAdmin_canUpdateArbitraryBookingItem() {
            // given
            final var givenBookingItemUuid = givenSomeTemporaryBookingItem("D-1000111 default project").getUuid();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
                final var foundBookingItem = em.find(HsBookingItemEntity.class, givenBookingItemUuid);
                foundBookingItem.getResources().put("CPU", 2);
                foundBookingItem.getResources().remove("SSD-storage");
                foundBookingItem.getResources().put("HSD-storage", 2048);
                foundBookingItem.setValidity(Range.closedOpen(
                        LocalDate.parse("2019-05-17"), LocalDate.parse("2023-01-01")));
                return toCleanup(bookingItemRepo.save(foundBookingItem));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assertThatBookingItemActuallyInDatabase(result.returnedValue());
            }).assertSuccessful();
        }

        private void assertThatBookingItemActuallyInDatabase(final HsBookingItemEntity saved) {
            final var found = bookingItemRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(HsBookingItemEntity::getResources)
                    .extracting(Object::toString)
                    .isEqualTo(saved.getResources().toString());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyBookingItem() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBookingItem = givenSomeTemporaryBookingItem("D-1000111 default project");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                bookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return bookingItemRepo.findByUuid(givenBookingItem.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedBookingItem() {
            // given
            context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var givenBookingItem = givenSomeTemporaryBookingItem("D-1000111 default project");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
                assertThat(bookingItemRepo.findByUuid(givenBookingItem.getUuid())).isPresent();

                bookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_booking_item");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return bookingItemRepo.findByUuid(givenBookingItem.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingABookingItemAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenBookingItem = givenSomeTemporaryBookingItem("D-1000111 default project");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return bookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
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
                    where targettable = 'hs_booking_item';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating booking-item test-data 1000111, hs_booking_item, INSERT]",
                "[creating booking-item test-data 1000212, hs_booking_item, INSERT]",
                "[creating booking-item test-data 1000313, hs_booking_item, INSERT]");
    }

    private HsBookingItemEntity givenSomeTemporaryBookingItem(final String projectCaption) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenProject = projectRepo.findByCaption(projectCaption).stream()
                    .findAny().orElseThrow();
            final var newBookingItem = HsBookingItemEntity.builder()
                    .project(givenProject)
                    .type(MANAGED_SERVER)
                    .caption("some temp booking item")
                    .validity(Range.closedOpen(
                            LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                    .resources(Map.ofEntries(
                            entry("CPU", 1),
                            entry("SSD-storage", 256)))
                    .build();

            return toCleanup(bookingItemRepo.save(newBookingItem));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseBookingItemsAreReturned(
            final List<HsBookingItemEntity> actualResult,
            final String... bookingItemNames) {
        assertThat(actualResult)
                .extracting(HsBookingItemEntity::toString)
                .extracting(string-> string.replaceAll("\\s+", " "))
                .extracting(string-> string.replaceAll("\"", ""))
                .containsExactlyInAnyOrder(bookingItemNames);
    }

    void allTheseBookingItemsAreReturned(final List<HsBookingItemEntity> actualResult, final String... bookingItemNames) {
        assertThat(actualResult)
                .extracting(HsBookingItemEntity::toString)
                .extracting(string -> string.replaceAll("\\s+", " "))
                .extracting(string -> string.replaceAll("\"", ""))
                .extracting(string -> string.replaceAll(" : ", ": "))
                .contains(bookingItemNames);
    }
}
