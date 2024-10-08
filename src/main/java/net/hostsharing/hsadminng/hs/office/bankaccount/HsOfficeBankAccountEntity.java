package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacView.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.*;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "bankaccount_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayAs("BankAccount")
public class HsOfficeBankAccountEntity implements BaseEntity<HsOfficeBankAccountEntity>, Stringifyable {

    private static Stringify<HsOfficeBankAccountEntity> toString = stringify(HsOfficeBankAccountEntity.class, "bankAccount")
            .withIdProp(HsOfficeBankAccountEntity::getIban)
            .withProp(Fields.holder, HsOfficeBankAccountEntity::getHolder)
            .withProp(Fields.bic, HsOfficeBankAccountEntity::getBic);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    private String holder;

    private String iban;

    private String bic;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return holder;
    }

    public static RbacView rbac() {
        return rbacViewFor("bankAccount", HsOfficeBankAccountEntity.class)
                .withIdentityView(SQL.projection("iban"))
                .withUpdatableColumns("holder", "iban", "bic")

                .toRole(GLOBAL, GUEST).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(REFERRER, (with) -> {
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/505-bankaccount/5053-hs-office-bankaccount-rbac");
    }
}
