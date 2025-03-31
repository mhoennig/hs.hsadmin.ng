package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainMboxHostingAssetValidatorUnitTest {

    static final HsHostingAssetRealEntity validDomainSetupEntity = HsHostingAssetRealEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org")
                .build();

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder validEntityBuilder() {
        return HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_MBOX_SETUP)
                .parentAsset(validDomainSetupEntity)
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(MANAGED_WEBSPACE).build())
                .identifier("example.org|MBOX");
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(DOMAIN_MBOX_SETUP);

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
        assertThat(givenEntity.getIdentifier()).isEqualTo("example.org|MBOX");
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
                "'identifier' expected to match '^\\Qexample.org|MBOX\\E$' but is 'example.org'"
        );
    }

    @Test
    void acceptsValidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier(validDomainSetupEntity.getIdentifier()+"|MBOX").build();
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
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(null)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_MBOX_SETUP:example.org|MBOX.bookingItem' must be null but is of type CLOUD_SERVER",
                "'DOMAIN_MBOX_SETUP:example.org|MBOX.parentAsset' must be of type DOMAIN_SETUP but is of type MANAGED_WEBSPACE",
                "'DOMAIN_MBOX_SETUP:example.org|MBOX.assignedToAsset' must be of type MANAGED_WEBSPACE but is null");
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
                "'DOMAIN_MBOX_SETUP:example.org|MBOX.config.any' is not expected but is set to 'false'");
    }
}
