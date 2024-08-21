package net.hostsharing.hsadminng.hs.hosting.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsHostingAssetRepository<E extends HsHostingAsset> {

    Optional<E> findByUuid(final UUID serverUuid);

    List<E> findByIdentifier(String assetIdentifier);

    List<E> findAllByCriteriaImpl(UUID projectUuid, UUID parentAssetUuid, String type);

    default List<E> findAllByCriteria(final UUID projectUuid, final UUID parentAssetUuid, final HsHostingAssetType type) {
        return findAllByCriteriaImpl(projectUuid, parentAssetUuid, HsHostingAssetType.asString(type));
    }

    E save(HsHostingAsset current);

    int deleteByUuid(final UUID uuid);

    long count();
}
