package net.hostsharing.hsadminng.rbac.rbacrole;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacRoleRepository extends Repository<RawRbacRoleEntity, UUID> {

    List<RawRbacRoleEntity> findAll();
}
