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
                WHERE ( CAST(:membershipUuid AS org.hibernate.type.UUIDCharType) IS NULL OR st.membership.uuid = :membershipUuid)
                    AND ( CAST(:fromValueDate AS java.time.LocalDate) IS NULL OR (st.valueDate >= :fromValueDate))
                    AND ( CAST(:toValueDate AS java.time.LocalDate)IS NULL OR (st.valueDate <= :toValueDate))
                ORDER BY st.membership.memberNumber, st.valueDate
               """)
    List<HsOfficeCoopSharesTransactionEntity> findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
            UUID membershipUuid, LocalDate fromValueDate, LocalDate toValueDate);

    HsOfficeCoopSharesTransactionEntity save(final HsOfficeCoopSharesTransactionEntity entity);

    long count();
}
