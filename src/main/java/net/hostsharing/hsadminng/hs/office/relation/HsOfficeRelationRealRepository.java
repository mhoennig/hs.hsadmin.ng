package net.hostsharing.hsadminng.hs.office.relation;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRealRepository extends Repository<HsOfficeRelationRealEntity, UUID> {

    @Timed("app.repo.relations.findByUuid.real")
    Optional<HsOfficeRelationRealEntity> findByUuid(UUID id);

    default List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidAndRelationType(@NotNull UUID personUuid, HsOfficeRelationType relationType) {
        return findRelationRelatedToPersonUuidAndRelationTypeString(personUuid, relationType == null ? null : relationType.toString());
    }

    @Query(value = """
            SELECT p.* FROM hs_office.relation AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
               """, nativeQuery = true)
    @Timed("app.repo.relations.findRelationRelatedToPersonUuid.real")
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    @Query(value = """
            SELECT p.* FROM hs_office.relation AS p
                WHERE (:relationType IS NULL OR p.type = cast(:relationType AS hs_office.RelationType))
                    AND ( p.anchorUuid = :personUuid OR p.holderUuid = :personUuid)
               """, nativeQuery = true)
    @Timed("app.repo.relations.findRelationRelatedToPersonUuidAndRelationTypeString.real")
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidAndRelationTypeString(@NotNull UUID personUuid, String relationType);

    @Timed("app.repo.relations.save.real")
    HsOfficeRelationRealEntity save(final HsOfficeRelationRealEntity entity);

    @Timed("app.repo.relations.count.real")
    long count();

    @Timed("app.repo.relations.deleteByUuid.real")
    int deleteByUuid(UUID uuid);
}
