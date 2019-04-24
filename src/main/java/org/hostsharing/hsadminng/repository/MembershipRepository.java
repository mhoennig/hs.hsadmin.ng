package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.Membership;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Membership entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long>, JpaSpecificationExecutor<Membership> {

}
