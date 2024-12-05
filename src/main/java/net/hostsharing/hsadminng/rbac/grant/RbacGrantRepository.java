package net.hostsharing.hsadminng.rbac.grant;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface RbacGrantRepository extends Repository<RbacGrantEntity, RbacGrantId> {

    @Query(value = """
             select g from RbacGrantEntity as g
                 where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
                   and g.granteeSubjectUuid=:#{#rbacGrantId.granteeSubjectUuid}
            """)
    @Timed("app.rbac.grants.repo.findById")
    RbacGrantEntity findById(RbacGrantId rbacGrantId);

    @Timed("app.rbac.grants.repo.count")
    long count();

    @Timed("app.rbac.grants.repo.findAll")
    List<RbacGrantEntity> findAll();

    @Timed("app.rbac.grants.repo.save")
    RbacGrantEntity save(final RbacGrantEntity grant);

    @Modifying
    @Query(value = """
             delete from RbacGrantEntity as g
                 where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
                   and g.granteeSubjectUuid=:#{#rbacGrantId.granteeSubjectUuid}
            """)
    @Timed("app.rbac.grants.repo.deleteByRbacGrantId")
    void deleteByRbacGrantId(RbacGrantId rbacGrantId);
}
