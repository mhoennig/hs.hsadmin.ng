package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsHostingAssetEntityValidatorUnitTest {

    @Test
    void validThrowsException() {
        // given
        final var managedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .build();

        // when
        final var result = catchThrowable( ()-> HsHostingAssetEntityValidatorRegistry.validated(managedServerHostingAssetEntity));

        // then
        assertThat(result).isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "'MANAGED_SERVER:vm1234.config.monit_max_ssd_usage' is required but missing",
                        "'MANAGED_SERVER:vm1234.config.monit_max_cpu_usage' is required but missing",
                        "'MANAGED_SERVER:vm1234.config.monit_max_ram_usage' is required but missing");
    }
}
