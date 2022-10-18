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
                WHERE (:memberNumber is null OR membership.memberNumber = :memberNumber)
                    AND ( CAST(:partnerUuid as org.hibernate.type.UUIDCharType) IS NULL
                         OR membership.partner.uuid = :partnerUuid )
                ORDER BY membership.memberNumber
               """)
    List<HsOfficeMembershipEntity> findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(UUID partnerUuid, Integer memberNumber);

    HsOfficeMembershipEntity save(final HsOfficeMembershipEntity entity);

    long count();

    int deleteByUuid(UUID uuid);
}
