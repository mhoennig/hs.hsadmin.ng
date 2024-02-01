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
                WHERE lower(c.holder) like lower(concat(:holder, '%'))
                ORDER BY c.holder
               """)
    List<HsOfficeBankAccountEntity> findByOptionalHolderLikeImpl(String holder);
    default List<HsOfficeBankAccountEntity> findByOptionalHolderLike(String holder) {
        return findByOptionalHolderLikeImpl(holder == null ? "" : holder);
    }

    List<HsOfficeBankAccountEntity> findByIbanOrderByIbanAsc(String iban);

    <S extends HsOfficeBankAccountEntity> S save(S entity);

    int deleteByUuid(final UUID uuid);

    long count();
}
