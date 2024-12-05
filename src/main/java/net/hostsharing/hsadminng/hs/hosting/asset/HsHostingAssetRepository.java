package net.hostsharing.hsadminng.hs.hosting.asset;

import io.micrometer.core.annotation.Timed;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsHostingAssetRepository<E extends HsHostingAsset> {

    @Timed("app.hosting.assets.repo.findByUuid")
    Optional<E> findByUuid(final UUID serverUuid);

    @Timed("app.hosting.assets.repo.findByIdentifier")
    List<E> findByIdentifier(String assetIdentifier);

    @Timed("app.hosting.assets.repo.findAllByCriteriaImpl")
    List<E> findAllByCriteriaImpl(UUID projectUuid, UUID parentAssetUuid, String type);

    default List<E> findAllByCriteria(final UUID projectUuid, final UUID parentAssetUuid, final HsHostingAssetType type) {
        return findAllByCriteriaImpl(projectUuid, parentAssetUuid, HsHostingAssetType.asString(type));
    }

    @Timed("app.hosting.assets.repo.save")
    E save(HsHostingAsset current);

    @Timed("app.hosting.assets.repo.deleteByUuid")
    int deleteByUuid(final UUID uuid);

    @Timed("app.hosting.assets.repo.count")
    long count();
}
