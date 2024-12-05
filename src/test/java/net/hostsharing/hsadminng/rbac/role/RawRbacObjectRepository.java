package net.hostsharing.hsadminng.rbac.role;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RawRbacObjectRepository extends Repository<RawRbacObjectEntity, UUID> {

    @Timed("app.rbac.objects.repo.findAll.real")
    List<RawRbacObjectEntity> findAll();
}
