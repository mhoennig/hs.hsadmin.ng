package net.hostsharing.hsadminng.hs.hscustomer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    List<CustomerEntity> findByPrefixLike(final String prefix);
}
