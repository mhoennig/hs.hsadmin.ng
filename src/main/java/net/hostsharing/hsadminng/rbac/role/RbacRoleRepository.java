package net.hostsharing.hsadminng.rbac.role;

import io.micrometer.core.annotation.Timed;
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

    @Timed("app.rbac.roles.repo.findByRoleName")
    RbacRoleEntity findByRoleName(String roleName);
}
