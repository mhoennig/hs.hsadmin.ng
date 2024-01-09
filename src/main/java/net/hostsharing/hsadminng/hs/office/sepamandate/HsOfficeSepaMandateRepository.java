package net.hostsharing.hsadminng.hs.office.sepamandate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeSepaMandateRepository extends Repository<HsOfficeSepaMandateEntity, UUID> {

    Optional<HsOfficeSepaMandateEntity> findByUuid(UUID id);

    @Query("""
            SELECT mandate FROM HsOfficeSepaMandateEntity mandate
                WHERE :iban is null
                    OR mandate.bankAccount.iban like concat(cast(:iban as text), '%')
                ORDER BY mandate.bankAccount.iban
               """)
    List<HsOfficeSepaMandateEntity> findSepaMandateByOptionalIban(String iban);

    HsOfficeSepaMandateEntity save(final HsOfficeSepaMandateEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
