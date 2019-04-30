// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import org.hostsharing.hsadminng.domain.Authority;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Authority entity.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {
}
