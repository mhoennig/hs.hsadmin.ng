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
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
               """)
    List<HsOfficeContactEntity> findContactByOptionalCaptionLike(String caption);

    HsOfficeContactEntity save(final HsOfficeContactEntity entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
