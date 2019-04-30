// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.Share;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Share entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ShareRepository extends JpaRepository<Share, Long>, JpaSpecificationExecutor<Share> {

}
