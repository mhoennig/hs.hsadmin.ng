package net.hostsharing.hsadminng.hs.hscustomer;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends Repository<CustomerEntity, UUID> {


    Optional<CustomerEntity> findByUuid(UUID id);

    @Query("SELECT c FROM CustomerEntity c WHERE :prefix is null or c.prefix like concat(:prefix, '%')")
    List<CustomerEntity> findCustomerByOptionalPrefixLike(@Param("prefix") String prefix);

    CustomerEntity save(final CustomerEntity entity);

}
