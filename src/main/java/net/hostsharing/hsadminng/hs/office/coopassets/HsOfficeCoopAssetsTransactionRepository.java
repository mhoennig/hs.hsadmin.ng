package net.hostsharing.hsadminng.hs.office.coopassets;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeCoopAssetsTransactionRepository extends Repository<HsOfficeCoopAssetsTransactionEntity, UUID> {

    @Timed("app.office.coopAssets.repo.findByUuid")
    Optional<HsOfficeCoopAssetsTransactionEntity> findByUuid(UUID id);

    @Query("""
            SELECT at FROM HsOfficeCoopAssetsTransactionEntity at
                WHERE ( CAST(:membershipUuid AS org.hibernate.type.UUIDCharType) IS NULL OR at.membership.uuid = :membershipUuid)
                    AND ( CAST(:fromValueDate AS java.time.LocalDate) IS NULL OR (at.valueDate >= :fromValueDate))
                    AND ( CAST(:toValueDate AS java.time.LocalDate)IS NULL OR (at.valueDate <= :toValueDate))
                ORDER BY at.membership.memberNumberSuffix, at.valueDate
            """)
    @Timed("app.office.coopAssets.repo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange")
    List<HsOfficeCoopAssetsTransactionEntity> findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
            UUID membershipUuid, LocalDate fromValueDate, LocalDate toValueDate);

    @Timed("app.office.coopAssets.repo.save")
    HsOfficeCoopAssetsTransactionEntity save(final HsOfficeCoopAssetsTransactionEntity entity);

    @Timed("app.office.coopAssets.repo.count")
    long count();
}
