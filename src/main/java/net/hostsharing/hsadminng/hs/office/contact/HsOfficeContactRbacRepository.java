package net.hostsharing.hsadminng.hs.office.contact;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeContactRbacRepository extends Repository<HsOfficeContactRbacEntity, UUID> {

    @Timed("app.office.contacts.repo.findByUuid.rbac")
    Optional<HsOfficeContactRbacEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeContactRbacEntity c
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
            """)
    @Timed("app.office.contacts.repo.findContactByOptionalCaptionLike.rbac")
    List<HsOfficeContactRbacEntity> findContactByOptionalCaptionLike(String caption);

    @Timed("app.office.contacts.repo.save.rbac")
    HsOfficeContactRbacEntity save(final HsOfficeContactRbacEntity entity);

    @Timed("app.office.contacts.repo.deleteByUuid.rbac")
    int deleteByUuid(final UUID uuid);

    @Timed("app.office.contacts.repo.count.rbac")
    long count();
}
