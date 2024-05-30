package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidators.valid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsHostingAssetEntityValidatorsUnitTest {

    @Test
    void validThrowsException() {
        // given
        final var cloudServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(CLOUD_SERVER)
                .build();

        // when
        final var result = catchThrowable( ()-> valid(cloudServerHostingAssetEntity) );

        // then
        assertThat(result).isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "'config.CPUs' is required but missing",
                        "'config.RAM' is required but missing",
                        "'config.SSD' is required but missing",
                        "'config.Traffic' is required but missing");
    }
}
