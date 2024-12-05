package net.hostsharing.hsadminng.rbac.role;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacRoleRepository extends Repository<RawRbacRoleEntity, UUID> {

    @Timed("app.rbac.roles.repo.findAll.real")
    List<RawRbacRoleEntity> findAll();
}
