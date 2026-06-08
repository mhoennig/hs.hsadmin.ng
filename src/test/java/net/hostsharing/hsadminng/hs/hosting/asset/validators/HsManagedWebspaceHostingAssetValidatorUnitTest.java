package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import lombok.val;

import net.hostsharing.hsadminng.hs.hosting.asset.EntityManagerMock;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.PROJECT_TEST_ENTITY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HsManagedWebspaceHostingAssetValidatorUnitTest {

    final HsBookingItemRealEntity managedServerBookingItem = HsBookingItemRealEntity.builder()
            .project(PROJECT_TEST_ENTITY)
            .type(HsBookingItemType.MANAGED_SERVER)
            .caption("Test Managed-Server")
            .resources(Map.ofEntries(
                    entry("CPU", 2),
                    entry("RAM", 25),
                    entry("SSD", 25),
                    entry("Traffic", 250),
                    entry("SLA-Platform", "EXT4H"),
                    entry("SLA-EMail", true)
            ))
            .build();
    final HsBookingItemRealEntity cloudServerBookingItem = managedServerBookingItem.toBuilder()
            .type(HsBookingItemType.CLOUD_SERVER)
            .caption("Test Cloud-Server")
            .build();

    final HsHostingAssetRealEntity mangedServerAssetEntity = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .bookingItem(managedServerBookingItem)
            .identifier("vm1234")
            .config(Map.ofEntries(
                    entry("monit_max_ssd_usage", 70),
                    entry("monit_max_cpu_usage", 80),
                    entry("monit_max_ram_usage", 90)
            ))
            .build();
    final HsHostingAssetRealEntity cloudServerAssetEntity = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.CLOUD_SERVER)
            .bookingItem(cloudServerBookingItem)
            .identifier("vm1234")
            .config(Map.ofEntries(
                    entry("monit_max_ssd_usage", 70),
                    entry("monit_max_cpu_usage", 80),
                    entry("monit_max_ram_usage", 90)
            ))
            .build();

    @Test
    void acceptsAlienIdentifierPrefixForPreExistingEntity() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val mangedWebspaceHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder()
                        .type(HsBookingItemType.MANAGED_WEBSPACE)
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .isLoaded(true)
                .build();
        val em = EntityManagerMock.createEntityManagerMockWithAssetQueryFake(null);

        // when
        val result = HsEntityValidator.doWithEntityManager(em, () ->
                validator.validateContext(mangedWebspaceHostingAssetEntity));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesIdentifierAndReferencedEntities() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val mangedWebspaceHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .build();

        // when
        val result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'identifier' expected to match '^abc[0-9][0-9]$' but is 'xyz00'");
    }

    @Test
    void validatesUnknownProperties() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val mangedWebspaceHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(Map.ofEntries(
                        entry("unknown", "some value")
                ))
                .build();

        // when
        val result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'MANAGED_WEBSPACE:abc00.config.unknown' is not expected but is set to 'some value'");
    }

    @Test
    void validatesValidEntity() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val mangedWebspaceHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder()
                        .type(HsBookingItemType.MANAGED_WEBSPACE)
                        .project(PROJECT_TEST_ENTITY)
                        .caption("some ManagedWebspace")
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .build();
        val em = EntityManagerMock.createEntityManagerMockWithAssetQueryFake(null);

        // when
        val result = HsEntityValidator.doWithEntityManager(em, () ->
                Stream.concat(
                        validator.validateEntity(mangedWebspaceHostingAssetEntity).stream(),
                        validator.validateContext(mangedWebspaceHostingAssetEntity).stream())
                .toList());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidEntityReferences() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val mangedWebspaceHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder()
                        .type(HsBookingItemType.MANAGED_SERVER)
                        .caption("some ManagedServer")
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(cloudServerAssetEntity)
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(CLOUD_SERVER).build())
                .identifier("abc00")
                .build();

        // when
        val result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly(
                "'MANAGED_WEBSPACE:abc00.bookingItem' must be of type MANAGED_WEBSPACE but is of type MANAGED_SERVER",
                "'MANAGED_WEBSPACE:abc00.parentAsset' must be null or of type MANAGED_SERVER but is of type CLOUD_SERVER",
                "'MANAGED_WEBSPACE:abc00.assignedToAsset' must be null but is of type CLOUD_SERVER");
    }

    @Test
    void postPersistCreatesDefaultUnixUserAndStoresGroupId() {
        // given
        val validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        val webspaceUuid = UUID.randomUUID();
        val webspaceRealEntity = HsHostingAssetRealEntity.builder()
                .uuid(webspaceUuid)
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemRealEntity.builder()
                        .type(HsBookingItemType.MANAGED_WEBSPACE)
                        .project(PROJECT_TEST_ENTITY)
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .build();
        val webspaceRbacEntity = HsHostingAssetRbacEntity.builder()
                .uuid(webspaceUuid)
                .type(MANAGED_WEBSPACE)
                .bookingItem(webspaceRealEntity.getBookingItem())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .build();
        val em = mock(EntityManager.class);
        val nativeQuery = mock(Query.class);
        val assetQuery = mock(TypedQuery.class);
        val assetStream = mock(Stream.class);
        when(em.find(HsHostingAssetRealEntity.class, webspaceUuid)).thenReturn(webspaceRealEntity);
        when(em.createNativeQuery("SELECT nextval('hs_hosting.asset_unixuser_system_id_seq')", Integer.class))
                .thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(100000);
        when(em.createQuery(any(String.class), any(Class.class))).thenReturn(assetQuery);
        when(assetQuery.setParameter(any(String.class), any())).thenReturn(assetQuery);
        when(assetQuery.getResultStream()).thenReturn(assetStream);
        when(assetStream.findFirst()).thenReturn(java.util.Optional.empty());
        when(em.contains(any())).thenReturn(false);
        doNothing().when(em).persist(any());
        doNothing().when(em).flush();

        // when
        validator.postPersist(em, webspaceRbacEntity);

        // then
        assertThat(webspaceRbacEntity.getSubHostingAssets()).hasSize(1);
        assertThat(webspaceRbacEntity.getSubHostingAssets().getFirst().getIdentifier()).isEqualTo("abc00");
        assertThat(webspaceRbacEntity.getConfig().get("groupid")).isEqualTo(100000);
    }
}
