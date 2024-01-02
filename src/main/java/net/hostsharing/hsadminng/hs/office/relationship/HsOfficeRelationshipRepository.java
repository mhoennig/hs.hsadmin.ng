package net.hostsharing.hsadminng.hs.office.relationship;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationshipRepository extends Repository<HsOfficeRelationshipEntity, UUID> {

    Optional<HsOfficeRelationshipEntity> findByUuid(UUID id);

    default List<HsOfficeRelationshipEntity> findRelationshipRelatedToPersonUuidAndRelationshipType(@NotNull UUID personUuid, HsOfficeRelationshipType relationshipType) {
        return findRelationshipRelatedToPersonUuidAndRelationshipTypeString(personUuid, relationshipType.toString());
    }

    @Query(value = """
            SELECT p.* FROM hs_office_relationship_rv AS p
                WHERE p.relAnchorUuid = :personUuid OR p.relHolderUuid = :personUuid
               """, nativeQuery = true)
    List<HsOfficeRelationshipEntity> findRelationshipRelatedToPersonUuid(@NotNull UUID personUuid);

    @Query(value = """
            SELECT p.* FROM hs_office_relationship_rv AS p
                WHERE (:relationshipType IS NULL OR p.relType = cast(:relationshipType AS HsOfficeRelationshipType))
                    AND ( p.relAnchorUuid = :personUuid OR p.relHolderUuid = :personUuid)
               """, nativeQuery = true)
    List<HsOfficeRelationshipEntity> findRelationshipRelatedToPersonUuidAndRelationshipTypeString(@NotNull UUID personUuid, String relationshipType);

    HsOfficeRelationshipEntity save(final HsOfficeRelationshipEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
