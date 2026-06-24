package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RealSubjectRepository extends Repository<RealSubjectEntity, UUID> {

    @Query(value = """
             select *
               from rbac.subject s
              where (:userName is null or s.name like concat(cast(:userName as text), '%'))
                and s.type = 'GROUP'
                and s.uuid = any(rbac.determineCurrentSubjectGroupUuids(current_setting('hsadminng.currentSubjectGroups', true)))
              order by s.name
            """, nativeQuery = true)
    @Timed("app.rbac.subjects.repo.findCurrentSubjectGroupSubjectsByOptionalNameLike.real")
    List<RealSubjectEntity> findCurrentSubjectGroupSubjectsByOptionalNameLike(String userName);
}
