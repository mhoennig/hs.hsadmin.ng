package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidators.forType;
import static org.assertj.core.api.Assertions.assertThat;

class HsCloudServerHostingAssetValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var cloudServerHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(CLOUD_SERVER)
                .config(Map.ofEntries(
                        entry("RAM", 2000),
                        entry("SSD", 256),
                        entry("Traffic", "250"),
                        entry("SLA-Platform", "xxx")
                ))
                .build();
        final var validator = forType(cloudServerHostingAssetEntity.getType());


        // when
        final var result = validator.validate(cloudServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'config.SLA-Platform' is not expected but is set to 'xxx'",
                "'config.CPUs' is required but missing",
                "'config.RAM' is expected to be <= 128 but is 2000",
                "'config.SSD' is expected to be multiple of 25 but is 256",
                "'config.Traffic' is expected to be of type class java.lang.Integer, but is of type 'String'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=CPUs, required=true, unit=null, min=1, max=32, step=null}",
                "{type=integer, propertyName=RAM, required=true, unit=GB, min=1, max=128, step=null}",
                "{type=integer, propertyName=SSD, required=true, unit=GB, min=25, max=1000, step=25}",
                "{type=integer, propertyName=HDD, required=false, unit=GB, min=0, max=4000, step=250}",
                "{type=integer, propertyName=Traffic, required=true, unit=GB, min=250, max=10000, step=250}");
    }
}
