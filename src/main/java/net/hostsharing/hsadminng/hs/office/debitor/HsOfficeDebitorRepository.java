package net.hostsharing.hsadminng.hs.office.debitor;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.lambda.Reducer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeDebitorRepository extends Repository<HsOfficeDebitorEntity, UUID> {

    @Timed("app.office.debitors.repo.findByUuid")
    Optional<HsOfficeDebitorEntity> findByUuid(UUID id);

    @Timed("app.office.debitors.repo.findDebitorByPartnerUuid")
    List<HsOfficeDebitorEntity> findDebitorsByPartnerUuid(UUID partnerUuid);

    @Query("""
            SELECT debitor FROM HsOfficeDebitorEntity debitor
            JOIN HsOfficePartnerRealEntity partner
                    ON partner.partnerRel.holder = debitor.debitorRel.anchor
                        AND partner.partnerRel.type = 'PARTNER' AND debitor.debitorRel.type = 'DEBITOR'
                WHERE partner.partnerNumber = :partnerNumber
                  AND (:debitorNumberSuffix IS NULL OR debitor.debitorNumberSuffix = :debitorNumberSuffix)
            """)
    @Timed("app.office.debitors.repo.findDebitorByPartnerNumberAndDebitorNumberSuffix")
    List<HsOfficeDebitorEntity> findDebitorByPartnerNumberAndOptionalDebitorNumberSuffix(int partnerNumber, String debitorNumberSuffix);

    default Optional<HsOfficeDebitorEntity> findDebitorByDebitorNumber(int debitorNumber) {
        final var partnerNumber = debitorNumber / 100;
        final String suffix = String.format("%02d", debitorNumber % 100);
        final var result = findDebitorByPartnerNumberAndOptionalDebitorNumberSuffix(partnerNumber, suffix);
        return result.stream().reduce(Reducer::toSingleElement);
    }

    default List<HsOfficeDebitorEntity> findDebitorsByPartnerNumber(int partnerNumber) {
        final var result = findDebitorByPartnerNumberAndOptionalDebitorNumberSuffix(partnerNumber, null);
        return result;
    }

    @Query("""
            SELECT debitor FROM HsOfficeDebitorEntity debitor
                JOIN HsOfficePartnerRealEntity partner
                    ON partner.partnerRel.holder = debitor.debitorRel.anchor
                        AND partner.partnerRel.type = 'PARTNER' AND debitor.debitorRel.type = 'DEBITOR'
                JOIN HsOfficePersonRealEntity person
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
    @Timed("app.office.debitors.repo.findDebitorByOptionalNameLike")
    List<HsOfficeDebitorEntity> findDebitorsByOptionalNameLike(String name);

    @Timed("app.office.debitors.repo.save")
    HsOfficeDebitorEntity save(final HsOfficeDebitorEntity entity);

    @Timed("app.office.debitors.repo.count")
    long count();

    @Timed("app.office.debitors.repo.deleteByUuid")
    int deleteByUuid(UUID uuid);
}
