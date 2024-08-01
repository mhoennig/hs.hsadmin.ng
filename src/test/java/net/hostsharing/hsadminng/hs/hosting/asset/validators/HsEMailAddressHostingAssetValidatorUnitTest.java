package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ADDRESS;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static org.assertj.core.api.Assertions.assertThat;

class HsEMailAddressHostingAssetValidatorUnitTest {

    final static HsHostingAssetEntity domainMboxetup = HsHostingAssetEntity.builder()
            .type(DOMAIN_MBOX_SETUP)
            .identifier("example.org")
            .build();
    static HsHostingAssetEntity.HsHostingAssetEntityBuilder validEntityBuilder() {
            return HsHostingAssetEntity.builder()
                .type(EMAIL_ADDRESS)
                .parentAsset(domainMboxetup)
                .identifier("test@example.org")
                .config(Map.ofEntries(
                        entry("local-part", "test"),
                        entry("target", Array.of("xyz00", "xyz00-abc", "office@example.com"))
                ));
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(EMAIL_ADDRESS);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=string, propertyName=local-part, matchesRegEx=[^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$], required=true}",
                "{type=string, propertyName=sub-domain, matchesRegEx=[^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$]}",
                "{type=string[], propertyName=target, elementsOf={type=string, propertyName=target, matchesRegEx=[^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\._-]*)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$], maxLength=320}, required=true, minLength=1}");
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("local-part", "no@allowed"),
                        entry("sub-domain", "no@allowedeither"),
                        entry("target", Array.of("xyz00", "xyz00-abc", "garbage", "office@example.com"))))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:test@example.org.config.local-part' is expected to match any of [^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$] but 'no@allowed' does not match",
                "'EMAIL_ADDRESS:test@example.org.config.sub-domain' is expected to match any of [^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$] but 'no@allowedeither' does not match",
                "'EMAIL_ADDRESS:test@example.org.config.target' is expected to match any of [^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\._-]*)?$, ^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$] but 'garbage' does not match any");
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .identifier("abc00-office")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^\\Qtest@example.org\\E$', but is 'abc00-office'");
    }

    @Test
    void validatesInvalidReferences() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
                .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                .assignedToAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:test@example.org.bookingItem' must be null but is of type MANAGED_SERVER",
                "'EMAIL_ADDRESS:test@example.org.parentAsset' must be of type DOMAIN_MBOX_SETUP but is of type MANAGED_SERVER",
                "'EMAIL_ADDRESS:test@example.org.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }
}
