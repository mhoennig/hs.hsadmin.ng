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
                JOIN HsOfficePartnerEntity partner ON partner.uuid = debitor.partner
                JOIN HsOfficePersonEntity person ON person.uuid = partner.person
                JOIN HsOfficeContactEntity contact ON contact.uuid = debitor.billingContact
                WHERE :name is null
                    OR partner.details.birthName like concat(:name, '%')
                    OR person.tradeName like concat(:name, '%')
                    OR person.familyName like concat(:name, '%')
                    OR person.givenName like concat(:name, '%')
                    OR contact.label like concat(:name, '%')
               """)
    List<HsOfficeDebitorEntity> findDebitorByOptionalNameLike(String name);

    HsOfficeDebitorEntity save(final HsOfficeDebitorEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
