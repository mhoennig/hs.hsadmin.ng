package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import java.util.Map;

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

    @Test
    void validatesIdentifier() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'identifier' expected to match '^abc[0-9][0-9]$', but is 'xyz00'");
    }

    @Test
    void validatesUnknownProperties() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(Map.ofEntries(
                        entry("unknown", "some value")
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'MANAGED_WEBSPACE:abc00.config.unknown' is not expected but is set to 'some value'");
    }

    @Test
    void validatesValidEntity() {
        // given
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }
}
