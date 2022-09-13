package net.hostsharing.hsadminng.hs.office.contact;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeContactRepository extends Repository<HsOfficeContactEntity, UUID> {

    Optional<HsOfficeContactEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeContactEntity c
                WHERE :label is null
                    OR c.label like concat(:label, '%')
               """)
        // TODO.feat: join tables missing
    List<HsOfficeContactEntity> findContactByOptionalLabelLike(String label);

    HsOfficeContactEntity save(final HsOfficeContactEntity entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
