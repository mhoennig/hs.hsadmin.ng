package net.hostsharing.hsadminng.hs.office.person;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePersonRepository extends Repository<HsOfficePersonEntity, UUID> {

    Optional<HsOfficePersonEntity> findByUuid(UUID personUuid);

    @Query("""
            SELECT p FROM HsOfficePersonEntity p
                WHERE :name is null
                    OR p.tradeName like concat(:name, '%')
                    OR p.givenName like concat(:name, '%')
                    OR p.familyName like concat(:name, '%')
               """)
    List<HsOfficePersonEntity> findPersonByOptionalNameLike(String name);

    HsOfficePersonEntity save(final HsOfficePersonEntity entity);

    int deleteByUuid(final UUID personUuid);

    long count();
}
