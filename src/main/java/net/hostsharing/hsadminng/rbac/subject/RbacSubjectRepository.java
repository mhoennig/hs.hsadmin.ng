package net.hostsharing.hsadminng.rbac.subject;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RbacSubjectRepository extends Repository<RbacSubjectEntity, UUID> {

    @Query("""
             select u from RbacSubjectEntity u
                 where :userName is null or u.name like concat(cast(:userName as text), '%')
                 order by u.name
            """)
    List<RbacSubjectEntity> findByOptionalNameLike(String userName);

    // bypasses the restricted view, to be able to grant rights to arbitrary user
    @Query(value = "select * from rbac.subject where name=:userName", nativeQuery = true)
    RbacSubjectEntity findByName(String userName);

    RbacSubjectEntity findByUuid(UUID uuid);

    @Query(value = "select * from rbac.grantedPermissions(:subjectUuid)", nativeQuery = true)
    List<RbacSubjectPermission> findPermissionsOfUserByUuid(UUID subjectUuid);

    /*
        Can't use save/saveAndFlush from SpringData because the uuid is not generated on the entity level,
        but explicitly, and then SpringData check's if it exists using an SQL SELECT.
        And SQL SELECT needs a currentSubject which we don't yet have in the case of self registration.
     */
    @Modifying
    @Query(value = "insert into rbac.subject_rv (uuid, name) values( :#{#newUser.uuid}, :#{#newUser.name})", nativeQuery = true)
    void insert(final RbacSubjectEntity newUser);

    default RbacSubjectEntity create(final RbacSubjectEntity rbacSubjectEntity) {
        if (rbacSubjectEntity.getUuid() == null) {
            rbacSubjectEntity.setUuid(UUID.randomUUID());
        }
        insert(rbacSubjectEntity);
        return rbacSubjectEntity;
    }

    void deleteByUuid(UUID subjectUuid);
}
