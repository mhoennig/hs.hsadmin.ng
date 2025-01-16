package net.hostsharing.hsadminng.hs.office.partner;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficePartnerRealRepository extends Repository<HsOfficePartnerRealEntity, UUID> {

    @Timed("app.office.partners.repo.findByUuid.real")
    Optional<HsOfficePartnerRealEntity> findByUuid(UUID id);

    @Timed("app.office.partners.repo.findAll.real")
    List<HsOfficePartnerRbacEntity> findAll(); // TODO.refa: move to a repo in test sources

    @Query(value = """
            select partner.uuid, partner.detailsuuid, partner.partnernumber, partner.partnerreluuid, partner.version
              from hs_office.partner partner
                       join hs_office.relation partnerRel on partnerRel.uuid = partner.partnerreluuid
                       join hs_office.contact contact on contact.uuid = partnerRel.contactuuid
                       join hs_office.person partnerPerson on partnerPerson.uuid = partnerRel.holderuuid
                       left join hs_office.partner_details_rv partnerDetails on partnerDetails.uuid = partner.detailsuuid
              where :name is null
                  or (partnerDetails.uuid is not null and partnerDetails.birthname like (cast(:name as text) || '%') escape '')
                  or contact.caption like (cast(:name as text) || '%') escape ''
                  or partnerPerson.tradename like (cast(:name as text) || '%') escape ''
                  or partnerPerson.givenname like (cast(:name as text) || '%') escape ''
                  or partnerPerson.familyname like (cast(:name as text) || '%') escape ''
            """, nativeQuery = true)
    @Timed("app.office.partners.repo.findPartnerByOptionalNameLike.real")
    List<HsOfficePartnerRealEntity> findPartnerByOptionalNameLike(String name);

    @Timed("app.office.partners.repo.findPartnerByPartnerNumber.real")
    Optional<HsOfficePartnerRealEntity> findPartnerByPartnerNumber(Integer partnerNumber);

    @Timed("app.office.partners.repo.save.real")
    HsOfficePartnerRealEntity save(final HsOfficePartnerRealEntity entity);
}
