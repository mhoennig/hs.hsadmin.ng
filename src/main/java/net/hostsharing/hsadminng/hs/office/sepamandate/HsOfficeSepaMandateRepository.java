package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeSepaMandateRepository extends Repository<HsOfficeSepaMandateEntity, UUID> {

    @Timed("app.office.sepaMandates.repo.findByUuid")
    Optional<HsOfficeSepaMandateEntity> findByUuid(UUID id);

    default List<HsOfficeSepaMandateEntity> findSepaMandateByOptionalIban(final String iban) {
        return findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt(iban, null, null);
    }

    default List<HsOfficeSepaMandateEntity> findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt(
            final String iban, final UUID debitorUuid, final LocalDate validAt) {
        // a null parameter without a type gets bound as bytea, which PostgreSQL cannot cast to date
        return findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAtIsoDate(
                iban, debitorUuid, validAt == null ? null : validAt.toString());
    }

    @Query("""
            SELECT mandate FROM HsOfficeSepaMandateEntity mandate
                WHERE (:iban is null
                        OR mandate.bankAccount.iban like concat(cast(:iban as text), '%'))
                    AND (cast(:debitorUuid as uuid) is null
                        OR mandate.debitor.uuid = :debitorUuid)
                    AND (cast(:validAtIsoDate as text) is null
                        OR cast(sql('(? @> cast(? as date))', mandate.validity, cast(:validAtIsoDate as text)) as boolean) = true)
                ORDER BY mandate.bankAccount.iban
            """)
    @Timed("app.office.sepaMandates.repo.findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt")
    List<HsOfficeSepaMandateEntity> findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAtIsoDate(
            String iban, UUID debitorUuid, String validAtIsoDate);

    @Timed("app.office.sepaMandates.repo.save")
    HsOfficeSepaMandateEntity save(final HsOfficeSepaMandateEntity entity);

    @Timed("app.office.sepaMandates.repo.count")
    long count();

    @Timed("app.office.sepaMandates.repo.deleteByUuid")
    int deleteByUuid(UUID uuid);
}
