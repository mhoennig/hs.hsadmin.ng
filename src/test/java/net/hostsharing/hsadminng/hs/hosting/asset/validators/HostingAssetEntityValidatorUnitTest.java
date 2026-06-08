package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.*;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static org.assertj.core.api.Assertions.assertThat;

class HostingAssetEntityValidatorUnitTest {

    // Custom test validator with a CPU total-limit property to exercise validateMaxTotalValue.
    // No actual hosting-asset type uses asTotalLimit(), so a dedicated subclass is the only way
    // to reach that code path in a unit test.
    private static class TestValidatorWithCpuTotalLimit extends HostingAssetEntityValidator {

        TestValidatorWithCpuTotalLimit() {
            super(MANAGED_SERVER, AlarmContact.isOptional(),
                    integerProperty("CPU").min(1).max(100).required().asTotalLimit());
        }

        @Override
        protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
            return Pattern.compile("^vm[0-9][0-9][0-9][0-9]$");
        }
    }

    // === validateIdentifierPattern ===

    @Test
    void validateEntityReportsNullIdentifier() {
        // given — valid references but null identifier, covering the null-identifier branch
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier(null)
                .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '^vm[0-9][0-9][0-9][0-9]$' but is 'null'");
    }

    // === case TERMINATORY: ===

    @Test
    void validateEntityRejectsTerminatoryBookingItemOfWrongTypeWhenParentAssetIsNull() {
        // given — TERMINATORY: parentAsset is null but bookingItem has a wrong non-null type,
        //         covering the "must be of type ... but is of type ..." branch
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org")
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then
        assertThat(result).contains(
                "'DOMAIN_SETUP:example.org.bookingItem' must be of type DOMAIN_SETUP but is of type CLOUD_SERVER");
    }

    @Test
    void validateEntityAcceptsTerminatoryWhenParentAssetIsSetAndBookingItemIsNull() {
        // given — TERMINATORY: parentAsset is set and bookingItem is null,
        //         both conditions in the TERMINATORY case are skipped → break
        //         isLoaded skips DNS verification to keep test self-contained
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("sub.example.org")
                .parentAsset(HsHostingAssetRealEntity.builder()
                        .type(DOMAIN_SETUP)
                        .identifier("example.org")
                        .build())
                .isLoaded(true)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then — no bookingItem reference error; TERMINATORY break was reached
        assertThat(result).isEmpty();
    }

    // === case OPTIONAL: ===

    @Test
    void validateEntityAcceptsOptionalWithCorrectNonNullType() {
        // given — OPTIONAL parentAsset with a correct type (MANAGED_SERVER for MANAGED_WEBSPACE),
        //         covering the type-matches → break path
        //         isLoaded avoids resolving the debitor prefix from a project
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .identifier("abc00")
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(MANAGED_SERVER).identifier("vm1234").build())
                .isLoaded(true)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then — no parentAsset error; OPTIONAL break was reached for parentAsset
        assertThat(result).isEmpty();
    }

    @Test
    void validateEntityRejectsOptionalWithWrongNonNullType() {
        // given — OPTIONAL parentAsset with a wrong type (CLOUD_SERVER for MANAGED_WEBSPACE)
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_WEBSPACE)
                .identifier("abc00")
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.MANAGED_WEBSPACE).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(CLOUD_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then
        assertThat(result).contains(
                "'MANAGED_WEBSPACE:abc00.parentAsset' must be null or of type MANAGED_SERVER but is of type CLOUD_SERVER");
    }

    // === case FORBIDDEN: ===

    @Test
    void validateEntityAcceptsForbiddenWhenEntityIsNull() {
        // given — FORBIDDEN assignedToAsset for CLOUD_SERVER when it is not set → break
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then — no assignedToAsset error; FORBIDDEN break was reached
        assertThat(result).isEmpty();
    }

    @Test
    void validateEntityRejectsForbiddenWhenEntityIsSet() {
        // given — FORBIDDEN assignedToAsset for CLOUD_SERVER is actually set
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(CLOUD_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());

        // when
        final var result = validator.validateEntity(entity);

        // then
        assertThat(result).containsExactly(
                "'CLOUD_SERVER:vm1234.assignedToAsset' must be null but is of type CLOUD_SERVER");
    }

    // === validateContext / optionallyValidate / validateAgainstSubEntities ===

    @Test
    void validateContextWithNullBookingItemAndParentAssetReturnsEmpty() {
        // given — CLOUD_SERVER with neither bookingItem nor parentAsset:
        //         both optionallyValidate calls return empty (null-entity path),
        //         and validateAgainstSubEntities produces no totals errors
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // when
        final var result = validator.validateContext(entity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validateContextWithNonNullBookingItemDelegatesToBookingItemValidator() {
        // given — CLOUD_SERVER with a bookingItem: covers the non-null path of optionallyValidate(HsBookingItem)
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // when
        final var result = validator.validateContext(entity);

        // then — booking item validateContext produces no errors (simple entity, no sub-items)
        assertThat(result).isEmpty();
    }

    @Test
    void validateContextWithNonNullParentAssetDelegatesToParentAssetValidator() {
        // given — entity with a parentAsset: covers the non-null path of optionallyValidate(HsHostingAsset)
        //         The CLOUD_SERVER parent has no bookingItem or further parents, so its validateContext is trivial
        final var cloudServerParent = HsHostingAssetRealEntity.builder()
                .type(CLOUD_SERVER)
                .identifier("vm1234")
                .build();
        final var entity = HsHostingAssetRbacEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm5678")
                .parentAsset(cloudServerParent)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(MANAGED_SERVER);

        // when
        final var result = validator.validateContext(entity);

        // then — parent validateContext produces no errors
        assertThat(result).isEmpty();
    }

    // === validateMaxTotalValue ===

    @Test
    void validateAgainstSubEntitiesDetectsExceededTotal() {
        // given — custom validator with CPU total limit; sub-entity exceeds max (totalValue > maxValue)
        final var validator = new TestValidatorWithCpuTotalLimit();
        final var mainEntity = HsHostingAssetRealEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .config(Map.of("CPU", 2))   // max total CPU = 2
                .build();
        mainEntity.getSubHostingAssets().add(HsHostingAssetRealEntity.builder()
                .type(MANAGED_WEBSPACE)
                .config(Map.of("CPU", 3))   // sub-entity allocates 3 → 3 > 2
                .build());

        // when
        final var result = validator.validateAgainstSubEntities(mainEntity);

        // then
        assertThat(result).containsExactly(
                "'MANAGED_SERVER:vm1234.config.CPU' maximum total is 2, but actual total CPU is 3");
    }

    @Test
    void validateAgainstSubEntitiesAcceptsWhenTotalIsWithinLimit() {
        // given — custom validator with CPU total limit; sub-entity is within max (totalValue <= maxValue)
        final var validator = new TestValidatorWithCpuTotalLimit();
        final var mainEntity = HsHostingAssetRealEntity.builder()
                .type(MANAGED_SERVER)
                .identifier("vm1234")
                .config(Map.of("CPU", 5))   // max total CPU = 5
                .build();
        mainEntity.getSubHostingAssets().add(HsHostingAssetRealEntity.builder()
                .type(MANAGED_WEBSPACE)
                .config(Map.of("CPU", 3))   // sub-entity allocates 3 → 3 <= 5
                .build());

        // when
        final var result = validator.validateAgainstSubEntities(mainEntity);

        // then
        assertThat(result).isEmpty();
    }
}
