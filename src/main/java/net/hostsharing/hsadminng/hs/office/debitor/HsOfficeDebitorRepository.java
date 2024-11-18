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
            JOIN HsOfficePartnerEntity partner
                    ON partner.partnerRel.holder = debitor.debitorRel.anchor
                        AND partner.partnerRel.type = 'PARTNER' AND debitor.debitorRel.type = 'DEBITOR'
                WHERE partner.partnerNumber = :partnerNumber
                  AND debitor.debitorNumberSuffix = :debitorNumberSuffix
            """)
     List<HsOfficeDebitorEntity> findDebitorByDebitorNumber(int partnerNumber, String debitorNumberSuffix);

    default List<HsOfficeDebitorEntity> findDebitorByDebitorNumber(int debitorNumber) {
        final var partnerNumber = debitorNumber / 100;
        final String suffix = String.format("%02d", debitorNumber % 100);
        final var result = findDebitorByDebitorNumber(partnerNumber, suffix);
        return result;
    }

    @Query("""
            SELECT debitor FROM HsOfficeDebitorEntity debitor
                JOIN HsOfficePartnerEntity partner
                    ON partner.partnerRel.holder = debitor.debitorRel.anchor
                        AND partner.partnerRel.type = 'PARTNER' AND debitor.debitorRel.type = 'DEBITOR'
                JOIN HsOfficePersonEntity person
                    ON person.uuid = partner.partnerRel.holder.uuid
                        OR person.uuid = debitor.debitorRel.holder.uuid
                JOIN HsOfficeContactRealEntity contact
                    ON contact.uuid = debitor.debitorRel.contact.uuid 
                        OR contact.uuid = partner.partnerRel.contact.uuid
                WHERE :name is null
                    OR partner.details.birthName like concat(cast(:name as text), '%')
                    OR person.tradeName like concat(cast(:name as text), '%')
                    OR person.familyName like concat(cast(:name as text), '%')
                    OR person.givenName like concat(cast(:name as text), '%')
                    OR contact.caption like concat(cast(:name as text), '%')
               """)
    List<HsOfficeDebitorEntity> findDebitorByOptionalNameLike(String name);

    HsOfficeDebitorEntity save(final HsOfficeDebitorEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
