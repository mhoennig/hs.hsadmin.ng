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

    // resolves client-supplied UUIDs in write-paths, where an invisible contact must not become
    // referencable; belongs here rather than into the real repository, because it reads the
    // restricted view, and returns the real entity, because that is the type a reference holds
    @Query(value = "select c.* from hs_office.contact_rv c where c.uuid = :uuid", nativeQuery = true)
    @Timed("app.office.contacts.repo.findVisibleByUuid.rbac")
    Optional<HsOfficeContactRealEntity> findVisibleByUuid(UUID uuid);

    @Query("""
            SELECT c FROM HsOfficeContactRbacEntity c
                WHERE :caption is null
                    OR c.caption like concat(cast(:caption as text), '%')
            """)
    @Timed("app.office.contacts.repo.findContactByOptionalCaptionLike.rbac")
    List<HsOfficeContactRbacEntity> findContactByOptionalCaptionLike(String caption);

    @Query(value = """
            select c.* from hs_office.contact_rv c
                where exists (
                    SELECT 1 FROM jsonb_each_text(c.emailAddresses) AS kv(key, value)
                        WHERE kv.value LIKE :emailAddressRegEx
                )
            """, nativeQuery = true)
    @Timed("app.office.contacts.repo.findContactByEmailAddress.rbac")
    List<HsOfficeContactRbacEntity> findContactByEmailAddress(final String emailAddressRegEx);

    @Timed("app.office.contacts.repo.save.rbac")
    HsOfficeContactRbacEntity save(final HsOfficeContactRbacEntity entity);

    @Timed("app.office.contacts.repo.deleteByUuid.rbac")
    int deleteByUuid(final UUID uuid);

    @Timed("app.office.contacts.repo.count.rbac")
    long count();
}
