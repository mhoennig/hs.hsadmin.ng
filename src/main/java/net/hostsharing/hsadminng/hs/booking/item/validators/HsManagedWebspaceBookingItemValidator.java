package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.validation.IntegerProperty;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_EMAIL_MAILBOX_SETUP;
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
            integerProperty("SSD").unit("GB").min(1).max(100).step(1).required(),
            integerProperty("HDD").unit("GB").min(0).max(250).step(10).optional(),
            integerProperty("Traffic").unit("GB").min(10).max(1000).step(10).required(),
            integerProperty("Multi").min(1).max(100).step(1).withDefault(1)
                    .eachComprising( 25, unixUsers())
                    .eachComprising(  5, databaseUsers())
                    .eachComprising(  5, databases())
                    .eachComprising(250, eMailAddresses()),
            integerProperty("Daemons").min(0).max(10).withDefault(0),
            booleanProperty("Online Office Server").optional(),
            enumerationProperty("SLA-Platform").values("BASIC", "EXT24H").withDefault("BASIC")
        );
    }

    private static TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> unixUsers() {
        return (final HsBookingItemEntity entity, final IntegerProperty prop, final Integer factor) -> {
            final var unixUserCount = ofNullable(entity.getRelatedHostingAsset())
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

    private static TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> databaseUsers() {
        return (final HsBookingItemEntity entity, final IntegerProperty prop, final Integer factor) -> {
            final var dbUserCount = ofNullable(entity.getRelatedHostingAsset())
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

    private static TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> databases() {
        return (final HsBookingItemEntity entity, final IntegerProperty prop, final Integer factor) -> {
            final var unixUserCount = ofNullable(entity.getRelatedHostingAsset())
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

    private static TriFunction<HsBookingItemEntity, IntegerProperty, Integer, List<String>> eMailAddresses() {
        return (final HsBookingItemEntity entity, final IntegerProperty prop, final Integer factor) -> {
            final var unixUserCount = ofNullable(entity.getRelatedHostingAsset())
                    .map(ha -> ha.getSubHostingAssets().stream()
                        .filter(bi -> bi.getType() == DOMAIN_EMAIL_MAILBOX_SETUP)
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
}
