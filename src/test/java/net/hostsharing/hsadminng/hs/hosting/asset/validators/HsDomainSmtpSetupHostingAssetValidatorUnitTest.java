package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainSmtpSetupHostingAssetValidatorUnitTest {

    static final HsHostingAssetEntity validDomainSetupEntity = HsHostingAssetEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org")
                .build();

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(DOMAIN_SMTP_SETUP)
                .parentAsset(validDomainSetupEntity)
                .assignedToAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).build())
                .identifier("example.org|SMTP");
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(DOMAIN_SMTP_SETUP);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }

    @Test
    void preprocessesTakesIdentifierFromParent() {
        // given
        final var givenEntity = validEntityBuilder().build();
        assertThat(givenEntity.getParentAsset().getIdentifier()).as("precondition failed").isEqualTo("example.org");
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        validator.preprocessEntity(givenEntity);

        // then
        assertThat(givenEntity.getIdentifier()).isEqualTo("example.org|SMTP");
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier("example.org").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^\\Qexample.org|SMTP\\E$', but is 'example.org'"
        );
    }

    @Test
    void acceptsValidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier(validDomainSetupEntity.getIdentifier()+"|SMTP").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(null)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_SMTP_SETUP:example.org|SMTP.bookingItem' must be null but is of type CLOUD_SERVER",
                "'DOMAIN_SMTP_SETUP:example.org|SMTP.parentAsset' must be of type DOMAIN_SETUP but is of type MANAGED_WEBSPACE",
                "'DOMAIN_SMTP_SETUP:example.org|SMTP.assignedToAsset' must be of type MANAGED_WEBSPACE but is null");
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var errors = validator.validateEntity(givenEntity);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("any", "false")
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_SMTP_SETUP:example.org|SMTP.config.any' is not expected but is set to 'false'");
    }
}
