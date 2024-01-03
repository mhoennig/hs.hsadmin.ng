package net.hostsharing.hsadminng.hs.office.partner;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePartnerRepository extends Repository<HsOfficePartnerEntity, UUID> {

    Optional<HsOfficePartnerEntity> findByUuid(UUID id);

    @Query("""
            SELECT partner FROM HsOfficePartnerEntity partner
                JOIN HsOfficeContactEntity contact ON contact.uuid = partner.contact.uuid
                JOIN HsOfficePersonEntity person ON person.uuid = partner.person.uuid
                WHERE :name is null
                    OR partner.details.birthName like concat(:name, '%')
                    OR contact.label like concat(:name, '%')
                    OR person.tradeName like concat(:name, '%')
                    OR person.givenName like concat(:name, '%')
                    OR person.familyName like concat(:name, '%')
               """)
    List<HsOfficePartnerEntity> findPartnerByOptionalNameLike(String name);

    HsOfficePartnerEntity save(final HsOfficePartnerEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
