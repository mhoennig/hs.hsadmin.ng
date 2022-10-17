package net.hostsharing.hsadminng.hs.office.membership;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsOfficeMembershipRepository extends Repository<HsOfficeMembershipEntity, UUID> {

    Optional<HsOfficeMembershipEntity> findByUuid(UUID id);

    @Query("""
            SELECT membership FROM HsOfficeMembershipEntity membership
                WHERE :memberNumber is null
                    OR membership.memberNumber = :memberNumber
                ORDER BY membership.memberNumber
               """)
    List<HsOfficeMembershipEntity> findMembershipByOptionalMemberNumber(Integer memberNumber);

    List<HsOfficeMembershipEntity> findMembershipsByPartnerUuid(UUID partnerUuid);

    HsOfficeMembershipEntity save(final HsOfficeMembershipEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
