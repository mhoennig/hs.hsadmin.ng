package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeSepaMandateRepository extends Repository<HsOfficeSepaMandateEntity, UUID> {

    @Timed("app.office.sepaMandates.repo.findByUuid")
    Optional<HsOfficeSepaMandateEntity> findByUuid(UUID id);

    @Query("""
            SELECT mandate FROM HsOfficeSepaMandateEntity mandate
                WHERE :iban is null
                    OR mandate.bankAccount.iban like concat(cast(:iban as text), '%')
                ORDER BY mandate.bankAccount.iban
            """)
    @Timed("app.office.sepaMandates.repo.findSepaMandateByOptionalIban")
    List<HsOfficeSepaMandateEntity> findSepaMandateByOptionalIban(String iban);

    @Timed("app.office.sepaMandates.repo.save")
    HsOfficeSepaMandateEntity save(final HsOfficeSepaMandateEntity entity);

    @Timed("app.office.sepaMandates.repo.count")
    long count();

    @Timed("app.office.sepaMandates.repo.deleteByUuid")
    int deleteByUuid(UUID uuid);
}
