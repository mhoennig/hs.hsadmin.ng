package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.CustomerContact;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the CustomerContact entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long>, JpaSpecificationExecutor<CustomerContact> {

}
