package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_coopsharestransaction_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("CoopShareTransaction")
public class HsOfficeCoopSharesTransactionEntity implements Stringifyable, RbacObject {

    private static Stringify<HsOfficeCoopSharesTransactionEntity> stringify = stringify(HsOfficeCoopSharesTransactionEntity.class)
            .withProp(HsOfficeCoopSharesTransactionEntity::getMemberNumberTagged)
            .withProp(HsOfficeCoopSharesTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopSharesTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopSharesTransactionEntity::getShareCount)
            .withProp(HsOfficeCoopSharesTransactionEntity::getReference)
            .withProp(HsOfficeCoopSharesTransactionEntity::getComment)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "membershipuuid")
    private HsOfficeMembershipEntity membership;

    @Column(name = "transactiontype")
    @Enumerated(EnumType.STRING)
    private HsOfficeCoopSharesTransactionType transactionType;

    /**
     * The signed value which directly affects the booking balance.
     *
     * <p>This means, that a SUBSCRIPTION is always positive, a CANCELLATION is always negative,
     * but an ADJUSTMENT can bei either positive or negative.
     * See {@link HsOfficeCoopSharesTransactionType} for</p> more information.
     */
    @Column(name = "valuedate")
    private LocalDate valueDate;

    @Column(name = "sharecount")
    private int shareCount;

    /**
     * The Booking reference.
     */
    @Column(name = "reference")
    private String reference;

    /**
     * An optional arbitrary comment.
     */
    @Column(name = "comment")
    private String comment;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    private String getMemberNumberTagged() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse(null);
    }

    @Override
    public String toShortString() {
        return "%s%+d".formatted(getMemberNumberTagged(), shareCount);
    }

    public static RbacView rbac() {
        return rbacViewFor("coopSharesTransaction", HsOfficeCoopSharesTransactionEntity.class)
                .withIdentityView(SQL.projection("reference"))
                .withUpdatableColumns("comment")
                .importEntityAlias("membership", HsOfficeMembershipEntity.class,
                        dependsOnColumn("membershipUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .toRole("membership", ADMIN).grantPermission(INSERT)
                .toRole("membership", ADMIN).grantPermission(UPDATE)
                .toRole("membership", AGENT).grantPermission(SELECT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/511-coopshares/5113-hs-office-coopshares-rbac");
    }
}
