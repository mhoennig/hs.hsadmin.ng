// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for the UserRoleAssignment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserRoleAssignmentRepository
        extends JpaRepository<UserRoleAssignment, Long>, JpaSpecificationExecutor<UserRoleAssignment> {

    @Query("select user_role_assignment from UserRoleAssignment user_role_assignment where user_role_assignment.user.login = ?#{principal.username}")
    List<UserRoleAssignment> findByUserIsCurrentUser();

}
