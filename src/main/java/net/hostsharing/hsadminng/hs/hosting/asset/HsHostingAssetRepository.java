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
        SELECT s FROM HsHostingAssetEntity s
            WHERE s.bookingItem.debitor.uuid = :debitorUuid
    """)
    List<HsHostingAssetEntity> findAllByDebitorUuid(final UUID debitorUuid);

    HsHostingAssetEntity save(HsHostingAssetEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
