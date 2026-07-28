package net.hostsharing.hsadminng.hs.office.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity.MEMBER_NUMBER_TAG;

/**
 * Raw (non-RBAC) membership access for resolving cross-membership references,
 * where the referring row already grants visibility to the referenced membership.
 */
@Entity
@Table(schema = "hs_office", name = "membership")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("RealMembership")
public class HsOfficeMembershipRealEntity implements BaseEntity<HsOfficeMembershipRealEntity> {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerRealEntity partner;

    @Column(name = "membernumbersuffix", length = 2)
    private String memberNumberSuffix;

    public Integer getMemberNumber() {
        if (partner == null || partner.getPartnerNumber() == null || memberNumberSuffix == null) {
            return null;
        }
        return partner.getPartnerNumber() * 100 + Integer.parseInt(memberNumberSuffix, 10);
    }

    public String getTaggedMemberNumber() {
        return MEMBER_NUMBER_TAG + getMemberNumber();
    }
}
