package net.hostsharing.hsadminng.hs.office.relation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeRelationRbacRepository extends Repository<HsOfficeRelationRbacEntity, UUID> {

    Optional<HsOfficeRelationRbacEntity> findByUuid(UUID id);

    @Query(value = """
            SELECT p.* FROM hs_office.relation_rv AS p
                WHERE p.anchorUuid = :personUuid OR p.holderUuid = :personUuid
            """, nativeQuery = true)
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuid(@NotNull UUID personUuid);

    /**
     * Finds relations by a conjunction of optional criteria, including anchorPerson, holderPerson and contact data.
     *      *
     * @param personUuid the optional UUID of the anchorPerson or holderPerson
     * @param relationType the type of the relation
     * @param personData a string to match the persons tradeName, familyName or givenName (use '%' for wildcard), case ignored
     * @param contactData a string to match the contacts caption, postalAddress, emailAddresses or phoneNumbers (use '%' for wildcard), case ignored
     * @return a list of (accessible) relations which match all given criteria
     */
    default List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidRelationTypePersonAndContactData(
            final UUID personUuid,
            final HsOfficeRelationType relationType,
            final String personData,
            final String contactData) {
        return findRelationRelatedToPersonUuidRelationTypePersonAndContactDataImpl(
                personUuid, toStringOrNull(relationType), toSqlLikeOperand(personData), toSqlLikeOperand(contactData));
    }

    @Query(value = """
            SELECT rel FROM HsOfficeRelationRbacEntity AS rel
                WHERE (:relationType IS NULL OR CAST(rel.type AS String) = :relationType)
                    AND ( :personUuid IS NULL
                            OR rel.anchor.uuid = :personUuid OR rel.holder.uuid = :personUuid )
                    AND ( :personData IS NULL
                            OR lower(rel.anchor.tradeName) LIKE :personData OR lower(rel.holder.tradeName) LIKE :personData
                            OR lower(rel.anchor.familyName) LIKE :personData OR lower(rel.holder.familyName) LIKE :personData
                            OR lower(rel.anchor.givenName) LIKE :personData OR lower(rel.holder.givenName) LIKE :personData )
                    AND ( :contactData IS NULL
                            OR lower(rel.contact.caption) LIKE :contactData
                            OR lower(rel.contact.postalAddress) LIKE :contactData
                            OR lower(CAST(rel.contact.emailAddresses AS String)) LIKE :contactData
                            OR lower(CAST(rel.contact.phoneNumbers AS String)) LIKE :contactData )
            """)
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidRelationTypePersonAndContactDataImpl(
            final UUID personUuid,
            final String relationType,
            final String personData,
            final String contactData);

    HsOfficeRelationRbacEntity save(final HsOfficeRelationRbacEntity entity);

    long count();

    int deleteByUuid(UUID uuid);

    private static String toSqlLikeOperand(final String text) {
        return text == null ? null : ("%" + text.toLowerCase() + "%");
    }

    private static String toStringOrNull(final HsOfficeRelationType relationType) {
        return relationType == null ? null : relationType.name();
    }
}
