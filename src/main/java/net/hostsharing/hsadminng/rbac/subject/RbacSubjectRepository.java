package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

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
    @Query(value = "insert into rbac.subject_rv (uuid, name, type) values( :#{#newUser.uuid}, :#{#newUser.name}, cast(:#{#newUser.type.name()} as rbac.SubjectType))", nativeQuery = true)
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
    @Query(value = "select rbac.upsert_subject(:uuid, :name, cast(:type as rbac.SubjectType))", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.upsert.rbac")
    String upsert(UUID uuid, String name, String type);

    // physical delete: removing the rbac.reference row cascades to the subject and, via the
    // rbac.subject delete triggers, to all of its grants and its account; a no-op if the uuid is
    // unknown. The subject's BEFORE DELETE trigger re-checks authorization at the DB level.
    @Modifying
    @Query(value = "delete from rbac.reference where uuid = :subjectUuid", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.deleteByUuid.rbac")
    void deleteByUuid(UUID subjectUuid);

    // soft-delete: mark the subject deactivated so it is retained but excluded from all read paths;
    // idempotent - a no-op if the uuid is unknown or the subject is already deactivated
    @Modifying
    @Query(value = "update rbac.subject set deactivated_at = now() where uuid = :subjectUuid and deactivated_at is null", nativeQuery = true)
    @Timed("app.rbac.subjects.repo.deactivateByUuid.rbac")
    void deactivateByUuid(UUID subjectUuid);
}
