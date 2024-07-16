package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV6_NUMBER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static org.assertj.core.api.Assertions.assertThat;

class HsIPv6NumberHostingAssetValidatorUnitTest {

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(IPV6_NUMBER)
                .identifier("2001:db8:3333:4444:5555:6666:7777:8888");
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(IPV6_NUMBER);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"83.223.95", "2a01:37:1000::53df:5f91:0:123::123"})
    void rejectsInvalidIdentifier(final String givenIdentifier) {
        // given
        final var givenEntity = validEntityBuilder().identifier(givenIdentifier).build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).contains(
                "'identifier' expected to be a valid IPv6 address, but is '" + givenIdentifier + "'"
        );
    }

    @ParameterizedTest
    @EnumSource(value = HsHostingAssetType.class, names = { "CLOUD_SERVER", "MANAGED_SERVER", "MANAGED_WEBSPACE" })
    void acceptsValidReferencedEntity(final HsHostingAssetType givenAssignedToAssetType) {
        // given
        final var ipNumberHostingAssetEntity = validEntityBuilder()
                .assignedToAsset(HsHostingAssetEntity.builder().type(givenAssignedToAssetType).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(ipNumberHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(ipNumberHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var ipNumberHostingAssetEntity = validEntityBuilder()
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(UNIX_USER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(ipNumberHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(ipNumberHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'IPV6_NUMBER:2001:db8:3333:4444:5555:6666:7777:8888.bookingItem' must be null but is of type CLOUD_SERVER",
                "'IPV6_NUMBER:2001:db8:3333:4444:5555:6666:7777:8888.parentAsset' must be null but is of type MANAGED_WEBSPACE",
                "'IPV6_NUMBER:2001:db8:3333:4444:5555:6666:7777:8888.assignedToAsset' must be null or of type CLOUD_SERVER or MANAGED_SERVER or MANAGED_WEBSPACE but is of type UNIX_USER");
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var ipNumberHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("any", "false")
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(ipNumberHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(ipNumberHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'IPV6_NUMBER:2001:db8:3333:4444:5555:6666:7777:8888.config.any' is not expected but is set to 'false'");
    }
}
