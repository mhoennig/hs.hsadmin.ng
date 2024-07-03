package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
class HsHostingAssetRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsHostingAssetRepository assetRepo;

    @Autowired
    HsBookingItemRepository bookingItemRepo;

    @Autowired
    HsBookingProjectRepository projectRepo;

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
    class CreateAsset {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewAsset() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = assetRepo.count();
            final var givenManagedServer = givenHostingAsset("D-1000111 default project", MANAGED_SERVER);
            final var newWebspaceBookingItem = newBookingItem(givenManagedServer.getBookingItem(), HsBookingItemType.MANAGED_WEBSPACE, "fir01");

            // when
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetEntity.builder()
                        .bookingItem(newWebspaceBookingItem)
                        .parentAsset(givenManagedServer)
                        .caption("some new managed webspace")
                        .type(MANAGED_WEBSPACE)
                        .identifier("xyz90")
                        .build();
                return toCleanup(assetRepo.save(newAsset));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsHostingAssetEntity::getUuid).isNotNull();
            assertThatAssetIsPersisted(result.returnedValue());
            assertThat(result.returnedValue().isLoaded()).isFalse();
            assertThat(assetRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenManagedServer = givenHostingAsset("D-1000111 default project", MANAGED_SERVER);
            final var newWebspaceBookingItem = newBookingItem(givenManagedServer.getBookingItem(), HsBookingItemType.MANAGED_WEBSPACE, "fir01");
            em.flush();
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetEntity.builder()
                        .bookingItem(newWebspaceBookingItem)
                        .parentAsset(givenManagedServer)
                        .type(HsHostingAssetType.MANAGED_WEBSPACE)
                        .identifier("fir00")
                        .caption("some new managed webspace")
                        .build();
                return toCleanup(assetRepo.save(newAsset));
            });

            // then
            result.assertSuccessful();
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_hosting_asset#fir00:ADMIN",
                    "hs_hosting_asset#fir00:AGENT",
                    "hs_hosting_asset#fir00:OWNER",
                    "hs_hosting_asset#fir00:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .containsExactlyInAnyOrder(fromFormatted(
                            initialGrantNames,

                            // owner
                            "{ grant role:hs_hosting_asset#fir00:OWNER to role:hs_booking_item#fir01:ADMIN by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:OWNER to role:hs_hosting_asset#vm1011:ADMIN by system and assume }",
                            "{ grant perm:hs_hosting_asset#fir00:DELETE to role:hs_hosting_asset#fir00:OWNER by system and assume }",

                            // admin
                            "{ grant role:hs_hosting_asset#fir00:ADMIN to role:hs_hosting_asset#fir00:OWNER by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:ADMIN to role:hs_booking_item#fir01:AGENT by system and assume }",
                            "{ grant perm:hs_hosting_asset#fir00:INSERT>hs_hosting_asset to role:hs_hosting_asset#fir00:ADMIN by system and assume }",
                            "{ grant perm:hs_hosting_asset#fir00:UPDATE to role:hs_hosting_asset#fir00:ADMIN by system and assume }",

                            // agent
                            "{ grant role:hs_hosting_asset#fir00:ADMIN to role:hs_hosting_asset#vm1011:AGENT by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:AGENT to role:hs_hosting_asset#fir00:ADMIN by system and assume }",

                            // tenant
                            "{ grant role:hs_booking_item#fir01:TENANT to role:hs_hosting_asset#fir00:TENANT by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:TENANT to role:hs_hosting_asset#fir00:AGENT by system and assume }",
                            "{ grant role:hs_hosting_asset#vm1011:TENANT to role:hs_hosting_asset#fir00:TENANT by system and assume }",
                            "{ grant perm:hs_hosting_asset#fir00:SELECT to role:hs_hosting_asset#fir00:TENANT by system and assume }",

                            null));
        }

        private void assertThatAssetIsPersisted(final HsHostingAssetEntity saved) {
            attempt(em, () -> {
                context("superuser-alex@hostsharing.net");
                final var found = assetRepo.findByUuid(saved.getUuid());
                assertThat(found).isNotEmpty().map(HsHostingAssetEntity::toString).get().isEqualTo(saved.toString());
            });
        }
    }

    @Nested
    class FindByDebitorUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewArbitraryAssetsOfAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = assetRepo.findAllByCriteria(null, null, MANAGED_WEBSPACE);

            // then
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAssetEntity(MANAGED_WEBSPACE, sec01, some Webspace, MANAGED_SERVER:vm1012, D-1000212:D-1000212 default project:separate ManagedWebspace)",
                    "HsHostingAssetEntity(MANAGED_WEBSPACE, fir01, some Webspace, MANAGED_SERVER:vm1011, D-1000111:D-1000111 default project:separate ManagedWebspace)",
                    "HsHostingAssetEntity(MANAGED_WEBSPACE, thi01, some Webspace, MANAGED_SERVER:vm1013, D-1000313:D-1000313 default project:separate ManagedWebspace)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedAsset() {
            // given:
            context("person-FirbySusan@example.com");
            final var projectUuid = projectRepo.findByCaption("D-1000111 default project").stream()
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = assetRepo.findAllByCriteria(projectUuid, null, null);

            // then:
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAssetEntity(MANAGED_WEBSPACE, fir01, some Webspace, MANAGED_SERVER:vm1011, D-1000111:D-1000111 default project:separate ManagedWebspace)",
                    "HsHostingAssetEntity(MANAGED_SERVER, vm1011, some ManagedServer, D-1000111:D-1000111 default project:separate ManagedServer, { monit_max_cpu_usage: 90, monit_max_ram_usage: 80, monit_max_ssd_usage: 70 } )");
        }

        @Test
        public void normalUser_canFilterAssetsRelatedToParentAsset() {
            // given
            context("superuser-alex@hostsharing.net");
            final var parentAssetUuid = assetRepo.findByIdentifier("vm1012").stream()
                    .filter(ha -> ha.getType() == MANAGED_SERVER)
                    .findAny().orElseThrow().getUuid();

            // when
            context("superuser-alex@hostsharing.net", "hs_hosting_asset#vm1012:AGENT");
            final var result = assetRepo.findAllByCriteria(null, parentAssetUuid, null);

            // then
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAssetEntity(MANAGED_WEBSPACE, sec01, some Webspace, MANAGED_SERVER:vm1012, D-1000212:D-1000212 default project:separate ManagedWebspace)");
        }
    }

    @Nested
    class UpdateAsset {

        @Test
        public void hostsharingAdmin_canUpdateArbitraryServer() {
            // given
            final var givenAssetUuid = givenSomeTemporaryAsset("D-1000111 default project", "vm1000").getUuid();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                final var foundAsset = em.find(HsHostingAssetEntity.class, givenAssetUuid);
                foundAsset.getConfig().put("CPUs", 2);
                foundAsset.getConfig().remove("SSD-storage");
                foundAsset.getConfig().put("HSD-storage", 2048);
                return toCleanup(assetRepo.save(foundAsset));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assertThatAssetActuallyInDatabase(result.returnedValue());
            }).assertSuccessful();
        }

        private void assertThatAssetActuallyInDatabase(final HsHostingAssetEntity saved) {
            final var found = assetRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(HsHostingAssetEntity::getVersion).isEqualTo(saved.getVersion());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyAsset() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return assetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedOwner_canDeleteTheirRelatedAsset() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com");
                assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isPresent();

                assetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return assetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedAdmin_canNotDeleteTheirRelatedAsset() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_hosting_asset#vm1000:ADMIN");
                assertThat(assetRepo.findByUuid(givenAsset.getUuid())).isPresent();

                assetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_hosting_asset");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return assetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAnAssetAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return assetRepo.deleteByUuid(givenAsset.getUuid());
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
                    where targettable = 'hs_hosting_asset';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating hosting-asset test-data D-1000111 default project, hs_hosting_asset, INSERT]",
                "[creating hosting-asset test-data D-1000212 default project, hs_hosting_asset, INSERT]",
                "[creating hosting-asset test-data D-1000313 default project, hs_hosting_asset, INSERT]");
    }

    private HsHostingAssetEntity givenSomeTemporaryAsset(final String projectCaption, final String identifier) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenBookingItem("D-1000111 default project", "test CloudServer");
            final var newAsset = HsHostingAssetEntity.builder()
                    .bookingItem(givenBookingItem)
                    .type(CLOUD_SERVER)
                    .identifier(identifier)
                    .caption("some temp cloud asset")
                    .config(Map.ofEntries(
                            entry("CPUs", 1),
                            entry("SSD-storage", 256)))
                    .build();

            return toCleanup(assetRepo.save(newAsset));
        }).assertSuccessful().returnedValue();
    }

    HsBookingItemEntity givenBookingItem(final String projectCaption, final String bookingItemCaption) {
        return bookingItemRepo.findByCaption(bookingItemCaption).stream()
                .filter(i -> i.getRelatedProject().getCaption().equals(projectCaption))
                .findAny().orElseThrow();
    }

    HsHostingAssetEntity givenHostingAsset(final String projectCaption, final HsHostingAssetType type) {
        final var givenProject = projectRepo.findByCaption(projectCaption).stream()
                .findAny().orElseThrow();
        return assetRepo.findAllByCriteria(givenProject.getUuid(), null, type).stream()
                .findAny().orElseThrow();
    }

    HsBookingItemEntity newBookingItem(
            final HsBookingItemEntity parentBookingItem,
            final HsBookingItemType type,
            final String caption) {
        final var newBookingItem = HsBookingItemEntity.builder()
                .parentItem(parentBookingItem)
                .type(type)
                .caption(caption)
                .build();
        return toCleanup(bookingItemRepo.save(newBookingItem));
    }

    void exactlyTheseAssetsAreReturned(
            final List<HsHostingAssetEntity> actualResult,
            final String... serverNames) {
        assertThat(actualResult)
                .extracting(HsHostingAssetEntity::toString)
                .extracting(input -> input.replaceAll("\\s+", " "))
                .extracting(input -> input.replaceAll("\"", ""))
                .containsExactlyInAnyOrder(serverNames);
    }
}
