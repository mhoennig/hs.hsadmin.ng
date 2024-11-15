package net.hostsharing.hsadminng.hs.office.membership;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeMembershipRepository extends Repository<HsOfficeMembershipEntity, UUID> {

    Optional<HsOfficeMembershipEntity> findByUuid(UUID id);

    HsOfficeMembershipEntity save(final HsOfficeMembershipEntity entity);

    List<HsOfficeMembershipEntity> findAll();

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE ( CAST(:partnerUuid as org.hibernate.type.UUIDCharType) IS NULL
                        OR membership.partner.uuid = :partnerUuid )
                ORDER BY membership.partner.partnerNumber, membership.memberNumberSuffix
            """)
    List<HsOfficeMembershipEntity> findMembershipsByOptionalPartnerUuid(UUID partnerUuid);

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE (:partnerNumber = membership.partner.partnerNumber) 
                    AND (membership.memberNumberSuffix = :suffix)
                ORDER BY membership.memberNumberSuffix
               """)
    HsOfficeMembershipEntity findMembershipByPartnerNumberAndSuffix(
            @NotNull Integer partnerNumber,
            @NotNull String suffix);

    default HsOfficeMembershipEntity findMembershipByMemberNumber(Integer memberNumber) {
        final var partnerNumber = memberNumber / 100;
        final String suffix = String.format("%02d", memberNumber % 100);
        final var result = findMembershipByPartnerNumberAndSuffix(partnerNumber, suffix);
        return result;
    }

    long count();

    int deleteByUuid(UUID uuid);
}
