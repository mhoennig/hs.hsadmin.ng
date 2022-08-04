package net.hostsharing.hsadminng.hs.hspackage;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface PackageRepository extends Repository<PackageEntity, UUID> {

    @Query("SELECT p FROM PackageEntity p WHERE :name is null or p.name like concat(:name, '%')")
    List<PackageEntity> findAllByOptionalNameLike(final String name);
}
