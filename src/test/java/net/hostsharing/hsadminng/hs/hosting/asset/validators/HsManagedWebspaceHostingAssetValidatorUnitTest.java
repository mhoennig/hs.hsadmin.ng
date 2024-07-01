package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.TEST_PROJECT;

class HsManagedWebspaceHostingAssetValidatorUnitTest {

    final HsBookingItemEntity managedServerBookingItem = HsBookingItemEntity.builder()
            .project(TEST_PROJECT)
            .type(HsBookingItemType.MANAGED_SERVER)
            .caption("Test Managed-Server")
            .resources(Map.ofEntries(
                    entry("CPUs", 2),
                    entry("RAM", 25),
                    entry("SSD", 25),
                    entry("Traffic", 250),
                    entry("SLA-Platform", "EXT4H"),
                    entry("SLA-EMail", true)
            ))
            .build();
    final HsBookingItemEntity cloudServerBookingItem = managedServerBookingItem.toBuilder()
            .type(HsBookingItemType.CLOUD_SERVER)
            .caption("Test Cloud-Server")
            .build();

    final HsHostingAssetEntity mangedServerAssetEntity = HsHostingAssetEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .bookingItem(managedServerBookingItem)
            .identifier("vm1234")
            .config(Map.ofEntries(
                    entry("monit_max_ssd_usage", 70),
                    entry("monit_max_cpu_usage", 80),
                    entry("monit_max_ram_usage", 90)
            ))
            .build();
    final HsHostingAssetEntity cloudServerAssetEntity = HsHostingAssetEntity.builder()
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
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemEntity.builder()
                        .type(HsBookingItemType.MANAGED_WEBSPACE)
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .isLoaded(true)
                .build();

        // when
        final var result = validator.validateContext(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesIdentifierAndReferencedEntities() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .build();

        // when
        final var result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'identifier' expected to match '^abc[0-9][0-9]$', but is 'xyz00'");
    }

    @Test
    void validatesUnknownProperties() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(Map.ofEntries(
                        entry("unknown", "some value")
                ))
                .build();

        // when
        final var result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'MANAGED_WEBSPACE:abc00.config.unknown' is not expected but is set to 'some value'");
    }

    @Test
    void validatesValidEntity() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemEntity.builder()
                        .type(HsBookingItemType.MANAGED_WEBSPACE)
                        .caption("some ManagedWebspace")
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .build();

        // when
        final var result = Stream.concat(
                        validator.validateEntity(mangedWebspaceHostingAssetEntity).stream(),
                        validator.validateContext(mangedWebspaceHostingAssetEntity).stream())
                .toList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesEntityReferences() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .bookingItem(HsBookingItemEntity.builder()
                        .type(HsBookingItemType.MANAGED_SERVER)
                        .caption("some ManagedServer")
                        .resources(Map.ofEntries(entry("SSD", 25), entry("Traffic", 250)))
                        .build())
                .parentAsset(cloudServerAssetEntity)
                .assignedToAsset(HsHostingAssetEntity.builder().build())
                .identifier("abc00")
                .build();

        // when
        final var result = validator.validateEntity(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly(
                "'MANAGED_WEBSPACE:abc00.bookingItem' must be of type MANAGED_WEBSPACE but is of type MANAGED_SERVER",
                "'MANAGED_WEBSPACE:abc00.parentAsset' must be of type MANAGED_SERVER but is of type CLOUD_SERVER",
                "'MANAGED_WEBSPACE:abc00.assignedToAsset' must be null but is set to D-???????-?:some ManagedServer");
    }
}
