package net.hostsharing.hsadminng.hs.office.relation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRepository extends Repository<HsOfficeRelationEntity, UUID> {

    Optional<HsOfficeRelationEntity> findByUuid(UUID id);

    default List<HsOfficeRelationEntity> findRelationRelatedToPersonUuidAndRelationType(@NotNull UUID personUuid, HsOfficeRelationType relationType) {
        return findRelationRelatedToPersonUuidAndRelationTypeString(personUuid, relationType.toString());
    }

    @Query(value = """
            SELECT p.* FROM hs_office_relation_rv AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
               """, nativeQuery = true)
    List<HsOfficeRelationEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    @Query(value = """
            SELECT p.* FROM hs_office_relation_rv AS p
                WHERE (:relationType IS NULL OR p.type = cast(:relationType AS HsOfficeRelationType))
                    AND ( p.anchorUuid = :personUuid OR p.holderUuid = :personUuid)
               """, nativeQuery = true)
    List<HsOfficeRelationEntity> findRelationRelatedToPersonUuidAndRelationTypeString(@NotNull UUID personUuid, String relationType);

    HsOfficeRelationEntity save(final HsOfficeRelationEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
