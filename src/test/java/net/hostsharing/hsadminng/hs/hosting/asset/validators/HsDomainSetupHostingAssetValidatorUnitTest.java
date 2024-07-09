package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainSetupHostingAssetValidatorUnitTest {

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org");
    }

    enum InvalidDomainNameIdentifier {
        EMPTY(""),
        TOO_LONG("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz0123456890123456789.de"),
        DASH_AT_BEGINNING("-example.com"),
        DOT_AT_BEGINNING(".example.com"),
        DOT_AT_END("example.com.");

        final String domainName;

        InvalidDomainNameIdentifier(final String domainName) {
            this.domainName = domainName;
        }
    }

    @ParameterizedTest
    @EnumSource(InvalidDomainNameIdentifier.class)
    void rejectsInvalidIdentifier(final InvalidDomainNameIdentifier testCase) {
        // given
        final var givenEntity = validEntityBuilder().identifier(testCase.domainName).build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}', but is '"+testCase.domainName+"'"
        );
    }


    enum ValidDomainNameIdentifier {
        SIMPLE("exampe.org"),
        MAX_LENGTH("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234568901.de"),
        WITH_DASH("example-test.com"),
        SUBDOMAIN("test.example.com");

        final String domainName;

        ValidDomainNameIdentifier(final String domainName) {
            this.domainName = domainName;
        }
    }

    @ParameterizedTest
    @EnumSource(ValidDomainNameIdentifier.class)
    void acceptsValidIdentifier(final ValidDomainNameIdentifier testCase) {
        // given
        final var givenEntity = validEntityBuilder().identifier(testCase.domainName).build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void containsNoProperties() {
        // when
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }

    @Test
    void validatesReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .parentAsset(HsHostingAssetEntity.builder().type(CLOUD_SERVER).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(MANAGED_SERVER).build())
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_SETUP:example.org.bookingItem' must be null but is of type CLOUD_SERVER",
                "'DOMAIN_SETUP:example.org.parentAsset' must be null or of type DOMAIN_SETUP but is of type CLOUD_SERVER",
                "'DOMAIN_SETUP:example.org.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }
}
