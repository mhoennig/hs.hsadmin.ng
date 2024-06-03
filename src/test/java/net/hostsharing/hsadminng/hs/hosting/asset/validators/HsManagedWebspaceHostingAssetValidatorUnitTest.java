package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.TEST_PROJECT;

class HsManagedWebspaceHostingAssetValidatorUnitTest {

    final HsBookingItemEntity managedServerBookingItem = HsBookingItemEntity.builder()
            .project(TEST_PROJECT)
            .build();
    final HsHostingAssetEntity mangedServerAssetEntity = HsHostingAssetEntity.builder()
            .type(MANAGED_SERVER)
            .bookingItem(managedServerBookingItem)
            .config(Map.ofEntries(
                    entry("HDD", 0),
                    entry("SSD", 1),
                    entry("Traffic", 10)
            ))
            .build();

    @Test
    void validatesIdentifier() {
        // given
        final var validator = HsHostingAssetEntityValidators.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("xyz00")
                .config(Map.ofEntries(
                        entry("HDD", 0),
                        entry("SSD", 1),
                        entry("Traffic", 10)
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'identifier' expected to match '^abc[0-9][0-9]$', but is 'xyz00'");
    }


    @Test
    void validatesMissingProperties() {
        // given
        final var validator = HsHostingAssetEntityValidators.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(emptyMap())
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'config.SSD' is required but missing",
                "'config.Traffic' is required but missing"
        );
    }

    @Test
    void validatesUnknownProperties() {
        // given
        final var validator = HsHostingAssetEntityValidators.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(Map.ofEntries(
                        entry("HDD", 0),
                        entry("SSD", 1),
                        entry("Traffic", 10),
                        entry("unknown", "some value")
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'config.unknown' is not expected but is set to 'some value'");
    }

    @Test
    void validatesValidProperties() {
        // given
        final var validator = HsHostingAssetEntityValidators.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .parentAsset(mangedServerAssetEntity)
                .identifier("abc00")
                .config(Map.ofEntries(
                        entry("HDD", 200),
                        entry("SSD", 25),
                        entry("Traffic", 250)
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }
}
