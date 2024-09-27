package net.hostsharing.hsadminng.hs.office.relation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRealRepository extends Repository<HsOfficeRelationRealEntity, UUID> {

    Optional<HsOfficeRelationRealEntity> findByUuid(UUID id);

    default List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidAndRelationType(@NotNull UUID personUuid, HsOfficeRelationType relationType) {
        return findRelationRelatedToPersonUuidAndRelationTypeString(personUuid, relationType == null ? null : relationType.toString());
    }

    @Query(value = """
            SELECT p.* FROM hs_office.relation AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
               """, nativeQuery = true)
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    @Query(value = """
            SELECT p.* FROM hs_office.relation AS p
                WHERE (:relationType IS NULL OR p.type = cast(:relationType AS hs_office.RelationType))
                    AND ( p.anchorUuid = :personUuid OR p.holderUuid = :personUuid)
               """, nativeQuery = true)
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidAndRelationTypeString(@NotNull UUID personUuid, String relationType);

    HsOfficeRelationRealEntity save(final HsOfficeRelationRealEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
