package net.hostsharing.hsadminng.hs.office.partner;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePartnerRepository extends Repository<HsOfficePartnerEntity, UUID> {

    @Timed("app.office.partners.repo.findByUuid")
    Optional<HsOfficePartnerEntity> findByUuid(UUID id);

    @Timed("app.office.partners.repo.findAll")
    List<HsOfficePartnerEntity> findAll(); // TODO.refa: move to a repo in test sources

    @Query("""
            SELECT partner FROM HsOfficePartnerEntity partner
                JOIN HsOfficeRelationRealEntity rel ON rel.uuid = partner.partnerRel.uuid
                JOIN HsOfficeContactRealEntity contact ON contact.uuid = rel.contact.uuid
                JOIN HsOfficePersonEntity person ON person.uuid = rel.holder.uuid
                WHERE :name is null
                    OR partner.details.birthName like concat(cast(:name as text), '%')
                    OR contact.caption like concat(cast(:name as text), '%')
                    OR person.tradeName like concat(cast(:name as text), '%')
                    OR person.givenName like concat(cast(:name as text), '%')
                    OR person.familyName like concat(cast(:name as text), '%')
               """)
    @Timed("app.office.partners.repo.findPartnerByOptionalNameLike")
    List<HsOfficePartnerEntity> findPartnerByOptionalNameLike(String name);

    @Timed("app.office.partners.repo.findPartnerByPartnerNumber")
    HsOfficePartnerEntity findPartnerByPartnerNumber(Integer partnerNumber);

    @Timed("app.office.partners.repo.save")
    HsOfficePartnerEntity save(final HsOfficePartnerEntity entity);

    @Timed("app.office.partners.repo.count")
    long count();

    @Timed("app.office.partners.repo.deleteByUuid")
    int deleteByUuid(UUID uuid);
}
