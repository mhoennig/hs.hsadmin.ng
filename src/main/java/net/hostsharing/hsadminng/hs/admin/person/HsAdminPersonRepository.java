package net.hostsharing.hsadminng.hs.admin.person;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAdminPersonRepository extends Repository<HsAdminPersonEntity, UUID> {

    Optional<HsAdminPersonEntity> findByUuid(UUID personUuid);

    @Query("""
            SELECT p FROM HsAdminPersonEntity p
                WHERE :name is null
                    OR p.tradeName like concat(:name, '%')
                    OR p.givenName like concat(:name, '%')
                    OR p.familyName like concat(:name, '%')
               """)
    List<HsAdminPersonEntity> findPersonByOptionalNameLike(String name);

    HsAdminPersonEntity save(final HsAdminPersonEntity entity);

    int deleteByUuid(final UUID personUuid);

    long count();
}
