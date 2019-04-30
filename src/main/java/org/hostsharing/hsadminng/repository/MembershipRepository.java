// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.Membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Membership entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long>, JpaSpecificationExecutor<Membership> {

    @Query("SELECT CASE WHEN COUNT(m)> 0 THEN TRUE ELSE FALSE END " +
            " FROM Membership m WHERE m.customer.id=:customerId AND m.memberUntilDate IS NULL")
    boolean hasUncancelledMembershipForCustomer(@Param("customerId") final long customerId);
}
