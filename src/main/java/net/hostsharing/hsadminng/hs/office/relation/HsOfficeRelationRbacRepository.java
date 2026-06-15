package net.hostsharing.hsadminng.hs.office.relation;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRbacRepository extends Repository<HsOfficeRelationRbacEntity, UUID> {

    @Timed("app.office.relations.repo.findByUuid.rbac")
    Optional<HsOfficeRelationRbacEntity> findByUuid(UUID id);

    @Query(value = """
            SELECT p.* FROM hs_office.relation_rv AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
            """, nativeQuery = true)
    @Timed("app.office.relations.repo.findRelationRelatedToPersonUuid.rbac")
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    // TODO: Or use jsonb_path with RegEx like emailAddressRegEx in ContactRepo?
    @Query(value = """
            SELECT rel FROM HsOfficeRelationRbacEntity AS rel
                WHERE (:#{#criteria.relationType} IS NULL OR CAST(rel.type AS String) = :#{#criteria.getRelationTypeString()})
                    AND ( :#{#criteria.personUuid} IS NULL
                            OR rel.anchor.uuid = :#{#criteria.personUuid} OR rel.holder.uuid = :#{#criteria.personUuid} )
                    AND ( :#{#criteria.anchorPersonUuid} IS NULL OR rel.anchor.uuid = :#{#criteria.anchorPersonUuid} )
                    AND ( :#{#criteria.holderPersonUuid} IS NULL OR rel.holder.uuid = :#{#criteria.holderPersonUuid} )
                    AND ( :#{#criteria.mark} IS NULL OR rel.mark ILIKE :#{#criteria.getMarkPattern()} )
                    AND ( :#{#criteria.personData} IS NULL
                            OR rel.anchor.tradeName ILIKE :#{#criteria.getPersonDataPattern()} OR rel.holder.tradeName ILIKE :#{#criteria.getPersonDataPattern()}
                            OR rel.anchor.familyName ILIKE :#{#criteria.getPersonDataPattern()} OR rel.holder.familyName ILIKE :#{#criteria.getPersonDataPattern()}
                            OR rel.anchor.givenName ILIKE :#{#criteria.getPersonDataPattern()} OR rel.holder.givenName ILIKE :#{#criteria.getPersonDataPattern()} )
                    AND ( :#{#criteria.contactData} IS NULL
                            OR rel.contact.caption ILIKE :#{#criteria.getContactDataPattern()}
                            OR CAST(rel.contact.postalAddress AS String) ILIKE :#{#criteria.getContactDataPattern()}
                            OR CAST(rel.contact.emailAddresses AS String) ILIKE :#{#criteria.getContactDataPattern()}
                            OR CAST(rel.contact.phoneNumbers AS String) ILIKE :#{#criteria.getContactDataPattern()} )
                    AND ( :#{#criteria.contactUuid} IS NULL OR rel.contact.uuid = :#{#criteria.contactUuid} )
            """)
    @Timed("app.office.relations.repo.findRelation.rbac")
    List<HsOfficeRelationRbacEntity> findRelations(@Param("criteria") HsOfficeRelationSearchCriteria criteria);

    @Timed("app.office.relations.repo.save.rbac")
    HsOfficeRelationRbacEntity save(final HsOfficeRelationRbacEntity entity);

    @Timed("app.office.relations.repo.count.rbac")
    long count();

    @Timed("app.office.relations.repo.deleteByUuid.rbac")
    int deleteByUuid(UUID uuid);
}
