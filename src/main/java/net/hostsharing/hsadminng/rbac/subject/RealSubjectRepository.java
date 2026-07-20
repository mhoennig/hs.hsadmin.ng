package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RealSubjectRepository extends Repository<RealSubjectEntity, UUID> {

    /**
     * The complete organization-(realm-)based subject-visibility policy, shared by the queries below:
     * <ul>
     *     <li>deactivated (soft-deleted) subjects are visible to nobody, not even global admins,</li>
     *     <li>a global admin (directly or via an assumed global admin role) sees all other subjects,</li>
     *     <li>assuming any other role drops all subject-derived visibility,</li>
     *     <li>otherwise all subjects of the current subject's own organization are visible,
     *         plus the groups of organizations in which the same natural person holds another user account.</li>
     * </ul>
     * JWT groups always belong to the current subject's own organization, thus they are visible via the
     * organization anyway and need no visibility source of their own.
     */
    // the assumed-role gate uses `cardinality(...) = 0` like rbac.subject_rv does,
    // because base.hasAssumedRole() yields null instead of false for an empty array
    String VISIBLE_SUBJECT_CONDITION = """
            (
                s.deactivated_at is null
                and (
                    rbac.hasGlobalAdminRole()
                    or (cardinality(base.assumedRoles()) = 0
                        and (
                            s.organization = (select currentSubject.organization
                                                from rbac.subject currentSubject
                                               where currentSubject.uuid = rbac.currentSubjectUuid())
                            or (s.type = 'GROUP'
                                and exists (
                                    select 1
                                      from hs_accounts.account ownAccount
                                      join hs_accounts.account samePersonAccount
                                        on samePersonAccount.person_uuid = ownAccount.person_uuid
                                      join rbac.subject samePersonSubject
                                        on samePersonSubject.uuid = samePersonAccount.uuid
                                     where ownAccount.uuid = rbac.currentSubjectUuid()
                                       and s.organization = samePersonSubject.organization
                                ))
                        ))
                )
            )
            """;

    @Query(value = """
             select *
               from rbac.subject s
              where (:userName is null or s.name like concat(cast(:userName as text), '%'))
                and (:organization is null or s.organization = cast(:organization as text))
                and (:type is null or s.type = cast(:type as rbac.SubjectType))
                and """ + VISIBLE_SUBJECT_CONDITION + """
              order by s.name
            """, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findVisibleSubjectsByOptionalNameLike.real")
    List<RealSubjectEntity> findVisibleSubjectsByOptionalNameLikeOrganizationAndTypeName(
            String userName, String organization, String type);

    default List<RealSubjectEntity> findVisibleSubjectsByOptionalNameLikeOrganizationAndType(
            final String userName,
            final String organization,
            final SubjectType type) {
        return findVisibleSubjectsByOptionalNameLikeOrganizationAndTypeName(
                userName, organization, type != null ? type.name() : null);
    }

    @Query(value = """
             select *
               from rbac.subject s
              where s.uuid = :subjectUuid
                and """ + VISIBLE_SUBJECT_CONDITION, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findVisibleSubjectByUuid.real")
    Optional<RealSubjectEntity> findVisibleSubjectByUuid(UUID subjectUuid);

    // deliberately WITHOUT the visibility condition: a global-admin must be able to physically
    // delete even a deactivated (soft-deleted) subject; only used by the admin-gated DELETE endpoint
    @Query(value = "select * from rbac.subject s where s.uuid = :subjectUuid", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findSubjectByUuidIncludingDeactivated.real")
    Optional<RealSubjectEntity> findSubjectByUuidIncludingDeactivated(UUID subjectUuid);

    @Query(value = """
             select *
               from rbac.subject s
              where s.type = 'GROUP'
                and s.uuid = any(rbac.currentSubjectOrAssumedRolesUuids())
              order by s.name
            """, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findEffectiveSubjectGroups.real")
    List<RealSubjectEntity> findEffectiveSubjectGroups();

    @Query(value = "select * from rbac.subject where uuid = rbac.currentSubjectUuid()", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findCurrentSubject.real")
    RealSubjectEntity findCurrentSubject();
}
