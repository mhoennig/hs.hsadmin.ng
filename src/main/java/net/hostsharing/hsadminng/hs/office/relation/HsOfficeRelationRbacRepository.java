package net.hostsharing.hsadminng.hs.office.relation;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

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
    default List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
            final UUID personUuid,
            final HsOfficeRelationType relationType,
            final String mark,
            final String personData,
            final String contactData) {
        return findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl(
                personUuid, toStringOrNull(relationType),
                toSqlLikeOperand(mark), toSqlLikeOperand(personData), toSqlLikeOperand(contactData));
    }

    // TODO: use ELIKE instead of lower(...) LIKE ...? Or use jsonb_path with RegEx like emailAddressRegEx in ContactRepo?
    @Query(value = """
            SELECT rel FROM HsOfficeRelationRbacEntity AS rel
                WHERE (:relationType IS NULL OR CAST(rel.type AS String) = :relationType)
                    AND ( :personUuid IS NULL
                            OR rel.anchor.uuid = :personUuid OR rel.holder.uuid = :personUuid )
                    AND ( :mark IS NULL OR lower(rel.mark) LIKE :mark )
                    AND ( :personData IS NULL
                            OR lower(rel.anchor.tradeName) LIKE :personData OR lower(rel.holder.tradeName) LIKE :personData
                            OR lower(rel.anchor.familyName) LIKE :personData OR lower(rel.holder.familyName) LIKE :personData
                            OR lower(rel.anchor.givenName) LIKE :personData OR lower(rel.holder.givenName) LIKE :personData )
                    AND ( :contactData IS NULL
                            OR lower(rel.contact.caption) LIKE :contactData
                            OR lower(CAST(rel.contact.postalAddress AS String)) LIKE :contactData
                            OR lower(CAST(rel.contact.emailAddresses AS String)) LIKE :contactData
                            OR lower(CAST(rel.contact.phoneNumbers AS String)) LIKE :contactData )
            """)
    @Timed("app.office.relations.repo.findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl.rbac")
    List<HsOfficeRelationRbacEntity> findRelationRelatedToPersonUuidRelationByTypeMarkPersonAndContactDataImpl(
            final UUID personUuid,
            final String relationType,
            final String mark,
            final String personData,
            final String contactData);

    @Timed("app.office.relations.repo.save.rbac")
    HsOfficeRelationRbacEntity save(final HsOfficeRelationRbacEntity entity);

    @Timed("app.office.relations.repo.count.rbac")
    long count();

    @Timed("app.office.relations.repo.deleteByUuid.rbac")
    int deleteByUuid(UUID uuid);

    private static String toSqlLikeOperand(final String text) {
        return text == null ? null : ("%" + text.toLowerCase() + "%");
    }

    private static String toStringOrNull(final HsOfficeRelationType relationType) {
        return relationType == null ? null : relationType.name();
    }
}
