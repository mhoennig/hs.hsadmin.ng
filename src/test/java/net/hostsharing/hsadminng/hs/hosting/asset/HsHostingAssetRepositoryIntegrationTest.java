package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
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
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.Array.fromFormatted;
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
    class CreateAsset {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewAsset() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = assetRepo.count();
            final var givenManagedServer = givenManagedServer("First", MANAGED_SERVER);

            // when
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetEntity.builder()
                        .bookingItem(givenManagedServer.getBookingItem())
                        .parentAsset(givenManagedServer)
                        .caption("some new managed webspace")
                        .type(HsHostingAssetType.MANAGED_WEBSPACE)
                        .identifier("xyz90")
                        .build();
                return toCleanup(assetRepo.save(newAsset));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsHostingAssetEntity::getUuid).isNotNull();
            assertThatAssetIsPersisted(result.returnedValue());
            assertThat(assetRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();
            final var givenBookingItem = givenBookingItem("First", "some PrivateCloud");

            // when
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetEntity.builder()
                        .bookingItem(givenBookingItem)
                        .type(HsHostingAssetType.MANAGED_SERVER)
                        .identifier("vm9000")
                        .caption("some new managed webspace")
                        .build();
                return toCleanup(assetRepo.save(newAsset));
            });

            // then
            result.assertSuccessful();
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:ADMIN",
                    "hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:OWNER",
                    "hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(fromFormatted(
                            initialGrantNames,

                            // owner
                            "{ grant perm:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:DELETE to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:OWNER by system and assume }",
                            "{ grant role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:OWNER to role:hs_booking_item#D-1000111-somePrivateCloud:ADMIN by system and assume }",

                            // admin
                            "{ grant perm:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:INSERT>hs_hosting_asset to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:ADMIN by system and assume }",
                            "{ grant perm:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:UPDATE to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:ADMIN by system and assume }",
                            "{ grant role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:ADMIN to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:OWNER by system and assume }",

                            // tenant
                            "{ grant perm:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:SELECT to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:TENANT by system and assume }",
                            "{ grant role:hs_booking_item#D-1000111-somePrivateCloud:TENANT to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:TENANT by system and assume }",
                            "{ grant role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:TENANT to role:hs_hosting_asset#D-1000111-somePrivateCloud-vm9000:ADMIN by system and assume }",

                            null));
        }

        private void assertThatAssetIsPersisted(final HsHostingAssetEntity saved) {
            final var found = assetRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().map(HsHostingAssetEntity::toString).get().isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindByDebitorUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllAssetsOfArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var debitorUuid = debitorRepo.findDebitorByDebitorNumber(1000212).stream()
                    .findAny().orElseThrow().getUuid();

            // when
            final var result = assetRepo.findAllByDebitorUuid(debitorUuid);

            // then
            allTheseServersAreReturned(
                    result,
                    "HsHostingAssetEntity(D-1000212:some ManagedServer, MANAGED_WEBSPACE, D-1000212:some PrivateCloud:vm1012, bbb01, some Webspace, { HDD: 2048, RAM: 1, SDD: 512, extra: 42 })",
                    "HsHostingAssetEntity(D-1000212:some PrivateCloud, MANAGED_SERVER, vm1012, some ManagedServer, { CPU: 2, SDD: 512, extra: 42 })",
                    "HsHostingAssetEntity(D-1000212:some PrivateCloud, CLOUD_SERVER, vm2012, another CloudServer, { CPU: 2, HDD: 1024, extra: 42 })");
        }

        @Test
        public void normalUser_canViewOnlyRelatedAsset() {
            // given:
            context("person-FirbySusan@example.com");
            final var debitorUuid = debitorRepo.findDebitorByDebitorNumber(1000111).stream().findAny().orElseThrow().getUuid();

            // when:
            final var result = assetRepo.findAllByDebitorUuid(debitorUuid);

            // then:
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAssetEntity(D-1000111:some ManagedServer, MANAGED_WEBSPACE, D-1000111:some PrivateCloud:vm1011, aaa01, some Webspace, { HDD: 2048, RAM: 1, SDD: 512, extra: 42 })",
                    "HsHostingAssetEntity(D-1000111:some PrivateCloud, MANAGED_SERVER, vm1011, some ManagedServer, { CPU: 2, SDD: 512, extra: 42 })",
                    "HsHostingAssetEntity(D-1000111:some PrivateCloud, CLOUD_SERVER, vm2011, another CloudServer, { CPU: 2, HDD: 1024, extra: 42 })");
        }
    }

    @Nested
    class UpdateAsset {

        @Test
        public void hostsharingAdmin_canUpdateArbitraryServer() {
            // given
            final var givenAssetUuid = givenSomeTemporaryAsset("First", "vm1000").getUuid();

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
                    .extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyAsset() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenAsset = givenSomeTemporaryAsset("First", "vm1000");

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
            final var givenAsset = givenSomeTemporaryAsset("First", "vm1000");

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
            final var givenAsset = givenSomeTemporaryAsset("First", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_hosting_asset#D-1000111-someCloudServer-vm1000:ADMIN");
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
            final var givenAsset = givenSomeTemporaryAsset("First", "vm1000");

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
                "[creating hosting-asset test-data 1000111, hs_hosting_asset, INSERT]",
                "[creating hosting-asset test-data 1000212, hs_hosting_asset, INSERT]",
                "[creating hosting-asset test-data 1000313, hs_hosting_asset, INSERT]");
    }

    private HsHostingAssetEntity givenSomeTemporaryAsset(final String debitorName, final String identifier) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenBookingItem = givenBookingItem(debitorName, "some CloudServer");
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

    HsBookingItemEntity givenBookingItem(final String debitorName, final String bookingItemCaption) {
        final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike(debitorName).stream().findAny().orElseThrow();
        return bookingItemRepo.findAllByDebitorUuid(givenDebitor.getUuid()).stream()
                .filter(i -> i.getCaption().equals(bookingItemCaption))
                .findAny().orElseThrow();
    }

    HsHostingAssetEntity givenManagedServer(final String debitorName, final HsHostingAssetType type) {
        final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike(debitorName).stream().findAny().orElseThrow();
        return assetRepo.findAllByDebitorUuid(givenDebitor.getUuid()).stream()
                .filter(i -> i.getType().equals(type))
                .findAny().orElseThrow();
    }

    void exactlyTheseAssetsAreReturned(
            final List<HsHostingAssetEntity> actualResult,
            final String... serverNames) {
        assertThat(actualResult)
                .extracting(HsHostingAssetEntity::toString)
                .containsExactlyInAnyOrder(serverNames);
    }

    void allTheseServersAreReturned(final List<HsHostingAssetEntity> actualResult, final String... serverNames) {
        assertThat(actualResult)
                .extracting(HsHostingAssetEntity::toString)
                .contains(serverNames);
    }
}
