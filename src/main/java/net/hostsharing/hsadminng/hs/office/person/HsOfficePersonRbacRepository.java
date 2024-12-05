package net.hostsharing.hsadminng.hs.office.person;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePersonRbacRepository extends Repository<HsOfficePersonRbacEntity, UUID> {

    @Timed("app.office.persons.repo.findByUuid.rbac")
    Optional<HsOfficePersonRbacEntity> findByUuid(UUID personUuid);

    @Query("""
            SELECT p FROM HsOfficePersonRbacEntity p
                WHERE :name is null
                    OR p.tradeName like concat(cast(:name as text), '%')
                    OR p.givenName like concat(cast(:name as text), '%')
                    OR p.familyName like concat(cast(:name as text), '%')
            """)
    @Timed("app.office.persons.repo.findPersonByOptionalNameLike.rbac")
    List<HsOfficePersonRbacEntity> findPersonByOptionalNameLike(String name);

    @Timed("app.office.persons.repo.save.rbac")
    HsOfficePersonRbacEntity save(final HsOfficePersonRbacEntity entity);

    @Timed("app.office.persons.repo.deleteByUuid.rbac")
    int deleteByUuid(final UUID personUuid);

    @Timed("app.office.persons.repo.count.rbac")
    long count();
}
