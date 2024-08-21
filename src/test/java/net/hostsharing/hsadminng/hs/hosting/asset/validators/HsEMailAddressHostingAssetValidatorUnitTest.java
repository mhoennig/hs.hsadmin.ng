package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ADDRESS;
import static net.hostsharing.hsadminng.mapper.PatchableMapWrapper.entry;
import static org.assertj.core.api.Assertions.assertThat;

class HsEMailAddressHostingAssetValidatorUnitTest {

    final static HsHostingAssetRealEntity domainSetup = HsHostingAssetRealEntity.builder()
            .type(DOMAIN_MBOX_SETUP)
            .identifier("example.org")
            .build();
    final static HsHostingAssetRealEntity domainMboxSetup = HsHostingAssetRealEntity.builder()
            .type(DOMAIN_MBOX_SETUP)
            .identifier("example.org|MBOX")
            .parentAsset(domainSetup)
            .build();
    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder() {
            return HsHostingAssetRbacEntity.builder()
                .type(EMAIL_ADDRESS)
                .parentAsset(domainMboxSetup)
                .identifier("old-local-part@example.org")
                .config(new HashMap<>(ofEntries(
                        entry("local-part", "old-local-part"),
                        entry("target", Array.of(
                                "xyz00",
                                "xyz00-abc",
                                "xyz00-xyz+list",
                                "office@example.com",
                                "/dev/null"
                        ))
                )));
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(EMAIL_ADDRESS);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=string, propertyName=local-part, matchesRegEx=[^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$], writeOnce=true}",
                "{type=string, propertyName=sub-domain, matchesRegEx=[^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$], writeOnce=true}",
                "{type=string[], propertyName=target, elementsOf={type=string, propertyName=target, matchesRegEx=[^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\.+_-]*)?$, ^([a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+)?@[a-zA-Z0-9.-]+$, ^nobody$, ^/dev/null$], maxLength=320}, required=true, minLength=1}");
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
                .config(new HashMap<>(ofEntries(
                        entry("local-part", "no@allowed"),
                        entry("sub-domain", "no@allowedeither"),
                        entry("target", Array.of(
                                "xyz00",
                                "xyz00-abc",
                                "garbage",
                                "office@example.com")))))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:old-local-part@example.org.config.local-part' is expected to match any of [^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$] but 'no@allowed' does not match",
                "'EMAIL_ADDRESS:old-local-part@example.org.config.sub-domain' is expected to match any of [^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+$] but 'no@allowedeither' does not match",
                "'EMAIL_ADDRESS:old-local-part@example.org.config.target' is expected to match any of [^[a-z][a-z0-9]{2}[0-9]{2}(-[a-z0-9][a-z0-9\\.+_-]*)?$, ^([a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+)?@[a-zA-Z0-9.-]+$, ^nobody$, ^/dev/null$] but 'garbage' does not match any");
    }

    @Test
    void rejectsOverwritingWriteOnceProperties() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .isLoaded(true)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        emailAddressHostingAssetEntity.getConfig().put("local-part", "new-local-part");
        emailAddressHostingAssetEntity.getConfig().put("sub-domain", "new-sub-domain");
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:old-local-part@example.org.config.local-part' is write-once but given as 'new-local-part'",
                "'EMAIL_ADDRESS:old-local-part@example.org.config.sub-domain' is write-once but given as 'new-sub-domain'");
    }

    @Test
    void rejectsRemovingWriteOnceProperties() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .config(new HashMap<>(ofEntries(
                        entry("local-part", "old-local-part"),
                        entry("sub-domain", "old-sub-domain"),
                        entry("target", Array.of("xyz00", "xyz00-abc", "office@example.com"))
                )))
                .isLoaded(true)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        emailAddressHostingAssetEntity.getConfig().remove("local-part");
        emailAddressHostingAssetEntity.getConfig().remove("sub-domain");
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:old-local-part@example.org.config.local-part' is write-once but got removed",
                "'EMAIL_ADDRESS:old-local-part@example.org.config.sub-domain' is write-once but got removed");
    }

    @Test
    void acceptsOverwritingWriteOncePropertiesWithSameValues() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .isLoaded(true)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        emailAddressHostingAssetEntity.getConfig().put("local-part", "old-local-part");
        emailAddressHostingAssetEntity.getConfig().remove("sub-domain"); // is not there anyway
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).isEmpty();
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
                "'identifier' expected to match '^\\Qold-local-part@example.org\\E$', but is 'abc00-office'");
    }

    @Test
    void validatesInvalidReferences() {
        // given
        final var emailAddressHostingAssetEntity = validEntityBuilder()
                .bookingItem(MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .parentAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
                .assignedToAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(emailAddressHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(emailAddressHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'EMAIL_ADDRESS:old-local-part@example.org.bookingItem' must be null but is of type MANAGED_SERVER",
                "'EMAIL_ADDRESS:old-local-part@example.org.parentAsset' must be of type DOMAIN_MBOX_SETUP but is of type MANAGED_SERVER",
                "'EMAIL_ADDRESS:old-local-part@example.org.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }
}
