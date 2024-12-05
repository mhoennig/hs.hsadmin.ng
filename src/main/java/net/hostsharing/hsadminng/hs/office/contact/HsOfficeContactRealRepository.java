package net.hostsharing.hsadminng.hs.office.contact;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeContactRealRepository extends Repository<HsOfficeContactRealEntity, UUID> {

    @Timed("app.office.contacts.repo.findByUuid.real")
    Optional<HsOfficeContactRealEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeContactRealEntity c
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
            """)
    @Timed("app.office.contacts.repo.findContactByOptionalCaptionLike.real")
    List<HsOfficeContactRealEntity> findContactByOptionalCaptionLike(String caption);

    @Timed("app.office.contacts.repo.save.real")
    HsOfficeContactRealEntity save(final HsOfficeContactRealEntity entity);

    @Timed("app.office.contacts.repo.deleteByUuid.real")
    int deleteByUuid(final UUID uuid);

    @Timed("app.office.contacts.repo.count.real")
    long count();
}
