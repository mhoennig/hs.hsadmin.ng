package net.hostsharing.hsadminng.rbac.test.pac;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface TestPackageRepository extends Repository<TestPackageEntity, UUID> {

    @Query("SELECT p FROM TestPackageEntity p WHERE :name is null or p.name like concat(cast(:name as text), '%')")
    List<TestPackageEntity> findAllByOptionalNameLike(final String name);

    TestPackageEntity findByUuid(UUID packageUuid);

    TestPackageEntity save(TestPackageEntity current);
}
