package net.hostsharing.hsadminng.hs.hosting.asset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface HsHostingAssetRepository extends Repository<HsHostingAssetEntity, UUID> {

    List<HsHostingAssetEntity> findAll();
    Optional<HsHostingAssetEntity> findByUuid(final UUID serverUuid);

    @Query("""
        SELECT asset FROM HsHostingAssetEntity asset
            WHERE (:projectUuid IS NULL OR asset.bookingItem.project.uuid = :projectUuid)
              AND (:parentAssetUuid IS NULL OR asset.parentAsset.uuid = :parentAssetUuid)
              AND (:type IS NULL OR :type = CAST(asset.type AS String))
    """)
    List<HsHostingAssetEntity> findAllByCriteriaImpl(UUID projectUuid, UUID parentAssetUuid, String type);
    default List<HsHostingAssetEntity> findAllByCriteria(final UUID projectUuid, final UUID parentAssetUuid, final HsHostingAssetType type) {
        return findAllByCriteriaImpl(projectUuid, parentAssetUuid, HsHostingAssetType.asString(type));
    }

    HsHostingAssetEntity save(HsHostingAssetEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
