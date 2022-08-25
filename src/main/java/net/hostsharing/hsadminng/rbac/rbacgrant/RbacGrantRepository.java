package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface RbacGrantRepository extends Repository<RbacGrantEntity, RbacGrantId> {

    @Query(value = """
             select g from RbacGrantEntity as g
                 where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
                   and g.granteeUserUuid=:#{#rbacGrantId.granteeUserUuid}
            """)
    RbacGrantEntity findById(RbacGrantId rbacGrantId);

    List<RbacGrantEntity> findAll();

    RbacGrantEntity save(final RbacGrantEntity grant);

    @Modifying
    @Query(value = """
             delete from RbacGrantEntity as g
                 where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
                   and g.granteeUserUuid=:#{#rbacGrantId.granteeUserUuid}
            """)
    void deleteByRbacGrantId(RbacGrantId rbacGrantId);
}
