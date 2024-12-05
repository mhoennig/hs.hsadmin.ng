package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeBankAccountRepository extends Repository<HsOfficeBankAccountEntity, UUID> {

    @Timed("app.office.bankAccounts.repo.findByUuid")
    Optional<HsOfficeBankAccountEntity> findByUuid(UUID id);

    @Query("""
            SELECT c FROM HsOfficeBankAccountEntity c
                WHERE lower(c.holder) like lower(concat(:holder, '%'))
                ORDER BY c.holder
            """)
    @Timed("app.office.bankAccounts.repo.findByOptionalHolderLikeImpl")
    List<HsOfficeBankAccountEntity> findByOptionalHolderLikeImpl(String holder);

    default List<HsOfficeBankAccountEntity> findByOptionalHolderLike(String holder) {
        return findByOptionalHolderLikeImpl(holder == null ? "" : holder);
    }


    @Timed("app.office.bankAccounts.repo.findByIbanOrderByIbanAsc")
    List<HsOfficeBankAccountEntity> findByIbanOrderByIbanAsc(String iban);

    @Timed("app.office.bankAccounts.repo.save")
    <S extends HsOfficeBankAccountEntity> S save(S entity);

    @Timed("app.office.bankAccounts.repo.deleteByUuid")
    int deleteByUuid(final UUID uuid);

    @Timed("app.office.bankAccounts.repo.count")
    long count();
}
