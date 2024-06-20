package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;


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
        assertThat(result).isNull(); // all required properties have defaults
    }
}
