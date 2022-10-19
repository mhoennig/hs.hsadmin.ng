package net.hostsharing.hsadminng.hs.office.coopshares;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeCoopSharesTransactionRepository extends Repository<HsOfficeCoopSharesTransactionEntity, UUID> {

    Optional<HsOfficeCoopSharesTransactionEntity> findByUuid(UUID id);

    @Query("""
            SELECT st FROM HsOfficeCoopSharesTransactionEntity st
                WHERE (:memberNumber IS NULL OR st.membership.memberNumber = :memberNumber)
                    AND ( CAST(:fromValueDate AS java.time.LocalDate) IS NULL OR (st.valueDate >= :fromValueDate))
                    AND ( CAST(:toValueDate AS java.time.LocalDate)IS NULL OR (st.valueDate <= :toValueDate))
                ORDER BY st.membership.memberNumber, st.valueDate
               """)
    List<HsOfficeCoopSharesTransactionEntity> findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
            Integer memberNumber, LocalDate fromValueDate, LocalDate toValueDate);

    HsOfficeCoopSharesTransactionEntity save(final HsOfficeCoopSharesTransactionEntity entity);

    long count();
}
