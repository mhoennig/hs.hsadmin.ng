package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsBookingItemRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsBookingItemRbacRepository rbacBookingItemRepo;

    @Autowired
    HsBookingProjectRealRepository realProjectRepo;

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

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp, targetdelta->>'caption'
                    from base.tx_journal_v
                    where targettable = 'hs_booking_item';
                """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating booking-item test-data, hs_booking_item, INSERT, prod CloudServer]",
                "[creating booking-item test-data, hs_booking_item, INSERT, separate ManagedServer]",
                "[creating booking-item test-data, hs_booking_item, INSERT, separate ManagedWebspace]",
                "[creating booking-item test-data, hs_booking_item, INSERT, some ManagedServer]",
                "[creating booking-item test-data, hs_booking_item, INSERT, some ManagedWebspace]",
                "[creating booking-item test-data, hs_booking_item, INSERT, some PrivateCloud]",
                "[creating booking-item test-data, hs_booking_item, INSERT, test CloudServer]");
    }

    @Test
    public void historizationIsAvailable() {
        // given
        final String nativeQuerySql = """
                select count(*)
                    from hs_booking_item_hv ha;
                """;

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countBefore = (Integer) query.getSingleResult();

        // then
        assertThat(countBefore).as("hs_booking_item should not contain rows for a timestamp in the past").isEqualTo(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()));
        em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countAfter = (Integer) query.getSingleResult();

        // then
        assertThat(countAfter).as("hs_booking_item should contain rows for a timestamp in the future").isGreaterThan(1);
    }

    @Nested
    class CreateBookingItem {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewBookingItem() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = rbacBookingItemRepo.count();
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenProject = realProjectRepo.findAllByDebitorUuid(givenDebitor.getUuid()).get(0);

            // when
            final var result = attempt(em, () -> {
                final var newBookingItem = HsBookingItemRbacEntity.builder()
                        .project(givenProject)
                        .type(HsBookingItemType.CLOUD_SERVER)
                        .caption("some new booking item")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(rbacBookingItemRepo.save(newBookingItem));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsBookingItem::getUuid).isNotNull();
            assertThatBookingItemIsPersisted(result.returnedValue());
            assertThat(rbacBookingItemRepo.count()).isEqualTo(count + 1);
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
                final var givenProject = realProjectRepo.findAllByDebitorUuid(givenDebitor.getUuid()).get(0);
                final var newBookingItem = HsBookingItemRbacEntity.builder()
                        .project(givenProject)
                        .type(MANAGED_WEBSPACE)
                        .caption("some new booking item")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(rbacBookingItemRepo.save(newBookingItem));
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

                            // rbac.global-admin
                            "{ grant perm:hs_booking_item#somenewbookingitem:INSERT>hs_booking_item to role:hs_booking_item#somenewbookingitem:ADMIN by system and assume }",
                            "{ grant perm:hs_booking_item#somenewbookingitem:DELETE to role:rbac.global#global:ADMIN by system and assume }",

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

        private void assertThatBookingItemIsPersisted(final HsBookingItem saved) {
            final var found = rbacBookingItemRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().map(HsBookingItem::toString).get().isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindByDebitorUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllBookingItemsOfArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var projectUuid = debitorRepo.findDebitorByDebitorNumber(1000212).stream()
                    .map(d -> realProjectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(List::stream)
                    .findAny().orElseThrow().getUuid();

            // when
            final var result = rbacBookingItemRepo.findAllByProjectUuid(projectUuid);

            // then
            allTheseBookingItemsAreReturned(
                    result,
                    "HsBookingItem(MANAGED_SERVER, separate ManagedServer, D-1000212:D-1000212 default project, [2022-10-01,), { CPU: 2, RAM: 8, SSD: 500, Traffic: 500 })",
                    "HsBookingItem(MANAGED_WEBSPACE, separate ManagedWebspace, D-1000212:D-1000212 default project, [2022-10-01,), { Daemons: 0, Multi: 1, SSD: 100, Traffic: 50 })",
                    "HsBookingItem(PRIVATE_CLOUD, some PrivateCloud, D-1000212:D-1000212 default project, [2024-04-01,), { CPU: 10, HDD: 10000, RAM: 32, SSD: 4000, Traffic: 2000 })");
        }

        @Test
        public void normalUser_canViewOnlyRelatedBookingItems() {
            // given:
            context("person-FirbySusan@example.com");
            final var debitor = debitorRepo.findDebitorByDebitorNumber(1000111);
            context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:OWNER");
            final var projectUuid = debitor.stream()
                    .map(d -> realProjectRepo.findAllByDebitorUuid(d.getUuid()))
                    .flatMap(List::stream)
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = rbacBookingItemRepo.findAllByProjectUuid(projectUuid);

            // then:
            exactlyTheseBookingItemsAreReturned(
                    result,
                    "HsBookingItem(MANAGED_SERVER, separate ManagedServer, D-1000111:D-1000111 default project, [2022-10-01,), { CPU : 2, RAM : 8, SSD : 500, Traffic : 500 })",
                    "HsBookingItem(MANAGED_WEBSPACE, separate ManagedWebspace, D-1000111:D-1000111 default project, [2022-10-01,), { Daemons : 0, Multi : 1, SSD : 100, Traffic : 50 })",
                    "HsBookingItem(PRIVATE_CLOUD, some PrivateCloud, D-1000111:D-1000111 default project, [2024-04-01,), { CPU : 10, HDD : 10000, RAM : 32, SSD : 4000, Traffic : 2000 })");
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
                final var foundBookingItem = em.find(HsBookingItemRbacEntity.class, givenBookingItemUuid);
                foundBookingItem.getResources().put("CPU", 2);
                foundBookingItem.getResources().remove("SSD-storage");
                foundBookingItem.getResources().put("HSD-storage", 2048);
                foundBookingItem.setValidity(Range.closedOpen(
                        LocalDate.parse("2019-05-17"), LocalDate.parse("2023-01-01")));
                return toCleanup(rbacBookingItemRepo.save(foundBookingItem));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assertThatBookingItemActuallyInDatabase(result.returnedValue());
            }).assertSuccessful();
        }

        private void assertThatBookingItemActuallyInDatabase(final HsBookingItem saved) {
            final var found = rbacBookingItemRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(HsBookingItem::getResources)
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
                rbacBookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return rbacBookingItemRepo.findByUuid(givenBookingItem.getUuid());
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
                assertThat(rbacBookingItemRepo.findByUuid(givenBookingItem.getUuid())).isPresent();

                rbacBookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_booking_item");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return rbacBookingItemRepo.findByUuid(givenBookingItem.getUuid());
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
                return rbacBookingItemRepo.deleteByUuid(givenBookingItem.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsBookingItem givenSomeTemporaryBookingItem(final String projectCaption) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenProject = realProjectRepo.findByCaption(projectCaption).stream()
                    .findAny().orElseThrow();
            final var newBookingItem = HsBookingItemRbacEntity.builder()
                    .project(givenProject)
                    .type(MANAGED_SERVER)
                    .caption("some temp booking item")
                    .validity(Range.closedOpen(
                            LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                    .resources(Map.ofEntries(
                            entry("CPU", 1),
                            entry("SSD-storage", 256)))
                    .build();

            return toCleanup(rbacBookingItemRepo.save(newBookingItem));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseBookingItemsAreReturned(
            final List<? extends HsBookingItem> actualResult,
            final String... bookingItemNames) {
        assertThat(actualResult)
                .extracting(HsBookingItem::toString)
                .extracting(string-> string.replaceAll("\\s+", " "))
                .extracting(string-> string.replaceAll("\"", ""))
                .containsExactlyInAnyOrder(bookingItemNames);
    }

    void allTheseBookingItemsAreReturned(final List<? extends HsBookingItem> actualResult, final String... bookingItemNames) {
        assertThat(actualResult)
                .extracting(HsBookingItem::toString)
                .extracting(string -> string.replaceAll("\\s+", " "))
                .extracting(string -> string.replaceAll("\"", ""))
                .extracting(string -> string.replaceAll(" : ", ": "))
                .contains(bookingItemNames);
    }
}
