package net.hostsharing.hsadminng.hs.office.bankaccount;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeBankAccountRepository extends Repository<HsOfficeBankAccountEntity, UUID> {

    Optional<HsOfficeBankAccountEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeBankAccountEntity c
                WHERE :holder is null
                    OR lower(c.holder) like lower(concat(:holder, '%'))
                ORDER BY c.holder
               """)
    List<HsOfficeBankAccountEntity> findByOptionalHolderLike(String holder);

    List<HsOfficeBankAccountEntity> findByIbanOrderByIban(String iban);

    HsOfficeBankAccountEntity save(final HsOfficeBankAccountEntity entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
