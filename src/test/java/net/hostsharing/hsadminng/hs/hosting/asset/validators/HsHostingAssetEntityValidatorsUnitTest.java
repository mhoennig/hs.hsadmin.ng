package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidators.valid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsHostingAssetEntityValidatorsUnitTest {

    @Test
    void validThrowsException() {
        // given
        final var managedServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .build();

        // when
        final var result = catchThrowable( ()-> valid(managedServerHostingAssetEntity) );

        // then
        assertThat(result).isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "'config.monit_max_ssd_usage' is required but missing",
                        "'config.monit_max_cpu_usage' is required but missing",
                        "'config.monit_max_ram_usage' is required but missing");
    }
}
