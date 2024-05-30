package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidators.forType;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .config(Map.ofEntries(
                        entry("RAM", 2000),
                        entry("SSD", 256),
                        entry("Traffic", "250"),
                        entry("SLA-Platform", "xxx")
                ))
                .build();
        final var validator = forType(mangedWebspaceHostingAssetEntity.getType());

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'config.SLA-Platform' is not expected but is set to 'xxx'",
                "'config.CPUs' is required but missing",
                "'config.RAM' is expected to be <= 128 but is 2000",
                "'config.SSD' is expected to be multiple of 25 but is 256",
                "'config.Traffic' is expected to be of type class java.lang.Integer, but is of type 'String'");
    }
}
