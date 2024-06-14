package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsCloudServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var cloudServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .config(Map.ofEntries(
                        entry("RAM", 2000)
                ))
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(cloudServerHostingAssetEntity.getType());


        // when
        final var result = validator.validate(cloudServerHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'CLOUD_SERVER:vm1234.config.RAM' is not expected but is set to '2000'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }
}
