package net.hostsharing.hsadminng.rbac.grant;

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
    RbacGrantEntity findById(RbacGrantId rbacGrantId);

    long count();

    List<RbacGrantEntity> findAll();

    RbacGrantEntity save(final RbacGrantEntity grant);

    @Modifying
    @Query(value = """
             delete from RbacGrantEntity as g
                 where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
                   and g.granteeSubjectUuid=:#{#rbacGrantId.granteeSubjectUuid}
            """)
    void deleteByRbacGrantId(RbacGrantId rbacGrantId);
}
