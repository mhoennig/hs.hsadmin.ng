package net.hostsharing.hsadminng.test.cust;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestCustomerRepository extends Repository<TestCustomerEntity, UUID> {


    Optional<TestCustomerEntity> findByUuid(UUID id);

    @Query("SELECT c FROM TestCustomerEntity c WHERE :prefix is null or c.prefix like concat(cast(:prefix as text), '%')")
    List<TestCustomerEntity> findCustomerByOptionalPrefixLike(String prefix);

    TestCustomerEntity save(final TestCustomerEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
