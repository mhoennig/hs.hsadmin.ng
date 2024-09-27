package net.hostsharing.hsadminng.hs.office.relation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRbacRepository extends Repository<HsOfficeRelationRbacEntity, UUID> {

    Optional<HsOfficeRelationRbacEntity> findByUuid(UUID id);

    default List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidAndRelationType(@NotNull UUID personUuid, HsOfficeRelationType relationType) {
        return findRelationRelatedToPersonUuidAndRelationTypeString(personUuid, relationType == null ? null : relationType.toString());
    }

    @Query(value = """
            SELECT p.* FROM hs_office.relation_rv AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
            """, nativeQuery = true)
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    @Query(value = """
            SELECT p.* FROM hs_office.relation_rv AS p
                WHERE (:relationType IS NULL OR p.type = cast(:relationType AS hs_office.RelationType))
                    AND ( p.anchorUuid = :personUuid OR p.holderUuid = :personUuid)
            """, nativeQuery = true)
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidAndRelationTypeString(@NotNull UUID personUuid, String relationType);

    HsOfficeRelationRbacEntity save(final HsOfficeRelationRbacEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
