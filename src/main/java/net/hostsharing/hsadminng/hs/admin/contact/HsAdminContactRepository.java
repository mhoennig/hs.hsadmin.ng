package net.hostsharing.hsadminng.hs.admin.contact;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAdminContactRepository extends Repository<HsAdminContactEntity, UUID> {

    Optional<HsAdminContactEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsAdminContactEntity c
                WHERE :label is null
                    OR c.label like concat(:label, '%')
               """)
        // TODO: join tables missing
    List<HsAdminContactEntity> findContactByOptionalLabelLike(String label);

    HsAdminContactEntity save(final HsAdminContactEntity entity);

    void deleteByUuid(final UUID uuid);

    long count();
}
