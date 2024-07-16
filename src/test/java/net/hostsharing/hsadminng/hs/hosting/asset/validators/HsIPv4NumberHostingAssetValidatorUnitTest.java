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
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV4_NUMBER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static org.assertj.core.api.Assertions.assertThat;

class HsIPv4NumberHostingAssetValidatorUnitTest {

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(IPV4_NUMBER)
                .identifier("83.223.95.145");
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(IPV4_NUMBER);

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
    @ValueSource(strings = {"a.b.c.d", "83.223.95", "83.223.95.145.1", "2a01:37:1000::53df:5f91:0"})
    void rejectsInvalidIdentifier(final String givenIdentifier) {
        // given
        final var givenEntity = validEntityBuilder().identifier(givenIdentifier).build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$', but is '" + givenIdentifier + "'"
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
                "'IPV4_NUMBER:83.223.95.145.bookingItem' must be null but is of type CLOUD_SERVER",
                "'IPV4_NUMBER:83.223.95.145.parentAsset' must be null but is of type MANAGED_WEBSPACE",
                "'IPV4_NUMBER:83.223.95.145.assignedToAsset' must be null or of type CLOUD_SERVER or MANAGED_SERVER or MANAGED_WEBSPACE but is of type UNIX_USER");
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
                "'IPV4_NUMBER:83.223.95.145.config.any' is not expected but is set to 'false'");
    }
}
