package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacGrantRepository extends Repository<RawRbacGrantEntity, UUID> {

    List<RawRbacGrantEntity> findAll();

    List<RawRbacGrantEntity> findByAscendingUuid(UUID ascendingUuid);

    List<RawRbacGrantEntity> findByDescendantUuid(UUID refUuid);
}
