package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static org.assertj.core.api.Assertions.assertThat;

class HsUnixUserHostingAssetValidatorUnitTest {

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var unixUserHostingAsset = HsHostingAssetEntity.builder()
                .type(UNIX_USER)
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).identifier("abc00").build())
                .identifier("xyz99-temp")
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());


        // when
        final var result = validator.validate(unixUserHostingAsset);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^abc00$|^abc00-[a-z0-9]+$', but is 'xyz99-temp'");
    }
}
