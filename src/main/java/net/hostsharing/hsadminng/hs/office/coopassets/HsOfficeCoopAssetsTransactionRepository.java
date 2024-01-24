package net.hostsharing.hsadminng.hs.office.coopassets;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeCoopAssetsTransactionRepository extends Repository<HsOfficeCoopAssetsTransactionEntity, UUID> {

    Optional<HsOfficeCoopAssetsTransactionEntity> findByUuid(UUID id);

    @Query("""
            SELECT at FROM HsOfficeCoopAssetsTransactionEntity at
                WHERE ( CAST(:membershipUuid AS org.hibernate.type.UUIDCharType) IS NULL OR at.membership.uuid = :membershipUuid)
                    AND ( CAST(:fromValueDate AS java.time.LocalDate) IS NULL OR (at.valueDate >= :fromValueDate))
                    AND ( CAST(:toValueDate AS java.time.LocalDate)IS NULL OR (at.valueDate <= :toValueDate))
                ORDER BY at.membership.memberNumberSuffix, at.valueDate
               """)
    List<HsOfficeCoopAssetsTransactionEntity> findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
            UUID membershipUuid, LocalDate fromValueDate, LocalDate toValueDate);

    HsOfficeCoopAssetsTransactionEntity save(final HsOfficeCoopAssetsTransactionEntity entity);

    long count();
}
