package net.hostsharing.hsadminng.hs.office.contact;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeContactRbacRepository extends Repository<HsOfficeContactRbacEntity, UUID> {

    Optional<HsOfficeContactRbacEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeContactRbacEntity c
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
               """)
    List<HsOfficeContactRbacEntity> findContactByOptionalCaptionLike(String caption);

    HsOfficeContactRbacEntity save(final HsOfficeContactRbacEntity entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
