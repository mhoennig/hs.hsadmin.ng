package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsPostgreSqlDbInstanceHostingAssetValidator.DEFAULT_INSTANCE_IDENTIFIER_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;

class HsPostgreSqlInstanceHostingAssetValidatorUnitTest {

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder() {
        return HsHostingAssetRbacEntity.builder()
                .type(PGSQL_INSTANCE)
                .parentAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
                .identifier(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY.getIdentifier() + DEFAULT_INSTANCE_IDENTIFIER_SUFFIX);
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(PGSQL_INSTANCE);

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
        assertThat(givenEntity.getIdentifier()).isEqualTo("vm1234|PgSql.default");
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
        final var givenEntity = validEntityBuilder().identifier("PostgreSQL").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^\\Qvm1234|PgSql.default\\E$' but is 'PostgreSQL'"
        );
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(MANAGED_WEBSPACE).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'PGSQL_INSTANCE:vm1234|PgSql.default.bookingItem' must be null but is of type CLOUD_SERVER",
                "'PGSQL_INSTANCE:vm1234|PgSql.default.parentAsset' must be of type MANAGED_SERVER but is of type MANAGED_WEBSPACE",
                "'PGSQL_INSTANCE:vm1234|PgSql.default.assignedToAsset' must be null but is of type MANAGED_WEBSPACE");
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
                "'PGSQL_INSTANCE:vm1234|PgSql.default.config.any' is not expected but is set to 'false'");
    }
}
