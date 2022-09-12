package net.hostsharing.hsadminng.hs.admin.partner;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAdminPartnerRepository extends Repository<HsAdminPartnerEntity, UUID> {

    Optional<HsAdminPartnerEntity> findByUuid(UUID id);

    @Query("""
            SELECT partner FROM HsAdminPartnerEntity partner
                JOIN HsAdminContactEntity contact ON contact.uuid = partner.contact
                JOIN HsAdminPersonEntity person ON person.uuid = partner.person
                WHERE :name is null
                    OR partner.birthName like concat(:name, '%')
                    OR contact.label like concat(:name, '%')
                    OR person.tradeName like concat(:name, '%')
                    OR person.givenName like concat(:name, '%')
                    OR person.familyName like concat(:name, '%')
               """)
    List<HsAdminPartnerEntity> findPartnerByOptionalNameLike(String name);

    HsAdminPartnerEntity save(final HsAdminPartnerEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
