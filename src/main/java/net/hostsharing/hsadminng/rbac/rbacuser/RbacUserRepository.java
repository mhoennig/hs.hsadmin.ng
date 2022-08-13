package net.hostsharing.hsadminng.rbac.rbacuser;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RbacUserRepository extends Repository<RbacUserEntity, UUID> {

    @Query("""
         select u from RbacUserEntity u
             where :userName is null or u.name like concat(:userName, '%')
             order by u.name
        """)
    List<RbacUserEntity> findByOptionalNameLike(String userName);

    @Query(value = "select uuid from rbacuser where name=:userName", nativeQuery = true)
    UUID findUuidByName(String userName);

    RbacUserEntity findByUuid(UUID uuid);

    @Query(value = "select * from grantedPermissions(:userName)", nativeQuery = true)
    List<RbacUserPermission> findPermissionsOfUser(String userName);

    /*
        Can't use save/saveAndFlush from SpringData because the uuid is not generated on the entity level,
        but explicitly, and then SpringData check's if it exists using an SQL SELECT.
        And SQL SELECT needs a currentUser which we don't yet have in the case of self registration.
     */
    @Modifying
    @Query(value = "insert into RBacUser_RV (uuid, name) values( :#{#newUser.uuid}, :#{#newUser.name})", nativeQuery = true)
    void insert(@Param("newUser") final RbacUserEntity newUser);

    default RbacUserEntity create(final RbacUserEntity rbacUserEntity) {
        if (rbacUserEntity.getUuid() == null) {
            rbacUserEntity.setUuid(UUID.randomUUID());
        }
        insert(rbacUserEntity);
        return rbacUserEntity;
    }
}
