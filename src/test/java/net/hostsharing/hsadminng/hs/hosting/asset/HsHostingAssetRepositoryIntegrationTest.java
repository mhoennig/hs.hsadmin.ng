package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealRepository;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRbacRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
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
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ADDRESS;
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
    HsHostingAssetRealRepository realAssetRepo;

    @Autowired
    HsHostingAssetRbacRepository rbacAssetRepo;

    @Autowired
    HsBookingItemRealRepository realBookingItemRepo;

    @Autowired
    HsBookingProjectRbacRepository projectRepo;

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
                    from tx_journal_v
                    where targettable = 'hs_hosting_asset';
                """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, another CloudServer]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Domain-DNS-Setup]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Domain-HTTP-Setup]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Domain-MBOX-Setup]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Domain-SMTP-Setup]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Domain-Setup]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some E-Mail-Address]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some E-Mail-Alias]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some ManagedServer]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some UnixUser for E-Mail]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some UnixUser for Website]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some Webspace]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default MariaDB instance]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default MariaDB user]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default MariaDB database]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default Postgresql instance]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default Postgresql user]",
                "[creating hosting-asset test-data, hs_hosting_asset, INSERT, some default Postgresql database]"
        );
    }

    @Test
    public void historizationIsAvailable() {
        // given
        final String nativeQuerySql = """
                select count(*)
                    from hs_hosting_asset_hv ha;
                """;

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countBefore = (Integer) query.getSingleResult();

        // then
        assertThat(countBefore).as("hs_hosting_asset_hv should not contain rows for a timestamp in the past").isEqualTo(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()));
        em.createNativeQuery(nativeQuerySql, Integer.class);
        @SuppressWarnings("unchecked") final var countAfter = (Integer) query.getSingleResult();

        // then
        assertThat(countAfter).as("hs_hosting_asset_hv should contain rows for a timestamp in the future").isGreaterThan(1);
    }

    @Nested
    class CreateAsset {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewAsset() {
            // given
            context("superuser-alex@hostsharing.net"); // TODO.test: remove context(...) once all entities have real entities
            final var count = realAssetRepo.count();
            final var givenManagedServer = givenHostingAsset("D-1000111 default project", MANAGED_SERVER);
            final var newWebspaceBookingItem = newBookingItem(givenManagedServer.getBookingItem(), HsBookingItemType.MANAGED_WEBSPACE, "fir01");

            // when
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetRbacEntity.builder()
                        .bookingItem(newWebspaceBookingItem)
                        .parentAsset(givenManagedServer)
                        .caption("some new managed webspace")
                        .type(MANAGED_WEBSPACE)
                        .identifier("xyz90")
                        .build();
                return toCleanup(rbacAssetRepo.save(newAsset));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsHostingAssetRbacEntity::getUuid).isNotNull();
            assertThatAssetIsPersisted(result.returnedValue());
            assertThat(result.returnedValue().isLoaded()).isFalse();
            assertThat(realAssetRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            // TODO.test: remove context(...) once all entities have real entities
            context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var givenManagedServer = givenHostingAsset("D-1000111 default project", MANAGED_SERVER);
            final var newWebspaceBookingItem = newBookingItem(givenManagedServer.getBookingItem(), HsBookingItemType.MANAGED_WEBSPACE, "fir01");
            em.flush();
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            context("superuser-alex@hostsharing.net", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetRbacEntity.builder()
                        .bookingItem(newWebspaceBookingItem)
                        .parentAsset(givenManagedServer)
                        .type(HsHostingAssetType.MANAGED_WEBSPACE)
                        .identifier("fir00")
                        .caption("some new managed webspace")
                        .build();
                return toCleanup(rbacAssetRepo.save(newAsset));
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

                            // global-admin
                            "{ grant role:hs_hosting_asset#fir00:OWNER to role:global#global:ADMIN by system }", // workaround

                            // owner
                            "{ grant role:hs_hosting_asset#fir00:OWNER to user:superuser-alex@hostsharing.net by hs_hosting_asset#fir00:OWNER and assume }",
                            "{ grant role:hs_hosting_asset#fir00:OWNER to role:hs_booking_item#fir01:ADMIN by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:OWNER to role:hs_hosting_asset#vm1011:ADMIN by system and assume }",
                            "{ grant perm:hs_hosting_asset#fir00:DELETE to role:hs_hosting_asset#fir00:OWNER by system and assume }",

                            // admin
                            "{ grant role:hs_hosting_asset#fir00:ADMIN to role:hs_hosting_asset#fir00:OWNER by system and assume }",
                            "{ grant role:hs_hosting_asset#fir00:ADMIN to role:hs_booking_item#fir01:AGENT by system and assume }",
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

        @Test
        public void anyUser_canCreateNewDomainSetupAsset() {
            // when
            context("person-SmithPeter@example.com");
            final var result = attempt(em, () -> {
                final var newAsset = HsHostingAssetRbacEntity.builder()
                        .type(DOMAIN_SETUP)
                        .identifier("example.net")
                        .caption("some new domain setup")
                        .build();
                return rbacAssetRepo.save(newAsset);
            });

            // then
            // ... the domain setup was created and returned
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsHostingAssetRbacEntity::getUuid).isNotNull();
            assertThat(result.returnedValue().isLoaded()).isFalse();

            // ... the creating user can read the new domain setup
            context("person-SmithPeter@example.com");
            assertThatAssetIsPersisted(result.returnedValue());

            // ... a global admin can see the new domain setup as well if the domain OWNER role is assumed
            context("superuser-alex@hostsharing.net", "hs_hosting_asset#example.net:OWNER"); // only works with the assumed role
            assertThatAssetIsPersisted(result.returnedValue());
        }

        private void assertThatAssetIsPersisted(final HsHostingAssetRbacEntity saved) {
            em.clear();
            attempt(em, () -> {
                final var found = realAssetRepo.findByUuid(saved.getUuid());
                assertThat(found).isNotEmpty().map(HsHostingAsset::toString).contains(saved.toString());
            });
        }
    }

    @Nested
    class FindAssets {

        @ParameterizedTest
        @EnumSource(TestCase.class)
        public void globalAdmin_withoutAssumedRole_canViewArbitraryAssetsOfAllDebitors(final TestCase testCase) {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = repoUnderTest(testCase).findAllByCriteria(null, null, MANAGED_WEBSPACE);

            // then
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAsset(MANAGED_WEBSPACE, sec01, some Webspace, MANAGED_SERVER:vm1012, D-1000212:D-1000212 default project:separate ManagedWebspace)",
                    "HsHostingAsset(MANAGED_WEBSPACE, fir01, some Webspace, MANAGED_SERVER:vm1011, D-1000111:D-1000111 default project:separate ManagedWebspace)",
                    "HsHostingAsset(MANAGED_WEBSPACE, thi01, some Webspace, MANAGED_SERVER:vm1013, D-1000313:D-1000313 default project:separate ManagedWebspace)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedAssets() {
            // given:
            context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
            final var projectUuid = projectRepo.findByCaption("D-1000111 default project").stream()
                    .findAny().orElseThrow().getUuid();

            // when:
            final var result = rbacAssetRepo.findAllByCriteria(projectUuid, null, null);

            // then:
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAsset(MANAGED_WEBSPACE, fir01, some Webspace, MANAGED_SERVER:vm1011, D-1000111:D-1000111 default project:separate ManagedWebspace)",
                    "HsHostingAsset(MANAGED_SERVER, vm1011, some ManagedServer, D-1000111:D-1000111 default project:separate ManagedServer, { monit_max_cpu_usage : 90, monit_max_ram_usage : 80, monit_max_ssd_usage : 70 })");
        }

        @Test
        public void managedServerAgent_canFindAssetsRelatedToManagedServer() {
            // given
            final var parentAssetUuid = realAssetRepo.findByIdentifier("vm1012").stream()
                    .filter(ha -> ha.getType() == MANAGED_SERVER)
                    .findAny().orElseThrow().getUuid();

            // when
            context("superuser-alex@hostsharing.net", "hs_hosting_asset#vm1012:AGENT");
            final var result = rbacAssetRepo.findAllByCriteria(null, parentAssetUuid, null);

            // then
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAsset(MANAGED_WEBSPACE, sec01, some Webspace, MANAGED_SERVER:vm1012, D-1000212:D-1000212 default project:separate ManagedWebspace)",
                    "HsHostingAsset(MARIADB_INSTANCE, vm1012.MariaDB.default, some default MariaDB instance, MANAGED_SERVER:vm1012)",
                    "HsHostingAsset(PGSQL_INSTANCE, vm1012.Postgresql.default, some default Postgresql instance, MANAGED_SERVER:vm1012)");
        }

        @Test
        public void managedServerAgent_canFindRelatedEmailAddresses() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            context("superuser-alex@hostsharing.net", "hs_hosting_asset#sec01:AGENT");
            final var result = rbacAssetRepo.findAllByCriteria(null, null, EMAIL_ADDRESS);

            // then
            exactlyTheseAssetsAreReturned(
                    result,
                    "HsHostingAsset(EMAIL_ADDRESS, test@sec.example.org, some E-Mail-Address, DOMAIN_MBOX_SETUP:sec.example.org|MBOX)");
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
                final var foundAsset = em.find(HsHostingAssetRbacEntity.class, givenAssetUuid);
                foundAsset.getConfig().put("CPU", 2);
                foundAsset.getConfig().remove("SSD-storage");
                foundAsset.getConfig().put("HSD-storage", 2048);
                return toCleanup(rbacAssetRepo.save(foundAsset));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                assertThatAssetActuallyInDatabase(result.returnedValue());
            }).assertSuccessful();
        }

        private void assertThatAssetActuallyInDatabase(final HsHostingAssetRbacEntity saved) {
            final var found = realAssetRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(HsHostingAsset::getVersion).isEqualTo(saved.getVersion());
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
                rbacAssetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                return realAssetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedOwner_canDeleteTheirRelatedAsset() {
            // given
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_booking_project#D-1000111-D-1000111defaultproject:AGENT");
                assertThat(rbacAssetRepo.findByUuid(givenAsset.getUuid())).isPresent();

                rbacAssetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                return realAssetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedAdmin_canNotDeleteTheirRelatedAsset() {
            // given
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-FirbySusan@example.com", "hs_hosting_asset#vm1000:ADMIN");
                assertThat(rbacAssetRepo.findByUuid(givenAsset.getUuid())).isPresent();

                rbacAssetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to delete hs_hosting_asset");
            assertThat(jpaAttempt.transacted(() -> {
                return realAssetRepo.findByUuid(givenAsset.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAnAssetAlsoDeletesRelatedRolesAndGrants() {
            // given
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenAsset = givenSomeTemporaryAsset("D-1000111 default project", "vm1000");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return rbacAssetRepo.deleteByUuid(givenAsset.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsHostingAssetRealEntity givenSomeTemporaryAsset(final String projectCaption, final String identifier) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net"); // needed to determine creator
            final var givenBookingItem = givenBookingItem("D-1000111 default project", "test CloudServer");
            final var newAsset = HsHostingAssetRealEntity.builder()
                    .bookingItem(givenBookingItem)
                    .type(CLOUD_SERVER)
                    .identifier(identifier)
                    .caption(projectCaption)
                    .config(Map.ofEntries(
                            entry("CPU", 1),
                            entry("SSD-storage", 256)))
                    .build();

            return toCleanup(realAssetRepo.save(newAsset));
        }).assertSuccessful().returnedValue();
    }

    HsBookingItemRealEntity givenBookingItem(final String projectCaption, final String bookingItemCaption) {
        return realBookingItemRepo.findByCaption(bookingItemCaption).stream()
                .filter(i -> i.getRelatedProject().getCaption().equals(projectCaption))
                .findAny().orElseThrow();
    }

    HsHostingAssetRealEntity givenHostingAsset(final String projectCaption, final HsHostingAssetType type) {
        final var givenProject = projectRepo.findByCaption(projectCaption).stream()
                .findAny().orElseThrow();
        return realAssetRepo.findAllByCriteria(givenProject.getUuid(), null, type).stream()
                .findAny().orElseThrow();
    }

    HsBookingItemRealEntity newBookingItem(
            final HsBookingItemRealEntity parentBookingItem,
            final HsBookingItemType type,
            final String caption) {
        final var newBookingItem = HsBookingItemRealEntity.builder()
                .parentItem(parentBookingItem)
                .type(type)
                .caption(caption)
                .build();
        return toCleanup(realBookingItemRepo.save(newBookingItem));
    }

    void exactlyTheseAssetsAreReturned(
            final List<? extends HsHostingAsset> actualResult,
            final String... serverNames) {
        assertThat(actualResult)
                .extracting(HsHostingAsset::toString)
                .extracting(input -> input.replaceAll("\\s+", " "))
                .extracting(input -> input.replaceAll("\"", ""))
                .extracting(input -> input.replaceAll("\" : ", "\": "))
                .containsExactlyInAnyOrder(serverNames);
    }

    void allTheseBookingProjectsAreReturned(
            final List<? extends HsHostingAsset> actualResult,
            final String... serverNames) {
        assertThat(actualResult)
                .extracting(HsHostingAsset::toString)
                .extracting(input -> input.replaceAll("\\s+", " "))
                .extracting(input -> input.replaceAll("\"", ""))
                .extracting(input -> input.replaceAll("\" : ", "\": "))
                .contains(serverNames);
    }

    private HsHostingAssetRepository<? extends HsHostingAsset> repoUnderTest(final HsHostingAssetRepositoryIntegrationTest.TestCase testCase) {
        return testCase.repo(HsHostingAssetRepositoryIntegrationTest.this);
    }

    enum TestCase {
        REAL {
            @Override
            HsHostingAssetRepository<HsHostingAssetRealEntity> repo(final HsHostingAssetRepositoryIntegrationTest test) {
                return test.realAssetRepo;
            }
        },
        RBAC {
            @Override
            HsHostingAssetRepository<HsHostingAssetRbacEntity> repo(final HsHostingAssetRepositoryIntegrationTest test) {
                return test.rbacAssetRepo;
            }
        };

        abstract HsHostingAssetRepository<? extends HsHostingAsset> repo(final HsHostingAssetRepositoryIntegrationTest test);
    }
}
