package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface RbacGrantRepository extends Repository<RbacGrantEntity, RbacGrantId> {

    List<RbacGrantEntity> findAll();

    void save(final RbacGrantEntity grant);

    void delete(final RbacGrantEntity grant);
}
