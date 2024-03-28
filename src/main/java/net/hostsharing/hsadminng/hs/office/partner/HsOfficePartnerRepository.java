package net.hostsharing.hsadminng.hs.office.partner;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePartnerRepository extends Repository<HsOfficePartnerEntity, UUID> {

    Optional<HsOfficePartnerEntity> findByUuid(UUID id);

    List<HsOfficePartnerEntity> findAll(); // TODO: move to a repo in test sources

    @Query("""
            SELECT partner FROM HsOfficePartnerEntity partner
                JOIN HsOfficeRelationEntity rel ON rel.uuid = partner.partnerRel.uuid
                JOIN HsOfficeContactEntity contact ON contact.uuid = rel.contact.uuid
                JOIN HsOfficePersonEntity person ON person.uuid = rel.holder.uuid
                WHERE :name is null
                    OR partner.details.birthName like concat(cast(:name as text), '%')
                    OR contact.label like concat(cast(:name as text), '%')
                    OR person.tradeName like concat(cast(:name as text), '%')
                    OR person.givenName like concat(cast(:name as text), '%')
                    OR person.familyName like concat(cast(:name as text), '%')
               """)
    List<HsOfficePartnerEntity> findPartnerByOptionalNameLike(String name);
    HsOfficePartnerEntity findPartnerByPartnerNumber(Integer partnerNumber);

    HsOfficePartnerEntity save(final HsOfficePartnerEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
