package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_bankaccount_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("BankAccount")
public class HsOfficeBankAccountEntity implements HasUuid, Stringifyable {

    private static Stringify<HsOfficeBankAccountEntity> toString = stringify(HsOfficeBankAccountEntity.class, "bankAccount")
            .withProp(Fields.holder, HsOfficeBankAccountEntity::getHolder)
            .withProp(Fields.iban, HsOfficeBankAccountEntity::getIban)
            .withProp(Fields.bic, HsOfficeBankAccountEntity::getBic);

    @Id
    @GeneratedValue
    private UUID uuid;

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
                .withIdentityView(SQL.projection("iban || ':' || holder"))
                .withUpdatableColumns("holder", "iban", "bic")
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
        rbac().generateWithBaseFileName("243-hs-office-bankaccount-rbac-generated");
    }
}
