package net.hostsharing.hsadminng.rbac.role;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RbacRoleRepository extends Repository<RbacRoleEntity, UUID> {

    /**
     * @return the number of persistent RbacRoleEntity instances, mostly for testing purposes.
     */
    long count(); // TODO: move to test sources

    /**
     * @return all persistent RbacRoleEntity instances, assigned to the current subject (user or assumed roles)
     */
    List<RbacRoleEntity> findAll();

    RbacRoleEntity findByRoleName(String roleName);
}
