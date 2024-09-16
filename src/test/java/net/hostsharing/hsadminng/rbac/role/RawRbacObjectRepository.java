package net.hostsharing.hsadminng.rbac.role;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacObjectRepository extends Repository<RawRbacObjectEntity, UUID> {

    List<RawRbacObjectEntity> findAll();
}
