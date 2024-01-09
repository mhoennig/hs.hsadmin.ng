package net.hostsharing.hsadminng.hs.office.debitor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeDebitorRepository extends Repository<HsOfficeDebitorEntity, UUID> {

    Optional<HsOfficeDebitorEntity> findByUuid(UUID id);

    @Query("""
            SELECT debitor FROM HsOfficeDebitorEntity debitor
                WHERE debitor.debitorNumber = :debitorNumber
               """)
    List<HsOfficeDebitorEntity> findDebitorByDebitorNumber(int debitorNumber);

    @Query("""
            SELECT debitor FROM HsOfficeDebitorEntity debitor
                JOIN HsOfficePartnerEntity partner ON partner.uuid = debitor.partner.uuid
                JOIN HsOfficePersonEntity person ON person.uuid = partner.person.uuid
                JOIN HsOfficeContactEntity contact ON contact.uuid = debitor.billingContact.uuid
                WHERE :name is null
                    OR partner.details.birthName like concat(cast(:name as text), '%')
                    OR person.tradeName like concat(cast(:name as text), '%')
                    OR person.familyName like concat(cast(:name as text), '%')
                    OR person.givenName like concat(cast(:name as text), '%')
                    OR contact.label like concat(cast(:name as text), '%')
               """)
    List<HsOfficeDebitorEntity> findDebitorByOptionalNameLike(String name);

    HsOfficeDebitorEntity save(final HsOfficeDebitorEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
