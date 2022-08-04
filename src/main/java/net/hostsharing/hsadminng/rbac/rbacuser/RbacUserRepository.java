package net.hostsharing.hsadminng.rbac.rbacuser;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RbacUserRepository extends Repository<RbacUserEntity, UUID> {

    @Query("SELECT u FROM RbacUserEntity u WHERE :userName is null or u.name like concat(:userName, '%')")
    List<RbacUserEntity> findByOptionalNameLike(final String userName);

    @Query(value = "SELECT * FROM grantedPermissions(:userName)", nativeQuery = true)
    Iterable<RbacUserPermission> findPermissionsOfUser(@Param("userName") String userName);
}
