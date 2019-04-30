// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.SepaMandate;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the SepaMandate entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SepaMandateRepository extends JpaRepository<SepaMandate, Long>, JpaSpecificationExecutor<SepaMandate> {

}
