package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.hs.validation.IntegerProperty;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_MBOX_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.EMAIL_ADDRESS;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedWebspaceBookingItemValidator extends HsBookingItemEntityValidator {

    public HsManagedWebspaceBookingItemValidator() {
        super(
            integerProperty("SSD").unit("GB").min(1).max(2000).step(1).required(),
            integerProperty("HDD").unit("GB").min(0).max(10000).step(10).optional(),
            integerProperty("Traffic").unit("GB").min(10).max(64000).step(10).requiresAtMaxOneOf("Bandwidth", "Traffic"),
            integerProperty("Bandwidth").unit("GB").min(10).max(1000).step(10).requiresAtMaxOneOf("Bandwidth", "Traffic"), // TODO.spec
            integerProperty("Multi").min(1).max(100).step(1).withDefault(1)
                    .eachComprising( 25, unixUsers())
                    .eachComprising(  5, databaseUsers())
                    .eachComprising(  5, databases())
                    .eachComprising(250, eMailAddresses()),
            integerProperty("Daemons").min(0).max(16).withDefault(0),
            booleanProperty("Online Office Server").optional(), // TODO.impl: shorten to "Office"
            enumerationProperty("SLA-Platform").values("BASIC", "EXT24H").withDefault("BASIC")
        );
    }

    private static TriFunction<HsBookingItem, IntegerProperty<?>, Integer, List<String>> unixUsers() {
        return (final HsBookingItem entity, final IntegerProperty<?> prop, final Integer factor) -> {
            final var unixUserCount = fetchRelatedBookingItem(entity)
                    .map(ha -> ha.getSubHostingAssets().stream()
                            .filter(subAsset -> subAsset.getType() == UNIX_USER)
                        .count())
                .orElse(0L);
            final long limitingValue = prop.getValue(entity.getResources());
            if (unixUserCount > factor*limitingValue) {
                return List.of(prop.propertyName() + "=" + limitingValue + " allows at maximum " + limitingValue*factor + " unix users, but " + unixUserCount + " found");
            }
            return emptyList();
        };
    }

    private static TriFunction<HsBookingItem, IntegerProperty<?>, Integer, List<String>> databaseUsers() {
        return (final HsBookingItem entity, final IntegerProperty<?> prop, final Integer factor) -> {
            final var dbUserCount = fetchRelatedBookingItem(entity)
                    .map(ha -> ha.getSubHostingAssets().stream()
                            .filter(bi -> bi.getType() == PGSQL_USER || bi.getType() == MARIADB_USER )
                            .count())
                    .orElse(0L);
            final long limitingValue = prop.getValue(entity.getResources());
            if (dbUserCount > factor*limitingValue) {
                return List.of(prop.propertyName() + "=" + limitingValue + " allows at maximum " + limitingValue*factor + " database users, but " + dbUserCount + " found");
            }
            return emptyList();
        };
    }

    private static TriFunction<HsBookingItem, IntegerProperty<?>, Integer, List<String>> databases() {
        return (final HsBookingItem entity, final IntegerProperty<?> prop, final Integer factor) -> {
            final var unixUserCount = fetchRelatedBookingItem(entity)
                    .map(ha -> ha.getSubHostingAssets().stream()
                        .filter(bi -> bi.getType()==PGSQL_USER || bi.getType()==MARIADB_USER )
                        .flatMap(domainEMailSetup -> domainEMailSetup.getSubHostingAssets().stream()
                            .filter(subAsset -> subAsset.getType()==PGSQL_DATABASE || subAsset.getType()==MARIADB_DATABASE))
                        .count())
                    .orElse(0L);
            final long limitingValue = prop.getValue(entity.getResources());
            if (unixUserCount > factor*limitingValue) {
                return List.of(prop.propertyName() + "=" + limitingValue + " allows at maximum " + limitingValue*factor + " databases, but " + unixUserCount + " found");
            }
            return emptyList();
        };
    }

    private static TriFunction<HsBookingItem, IntegerProperty<?>, Integer, List<String>> eMailAddresses() {
        return (final HsBookingItem entity, final IntegerProperty<?> prop, final Integer factor) -> {
            final var unixUserCount = fetchRelatedBookingItem(entity)
                    .map(ha -> ha.getSubHostingAssets().stream()
                        .filter(bi -> bi.getType() == DOMAIN_MBOX_SETUP)
                        .flatMap(domainEMailSetup -> domainEMailSetup.getSubHostingAssets().stream()
                            .filter(subAsset -> subAsset.getType()==EMAIL_ADDRESS))
                        .count())
                    .orElse(0L);
            final long limitingValue = prop.getValue(entity.getResources());
            if (unixUserCount > factor*limitingValue) {
                return List.of(prop.propertyName() + "=" + limitingValue + " allows at maximum " + limitingValue*factor + " databases, but " + unixUserCount + " found");
            }
            return emptyList();
        };
    }

    private static Optional<HsHostingAssetRealEntity> fetchRelatedBookingItem(final HsBookingItem entity) {
        // TODO.perf: maybe we need to cache the result at least for a single valiationrun
        return HsEntityValidator.localEntityManager.get().createQuery(
                        "SELECT asset FROM HsHostingAssetRealEntity asset WHERE asset.bookingItem.uuid=:bookingItemUuid",
                        HsHostingAssetRealEntity.class)
                .setParameter("bookingItemUuid", entity.getUuid())
                .getResultStream().findFirst(); // there are 0 or 1, never more
    }
}
