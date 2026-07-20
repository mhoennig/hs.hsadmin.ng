package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RbacSubjectRepository extends Repository<RbacSubjectEntity, UUID> {

    // bypasses the restricted view, to be able to grant rights to arbitrary user
    @Query(value = "select * from rbac.subject where name=:userName", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findByName.rbac")
    RbacSubjectEntity findByName(String userName);

    @Timed("app.rbac.subjects.repo.findByUuid.rbac")
    RbacSubjectEntity findByUuid(UUID uuid);

    @Query(value = "select * from rbac.grantedPermissions(:subjectUuid)", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findPermissionsOfUserByUuid.rbac")
    List<RbacSubjectPermission> findPermissionsOfUserByUuid(UUID subjectUuid);

    /*
        Can't use save/saveAndFlush from SpringData because the uuid is not generated on the entity level,
        but explicitly, and then SpringData check's if it exists using an SQL SELECT.
        And SQL SELECT needs a currentSubject which we don't yet have in the case of self-registration.
     */
    @Modifying
    @Query(value = "insert into rbac.subject_rv (uuid, name, organization, type) values( :#{#newUser.uuid}, :#{#newUser.name}, :#{#newUser.organization}, cast(:#{#newUser.type.name()} as rbac.SubjectType))", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.insert.rbac")
    void insert(final RbacSubjectEntity newUser);

    default RbacSubjectEntity create(final RbacSubjectEntity rbacSubjectEntity) {
        if (rbacSubjectEntity.getUuid() == null) {
            rbacSubjectEntity.setUuid(UUID.randomUUID());
        }
        if (rbacSubjectEntity.getType() == null) {
            rbacSubjectEntity.setType(SubjectType.USER);
        }
        insert(rbacSubjectEntity);
        // RbacSubjectEntity binds to 'rbac.subject_rv',
        // but the current user might not be allowed to read the newly created row from the restricted view,
        // only the newly created subject (or a global admin) is allowed to read the new subject.
        // Thus, the code which calls this method needs to switch the login user and fetch an attached entity.
        return rbacSubjectEntity; // Not yet attached to EM!
    }

    // idempotent upsert keyed by uuid; returns 'created' or 'updated'. The type is immutable.
    // A null organization is defaulted from the name prefix in the DB function.
    // deactivated=true deactivates the subject (soft-delete), false (re)activates it.
    @Query(value = "select rbac.upsert_subject(:uuid, :name, :organization, cast(:type as rbac.SubjectType), :deactivated)", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.upsert.rbac")
    String upsert(UUID uuid, String name, String organization, String type, boolean deactivated);

    // stores the SHA-256 hash of the API-key of an API_KEY subject, its named
    // endpoint-scopes (empty array = unrestricted API-key), and its optional expiry
    // timestamp (null = never expires);
    // the DB function re-checks global-admin permission and the subject type
    @Query(
            value = "select rbac.create_api_key(:subjectUuid, :keyHash, cast(:scopes as varchar[]), cast(:expiresAt as timestamptz))",
            nativeQuery = true)
    @Timed("app.rbac.subjects.repo.createApiKey.rbac")
    UUID createApiKey(UUID subjectUuid, String keyHash, String[] scopes, OffsetDateTime expiresAt);

    // physical delete: removing the rbac.reference row cascades to the subject and, via the
    // rbac.subject delete triggers, to all of its grants and its account; a no-op if the uuid is
    // unknown. The subject's BEFORE DELETE trigger re-checks authorization at the DB level.
    @Modifying
    @Query(value = "delete from rbac.reference where uuid = :subjectUuid", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.deleteByUuid.rbac")
    void deleteByUuid(UUID subjectUuid);
}
