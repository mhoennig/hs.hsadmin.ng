package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SMTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsMariaDbInstanceHostingAssetValidator.DEFAULT_INSTANCE_IDENTIFIER_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;

class HsPostgreSqlInstanceHostingAssetValidatorUnitTest {

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(MARIADB_INSTANCE)
                .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                .identifier(TEST_MANAGED_SERVER_HOSTING_ASSET.getIdentifier() + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX);
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
        assertThat(givenEntity.getParentAsset().getIdentifier()).as("precondition failed").isEqualTo("vm1234");
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        validator.preprocessEntity(givenEntity);

        // then
        assertThat(givenEntity.getIdentifier()).isEqualTo("vm1234|MariaDB.default");
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

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier("example.org").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^\\Qvm1234|MariaDB.default\\E$', but is 'example.org'"
        );
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MARIADB_INSTANCE:vm1234|MariaDB.default.bookingItem' must be null but is of type CLOUD_SERVER",
                "'MARIADB_INSTANCE:vm1234|MariaDB.default.parentAsset' must be of type MANAGED_SERVER but is of type MANAGED_WEBSPACE",
                "'MARIADB_INSTANCE:vm1234|MariaDB.default.assignedToAsset' must be null but is of type MANAGED_WEBSPACE");
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
                "'MARIADB_INSTANCE:vm1234|MariaDB.default.config.any' is not expected but is set to 'false'");
    }
}
