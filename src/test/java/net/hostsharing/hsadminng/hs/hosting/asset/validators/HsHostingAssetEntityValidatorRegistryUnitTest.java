package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsHostingAssetEntityValidatorRegistryUnitTest {

    @Test
    void forTypeWithUnknownTypeThrowsException() {
        // when
        final var thrown = catchThrowable(() -> {
            HsHostingAssetEntityValidatorRegistry.forType(null);
        });

        // then
        assertThat(thrown).hasMessage("no validator found for type null");
    }

    @Test
    void typesReturnsAllImplementedTypes() {
        // when
        final var types = HsHostingAssetEntityValidatorRegistry.types();

        // then
        // TODO.test: when all types are implemented, replace with set of all types:
        // assertThat(types).isEqualTo(EnumSet.allOf(HsHostingAssetType.class));
        // also remove "Implemented" from the test method name.
        assertThat(types).containsExactlyInAnyOrder(
                HsHostingAssetType.CLOUD_SERVER,
                HsHostingAssetType.MANAGED_SERVER,
                HsHostingAssetType.MANAGED_WEBSPACE,
                HsHostingAssetType.UNIX_USER,
                HsHostingAssetType.EMAIL_ALIAS,
                HsHostingAssetType.DOMAIN_SETUP,
                HsHostingAssetType.DOMAIN_DNS_SETUP
        );
    }
}
