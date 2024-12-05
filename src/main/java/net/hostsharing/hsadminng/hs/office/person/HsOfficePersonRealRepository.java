package net.hostsharing.hsadminng.hs.office.person;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePersonRealRepository extends Repository<HsOfficePersonRealEntity, UUID> {

    @Timed("app.office.persons.repo.findByUuid.real")
    Optional<HsOfficePersonRealEntity> findByUuid(UUID personUuid);

    @Query("""
            SELECT p FROM HsOfficePersonRealEntity p
                WHERE :name is null
                    OR p.tradeName like concat(cast(:name as text), '%')
                    OR p.givenName like concat(cast(:name as text), '%')
                    OR p.familyName like concat(cast(:name as text), '%')
            """)
    @Timed("app.office.persons.repo.findPersonByOptionalNameLike.real")
    List<HsOfficePersonRealEntity> findPersonByOptionalNameLike(String name);

    @Timed("app.office.persons.repo.save.real")
    HsOfficePersonRealEntity save(final HsOfficePersonRealEntity entity);

    @Timed("app.office.persons.repo.count.real")
    long count();
}
