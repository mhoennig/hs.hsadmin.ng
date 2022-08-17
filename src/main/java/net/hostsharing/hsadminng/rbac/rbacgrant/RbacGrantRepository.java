package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface RbacGrantRepository extends Repository<RbacGrantEntity, RbacGrantId> {

    List<RbacGrantEntity> findAll();

    void save(final RbacGrantEntity grant);

    @Modifying
    @Query(value = """
         delete from RbacGrantEntity as g
             where g.grantedRoleUuid=:#{#rbacGrantId.grantedRoleUuid}
               and g.granteeUserUuid=:#{#rbacGrantId.granteeUserUuid}
        """)
    void deleteByRbacGrantId(RbacGrantId rbacGrantId);
}
