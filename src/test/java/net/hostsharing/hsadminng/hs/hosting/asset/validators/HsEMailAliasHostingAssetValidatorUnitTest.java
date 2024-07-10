package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ALIAS;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static org.assertj.core.api.Assertions.assertThat;

class HsEMailAliasHostingAssetValidatorUnitTest {

    @Test
    void containsAllValidations() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(EMAIL_ALIAS);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=string[], propertyName=target, elementsOf={type=string, propertyName=target, matchesRegEx=[^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9]+)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$], maxLength=320}, required=true, minLength=1}");
    }

    @Test
    void validatesValidEntity() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .identifier("xyz00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of("xyz00", "xyz00-abc", "office@example.com"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesProperties() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .identifier("xyz00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of("xyz00", "xyz00-abc", "garbage", "office@example.com"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ALIAS:xyz00-office.config.target' is expected to match any of [^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9]+)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$] but 'garbage' does not match any");
    }

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .identifier("abc00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of("office@example.com"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^xyz00$|^xyz00-[a-z0-9]+$', but is 'abc00-office'");
    }

    @Test
    void validatesInvalidReferences() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetEntity.builder()
                .type(EMAIL_ALIAS)
                .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
                .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                .assignedToAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                .identifier("abc00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of("office@example.com"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ALIAS:abc00-office.bookingItem' must be null but is of type MANAGED_SERVER",
                "'EMAIL_ALIAS:abc00-office.parentAsset' must be of type MANAGED_WEBSPACE but is of type MANAGED_SERVER",
                "'EMAIL_ALIAS:abc00-office.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }
}
