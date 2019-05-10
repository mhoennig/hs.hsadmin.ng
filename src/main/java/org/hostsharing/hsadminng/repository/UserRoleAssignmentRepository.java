// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for the UserRoleAssignment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserRoleAssignmentRepository
        extends JpaRepository<UserRoleAssignment, Long>, JpaSpecificationExecutor<UserRoleAssignment> {

    String CURRENT_USER_ROLE_ASSIGNMENTS_CACHE = "currentUserRoleAssignments";

    // TODO mhoennig: optimize with query by id
    @Cacheable(CURRENT_USER_ROLE_ASSIGNMENTS_CACHE)
    @Query("select user_role_assignment from UserRoleAssignment user_role_assignment where user_role_assignment.user.login = :login")
    List<UserRoleAssignment> findByLogin(@Param("login") final String login);

    @CacheEvict(value = CURRENT_USER_ROLE_ASSIGNMENTS_CACHE, allEntries = true)
    default void evictCache() {
    }
}
