package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RealSubjectRepository extends Repository<RealSubjectEntity, UUID> {

    // TODO.impl[Taiga#471]: extract organization from prefix on ingesting the
    //  - LIKE patterns built from stable functions are non-sargable (no index on s.name usable, contradicting the
    //    PR-doc performance contemplation) and may be re-evaluated per row; consider binding the realm prefix as a parameter
    // - the realm-name grammar is hand-rolled as LIKE fragments (2x same-realm, 1x same-person);
    //               a symmetric rbac.subject_realm_prefix(s.name) = rbac.subject_realm_prefix(base.currentSubject()) comparison
    //               would keep the grammar in the one SQL function (mind dash-less names before switching)
    // - the correlated EXISTS builds the LIKE pattern per row (nested loop with per-row subject_realm_prefix calls);
    //               precompute the person's realm prefixes once (e.g. in a CTE) and semi-join against that small set
    /**
     * The complete realm-based subject-visibility policy, shared by the queries below:
     * <ul>
     *     <li>a global admin (directly or via an assumed global admin role) sees all subjects,</li>
     *     <li>assuming any other role drops all subject-derived visibility,</li>
     *     <li>otherwise all subjects of the current subject's own realm (by name prefix) are visible,
     *         plus the groups of realms in which the same natural person holds another user account.</li>
     * </ul>
     * JWT groups always belong to the current subject's own realm, thus they are visible via the realm prefix anyway
     * and need no visibility source of their own.
     */
    // the assumed-role gate uses `cardinality(...) = 0` like rbac.subject_rv does,
    // because base.hasAssumedRole() yields null instead of false for an empty array
    String VISIBLE_SUBJECT_CONDITION = """
            (
                rbac.hasGlobalAdminRole()
                or (cardinality(base.assumedRoles()) = 0
                    and (
                        s.name like concat(rbac.subject_realm_prefix(base.currentSubject()), '-%')
                        or s.name like concat('/', rbac.subject_realm_prefix(base.currentSubject()), '-%')
                        or (s.type = 'GROUP'
                            and exists (
                                select 1
                                  from hs_accounts.account ownAccount
                                  join hs_accounts.account samePersonAccount
                                    on samePersonAccount.person_uuid = ownAccount.person_uuid
                                  join rbac.subject samePersonSubject
                                    on samePersonSubject.uuid = samePersonAccount.uuid
                                 where ownAccount.uuid = rbac.currentSubjectUuid()
                                   and s.name like concat('/', rbac.subject_realm_prefix(samePersonSubject.name), '-%')
                            ))
                    ))
            )
            """;

    @Query(value = """
             select *
               from rbac.subject s
              where (:userName is null or s.name like concat(cast(:userName as text), '%'))
                and (:type is null or s.type = cast(:type as rbac.SubjectType))
                and """ + VISIBLE_SUBJECT_CONDITION + """
              order by s.name
            """, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findVisibleSubjectsByOptionalNameLike.real")
    List<RealSubjectEntity> findVisibleSubjectsByOptionalNameLikeAndOptionalTypeName(String userName, String type);

    default List<RealSubjectEntity> findVisibleSubjectsByOptionalNameLikeAndOptionalType(
            final String userName,
            final SubjectType type) {
        return findVisibleSubjectsByOptionalNameLikeAndOptionalTypeName(userName, type != null ? type.name() : null);
    }

    @Query(value = """
             select *
               from rbac.subject s
              where s.uuid = :subjectUuid
                and """ + VISIBLE_SUBJECT_CONDITION, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findVisibleSubjectByUuid.real")
    Optional<RealSubjectEntity> findVisibleSubjectByUuid(UUID subjectUuid);

    @Query(value = """
             select *
               from rbac.subject s
              where s.type = 'GROUP'
                and s.uuid = any(rbac.currentSubjectOrAssumedRolesUuids())
              order by s.name
            """, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findEffectiveSubjectGroups.real")
    List<RealSubjectEntity> findEffectiveSubjectGroups();
}
