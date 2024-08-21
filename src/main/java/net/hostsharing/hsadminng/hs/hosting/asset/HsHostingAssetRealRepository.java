package net.hostsharing.hsadminng.hs.hosting.asset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsHostingAssetRealRepository extends HsHostingAssetRepository<HsHostingAssetRealEntity>, Repository<HsHostingAssetRealEntity, UUID> {

    Optional<HsHostingAssetRealEntity> findByUuid(final UUID serverUuid);

    List<HsHostingAssetRealEntity> findByIdentifier(String assetIdentifier);

    @Query(value = """
        select ha.uuid,
               ha.alarmcontactuuid,
               ha.assignedtoassetuuid,
               ha.bookingitemuuid,
               ha.caption,
               ha.config,
               ha.identifier,
               ha.parentassetuuid,
               ha.type,
               ha.version
            from hs_hosting_asset_rv ha
                left join hs_booking_item bi on bi.uuid = ha.bookingitemuuid
                left join hs_hosting_asset pha on pha.uuid = ha.parentassetuuid
            where (:projectUuid is null or bi.projectuuid=:projectUuid)
              and (:parentAssetUuid is null or pha.uuid=:parentAssetUuid)
              and (:type is null or :type=cast(ha.type as text))
    """, nativeQuery = true)
    // The JPQL query did not generate "left join" but just "join".
    // I also optimized the query by not using the _rv for hs_booking_item and hs_hosting_asset, only for hs_hosting_asset_rv.
    List<HsHostingAssetRealEntity> findAllByCriteriaImpl(UUID projectUuid, UUID parentAssetUuid, String type);
    default List<HsHostingAssetRealEntity> findAllByCriteria(final UUID projectUuid, final UUID parentAssetUuid, final HsHostingAssetType type) {
        return findAllByCriteriaImpl(projectUuid, parentAssetUuid, HsHostingAssetType.asString(type));
    }

    HsHostingAssetRealEntity save(HsHostingAssetRealEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
