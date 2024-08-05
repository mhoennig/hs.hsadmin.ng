package net.hostsharing.hsadminng.hs.office.contact;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeContactRealRepository extends Repository<HsOfficeContactRealEntity, UUID> {

    Optional<HsOfficeContactRealEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeContactRealEntity c
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
               """)
    List<HsOfficeContactRealEntity> findContactByOptionalCaptionLike(String caption);

    HsOfficeContactRealEntity save(final HsOfficeContactRealEntity entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
