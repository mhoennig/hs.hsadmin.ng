package net.hostsharing.hsadminng.hs.office.membership;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeMembershipRepository extends Repository<HsOfficeMembershipEntity, UUID> {

    @Timed("app.office.membership.repo.findByUuid")
    Optional<HsOfficeMembershipEntity> findByUuid(UUID id);

    @Timed("app.office.membership.repo.save")
    HsOfficeMembershipEntity save(final HsOfficeMembershipEntity entity);

    @Timed("app.office.membership.repo.findAll")
    List<HsOfficeMembershipEntity> findAll();

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE membership.partner.uuid = :partnerUuid
                ORDER BY membership.partner.partnerNumber, membership.memberNumberSuffix
            """)
    @Timed("app.office.membership.repo.findMembershipsByOptionalPartnerUuid")
    List<HsOfficeMembershipEntity> findMembershipsByPartnerUuid(UUID partnerUuid);

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE membership.partner.partnerNumber = :partnerNumber
                ORDER BY membership.partner.partnerNumber, membership.memberNumberSuffix
            """)
    @Timed("app.office.membership.repo.findMembershipsByPartnerNumber")
    List<HsOfficeMembershipEntity> findMembershipsByPartnerNumber(Integer partnerNumber);

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE (:partnerNumber = membership.partner.partnerNumber) 
                    AND (membership.memberNumberSuffix = :suffix)
                ORDER BY membership.memberNumberSuffix
               """)
    @Timed("app.office.membership.repo.findMembershipByMemberNumber")
    Optional<HsOfficeMembershipEntity> findMembershipByPartnerNumberAndSuffix(
            @NotNull Integer partnerNumber,
            @NotNull String suffix);

    default Optional<HsOfficeMembershipEntity> findMembershipByMemberNumber(final Integer memberNumber) {
        final var partnerNumber = memberNumber / 100;
        final String suffix = String.format("%02d", memberNumber % 100);
        final var result = findMembershipByPartnerNumberAndSuffix(partnerNumber, suffix);
        return result;
    }

    @Timed("app.office.membership.repo.count")
    long count();

    @Timed("app.office.membership.repo.deleteByUuid")
    int deleteByUuid(UUID uuid);
}
