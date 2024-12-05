package net.hostsharing.hsadminng.rbac.grant;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacGrantRepository extends Repository<RawRbacGrantEntity, UUID> {

    @Timed("app.rbac.grants.repo.findAll")
    List<RawRbacGrantEntity> findAll(); // TODO.refa: move to test repo

    @Timed("app.rbac.grants.repo.findByAscendingUuid")
    List<RawRbacGrantEntity> findByAscendingUuid(UUID ascendingUuid);

    @Timed("app.rbac.grants.repo.findByDescendantUuid")
    List<RawRbacGrantEntity> findByDescendantUuid(UUID refUuid);
}
