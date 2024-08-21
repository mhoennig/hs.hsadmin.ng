package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;

class HsEMailAliasHostingAssetValidatorUnitTest {

    @Test
    void containsAllValidations() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(EMAIL_ALIAS);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=string[], propertyName=target, elementsOf={type=string, propertyName=target, matchesRegEx=[^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\._-]*)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$, ^:include:/.*$, ^\\|.*$, ^/dev/null$], maxLength=320}, required=true, minLength=1}");
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
                .identifier("xyz00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of(
                                "xyz00",
                                "xyz00-abc",
                                "office@example.com",
                                "/dev/null",
                                "|/home/pacs/xyz00/mailinglists/ecartis -s xyz00-intern"
                                ))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidConfig() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
                .identifier("xyz00-office")
                .config(Map.ofEntries(
                        entry("target", Array.of(
                                "/dev/null",
                                "xyz00",
                                "xyz00-abc",
                                "garbage",
                                "office@example.com",
                                ":include:/home/pacs/xyz00/mailinglists/textfile",
                                "|/home/pacs/xyz00/mailinglists/executable"
                        ))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ALIAS:xyz00-office.config.target' is expected to match any of [^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\._-]*)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$, ^:include:/.*$, ^\\|.*$, ^/dev/null$] but 'garbage' does not match any");
    }

    @Test
    void rejectsEmptyTargetArray() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
                .identifier("xyz00-office")
                .config(Map.ofEntries(
                        entry("target", new String[0])
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAliasHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAliasHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ALIAS:xyz00-office.config.target' length is expected to be at min 1 but length of [[]] is 0");
    }

    @Test
    void rejectsInvalidIndentifier() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ALIAS)
                .parentAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
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
                "'identifier' expected to match '^xyz00$|^xyz00-[a-z0-9][a-z0-9\\._-]*$', but is 'abc00-office'");
    }

    @Test
    void validatesInvalidReferences() {
        // given
        final var emailAliasHostingAssetEntity = HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ALIAS)
                .bookingItem(MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .parentAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
                .assignedToAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
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
