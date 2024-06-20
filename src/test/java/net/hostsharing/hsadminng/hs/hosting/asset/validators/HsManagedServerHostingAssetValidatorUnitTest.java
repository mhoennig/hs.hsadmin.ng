package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .config(Map.ofEntries(
                        entry("monit_max_hdd_usage", "90"),
                        entry("monit_max_cpu_usage", 2),
                        entry("monit_max_ram_usage", 101)
                ))
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedWebspaceHostingAssetEntity.getType());

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MANAGED_SERVER:vm1234.config.monit_max_cpu_usage' is expected to be >= 10 but is 2",
                "'MANAGED_SERVER:vm1234.config.monit_max_ram_usage' is expected to be <= 100 but is 101",
                "'MANAGED_SERVER:vm1234.config.monit_max_hdd_usage' is expected to be of type class java.lang.Integer, but is of type 'String'");
    }
}
