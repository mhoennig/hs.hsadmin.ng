package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.asset.validator.HsHostingAssetValidator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;

class HsHostingAssetValidatorUnitTest {

    @Test
    void validatesMissingProperties() {
        // given
        final var validator = HsHostingAssetValidator.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                        .type(MANAGED_WEBSPACE)
                        .config(emptyMap())
                        .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'SSD' is required but missing",
                "'Traffic' is required but missing"
        );
    }

    @Test
    void validatesUnknownProperties() {
        // given
        final var validator = HsHostingAssetValidator.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .config(Map.ofEntries(
                        entry("HDD", 0),
                        entry("SSD", 1),
                        entry("Traffic", 10),
                        entry("unknown", "some value")
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'unknown' is not expected but is 'some value'");
    }

    @Test
    void validatesDependentProperties() {
        // given
        final var validator = HsHostingAssetValidator.forType(MANAGED_SERVER);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_SERVER)
                .config(Map.ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'SLA-EMail' is expected to be false because SLA-Platform=BASIC but is true");
    }

    @Test
    void validatesValidProperties() {
        // given
        final var validator = HsHostingAssetValidator.forType(MANAGED_WEBSPACE);
        final var mangedWebspaceHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(MANAGED_WEBSPACE)
                .config(Map.ofEntries(
                        entry("HDD", 200),
                        entry("SSD", 25),
                        entry("Traffic", 250)
                ))
                .build();

        // when
        final var result = validator.validate(mangedWebspaceHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }
}
