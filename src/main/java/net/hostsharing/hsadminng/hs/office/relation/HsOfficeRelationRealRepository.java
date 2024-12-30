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

    @Query(value = """
            SELECT p.* FROM hs_office.relation AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
            """, nativeQuery = true)
    @Timed("app.repo.relations.findRelationRelatedToPersonUuid.real")
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    /**
     * Finds relations by a conjunction of optional criteria, including anchorPerson, holderPerson and contact data.
     *      *
     * @param personUuid the optional UUID of the anchorPerson or holderPerson
     * @param relationType the type of the relation
     * @param mark the mark (use '%' for wildcard), case ignored
     * @param personData a string to match the persons tradeName, familyName or givenName (use '%' for wildcard), case ignored
     * @param contactData a string to match the contacts caption, postalAddress, emailAddresses or phoneNumbers (use '%' for wildcard), case ignored
     * @return a list of (accessible) relations which match all given criteria
     */
    default List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
            final UUID personUuid,
            final HsOfficeRelationType relationType,
            final String mark,
            final String personData,
            final String contactData) {
        return findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl(
                personUuid, toStringOrNull(relationType),
                toSqlLikeOperand(mark), toSqlLikeOperand(personData), toSqlLikeOperand(contactData));
    }

    // TODO: Or use jsonb_path with RegEx like emailAddressRegEx in ContactRepo?
    @Query(value = """
            SELECT rel FROM HsOfficeRelationRealEntity AS rel
                WHERE (:relationType IS NULL OR CAST(rel.type AS String) = :relationType)
                    AND ( :personUuid IS NULL
                            OR rel.anchor.uuid = :personUuid OR rel.holder.uuid = :personUuid )
                    AND ( :mark IS NULL OR rel.mark ILIKE :mark )
                    AND ( :personData IS NULL
                            OR rel.anchor.tradeName ILIKE :personData OR rel.holder.tradeName ILIKE :personData
                            OR rel.anchor.familyName ILIKE :personData OR rel.holder.familyName ILIKE :personData
                            OR rel.anchor.givenName ILIKE :personData OR rel.holder.givenName ILIKE :personData )
                    AND ( :contactData IS NULL
                            OR rel.contact.caption ILIKE :contactData
                            OR CAST(rel.contact.postalAddress AS String) ILIKE :contactData
                            OR CAST(rel.contact.emailAddresses AS String) ILIKE :contactData
                            OR CAST(rel.contact.phoneNumbers AS String) ILIKE :contactData )
            """)
    @Timed("app.office.relations.repo.findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl.real")
    List<HsOfficeRelationRealEntity> findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl(
            final UUID personUuid,
            final String relationType,
            final String mark,
            final String personData,
            final String contactData);

    @Timed("app.repo.relations.save.real")
    HsOfficeRelationRealEntity save(final HsOfficeRelationRealEntity entity);

    @Timed("app.repo.relations.count.real")
    long count();

    @Timed("app.repo.relations.deleteByUuid.real")
    int deleteByUuid(UUID uuid);
    private static String toSqlLikeOperand(final String text) {
        return text == null ? null : ("%" + text.toLowerCase() + "%");
    }

    private static String toStringOrNull(final HsOfficeRelationType relationType) {
        return relationType == null ? null : relationType.name();
    }
}
