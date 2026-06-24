package net.hostsharing.hsadminng.rbac.role;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RbacRoleRepository extends Repository<RbacRoleEntity, UUID> {

    /**
     * @return the number of persistent RbacRoleEntity instances, mostly for testing purposes.
     */
    @Timed("app.rbac.roles.repo.findByUuid")
    long count(); // TODO.refa: move to test sources

    /**
     * @return all persistent RbacRoleEntity instances, assigned to the current subject (user or assumed roles)
     */
    @Timed("app.rbac.roles.repo.findAll")
    List<RbacRoleEntity> findAll(); // TODO.refa: move to test sources

    @Query(value = """
        SELECT rev.*,
               rev.objectTable||'#'||cast(rev.objectUuid as varchar)||':'||rev.roleType AS roleName,
               rev.objectTable||'#'||rev.objectIdName||':'||rev.roleType AS roleIdName
        FROM rbac.role_rv rev
        WHERE rev.objectTable||'#'||rev.objectIdName||':'||rev.roleType = :roleIdName
        """, nativeQuery = true)
    @Timed("app.rbac.roles.repo.findByRoleIdName")
    List<RbacRoleEntity> findByRoleIdName(String roleIdName);

    @Query(value = """
        SELECT rev.*,
               rev.objectTable||'#'||cast(rev.objectUuid as varchar)||':'||rev.roleType AS roleName,
               rev.objectTable||'#'||rev.objectIdName||':'||rev.roleType AS roleIdName
        FROM rbac.role_rv rev
        WHERE rev.objectTable||'#'||cast(rev.objectUuid as varchar)||':'||rev.roleType = :roleName
        """, nativeQuery = true)
    @Timed("app.rbac.roles.repo.findByRoleName")
    RbacRoleEntity findByRoleName(String roleName);

    @Timed("app.rbac.roles.repo.findByObjectUuidAndRoleType")
    @Query(value = """
        SELECT rev.*,
               rev.objectTable||'#'||cast(rev.objectUuid as varchar)||':'||rev.roleType AS roleName,
               rev.objectTable||'#'||rev.objectIdName||':'||rev.roleType AS roleIdName
        FROM rbac.role_rv rev
        WHERE rev.objectuuid = :objectUuid
          AND rev.roletype = cast(:roleType as rbac.roletype)
        """, nativeQuery = true)
    RbacRoleEntity findByObjectUuidAndRoleType(UUID objectUuid, String roleType);

    default RbacRoleEntity findByObjectUuidAndRoleType(UUID objectUuid, RbacRoleType roleType) {
        return findByObjectUuidAndRoleType(objectUuid, roleType.name());
    }

    @Timed("app.rbac.roles.repo.fetchAssumedRoles")
    @Query(value = """
        SELECT rev.*,
               rev.objectTable||'#'||cast(rev.objectUuid as varchar)||':'||rev.roleType AS roleName
        FROM rbac.role_ev rev
        WHERE rev.uuid = ANY(rbac.currentSubjectOrAssumedRolesUuids())
        """, nativeQuery = true)
    List<RbacRoleEntity> fetchAssumedRoles();
}
