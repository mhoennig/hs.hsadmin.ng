package net.hostsharing.hsadminng.hs.office.coopassets;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeCoopAssetsTransactionRealRepository extends Repository<HsOfficeCoopAssetsTransactionRealEntity, UUID> {

    @Timed("app.office.coopAssets.realRepo.findByUuid")
    Optional<HsOfficeCoopAssetsTransactionRealEntity> findByUuid(UUID uuid);

    @Timed("app.office.coopAssets.realRepo.findByUuidIn")
    List<HsOfficeCoopAssetsTransactionRealEntity> findByUuidIn(Collection<UUID> uuids);
}
