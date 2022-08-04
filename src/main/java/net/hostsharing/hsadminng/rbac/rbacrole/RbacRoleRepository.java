package net.hostsharing.hsadminng.rbac.rbacrole;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RbacRoleRepository extends Repository<RbacRoleEntity, UUID> {

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    List<RbacRoleEntity> findAll();
}
