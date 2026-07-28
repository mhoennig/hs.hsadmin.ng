package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;

/**
 * Raw (non-RBAC) coop-asset-transaction access for resolving the self-referential
 * links off an RBAC-visible root row, without paying the restricted view per link.
 */
@Entity
@Table(schema = "hs_office", name = "coopassettx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("RealCoopAssetsTransaction")
public class HsOfficeCoopAssetsTransactionRealEntity implements BaseEntity<HsOfficeCoopAssetsTransactionRealEntity> {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membershipuuid")
    private HsOfficeMembershipRealEntity membership;

    @Column(name = "transactiontype")
    @Enumerated(EnumType.STRING)
    private HsOfficeCoopAssetsTransactionType transactionType;

    @Column(name = "valuedate")
    private LocalDate valueDate;

    @Column(name = "assetvalue")
    private BigDecimal assetValue;

    @Column(name = "reference")
    private String reference;

    @Column(name = "comment")
    private String comment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revertedassettxuuid")
    private HsOfficeCoopAssetsTransactionRealEntity revertedAssetTx;

    @OneToOne(mappedBy = "revertedAssetTx", fetch = FetchType.LAZY)
    private HsOfficeCoopAssetsTransactionRealEntity reversalAssetTx;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assetadoptiontxuuid")
    private HsOfficeCoopAssetsTransactionRealEntity adoptionAssetTx;

    @OneToOne(mappedBy = "adoptionAssetTx", fetch = FetchType.LAZY)
    private HsOfficeCoopAssetsTransactionRealEntity transferAssetTx;

    public String getTaggedMemberNumber() {
        return ofNullable(membership).map(HsOfficeMembershipRealEntity::getTaggedMemberNumber).orElse("M-???????");
    }
}
