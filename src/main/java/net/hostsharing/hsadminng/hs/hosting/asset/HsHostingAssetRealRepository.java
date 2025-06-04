package net.hostsharing.hsadminng.hs.hosting.asset;

import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!only-prod-schema")
public interface HsHostingAssetRealRepository extends HsHostingAssetRepository<HsHostingAssetRealEntity>, Repository<HsHostingAssetRealEntity, UUID> {

    @Timed("app.hostingAsset.repo.findByUuid.real")
    Optional<HsHostingAssetRealEntity> findByUuid(final UUID serverUuid);

    @Timed("app.hostingAsset.repo.findByIdentifier.real")
    List<HsHostingAssetRealEntity> findByIdentifier(String assetIdentifier);

    default List<HsHostingAssetRealEntity> findByTypeAndIdentifier(@NotNull HsHostingAssetType type,  @NotNull String identifier) {
        return findByTypeAndIdentifierImpl(type.name(), identifier);
    }

    @Query("""
            select ha
              from HsHostingAssetRealEntity ha
             where cast(ha.type as String) = :type
               and ha.identifier = :identifier
            """)
    @Timed("app.hostingAsset.repo.findByTypeAndIdentifierImpl.real")
    List<HsHostingAssetRealEntity> findByTypeAndIdentifierImpl(@NotNull String type, @NotNull String identifier);

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
            from hs_hosting.asset_rv ha
                left join hs_booking.item bi on bi.uuid = ha.bookingitemuuid
                left join hs_hosting.asset pha on pha.uuid = ha.parentassetuuid
            where (:projectUuid is null or bi.projectuuid=:projectUuid)
              and (:parentAssetUuid is null or pha.uuid=:parentAssetUuid)
              and (:type is null or :type=cast(ha.type as text))
    """, nativeQuery = true)
    // The JPQL query did not generate "left join" but just "join".
    // I also optimized the query by not using the _rv for hs_booking.item and hs_hosting.asset, only for hs_hosting.asset_rv.
    @Timed("app.hostingAsset.repo.findAllByCriteriaImpl.real")
    List<HsHostingAssetRealEntity> findAllByCriteriaImpl(UUID projectUuid, UUID parentAssetUuid, String type);

    default List<HsHostingAssetRealEntity> findAllByCriteria(final UUID projectUuid, final UUID parentAssetUuid, final HsHostingAssetType type) {
        return findAllByCriteriaImpl(projectUuid, parentAssetUuid, HsHostingAssetType.asString(type));
    }

    @Timed("app.hostingAsset.repo.save.real")
    HsHostingAssetRealEntity save(HsHostingAssetRealEntity current);

    @Timed("app.hostingAsset.repo.deleteByUuid.real")
    int deleteByUuid(final UUID uuid);

    @Timed("app.hostingAsset.repo.count.real")
    long count();
}
