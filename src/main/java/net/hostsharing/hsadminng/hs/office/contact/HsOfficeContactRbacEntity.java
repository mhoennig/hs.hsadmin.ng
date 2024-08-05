package net.hostsharing.hsadminng.hs.office.contact;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;

import jakarta.persistence.*;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;

@Entity
@Table(name = "hs_office_contact_rv")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@DisplayAs("RbacContact")
public class HsOfficeContactRbacEntity extends HsOfficeContact {

    public static RbacView rbac() {
        return rbacViewFor("contact", HsOfficeContactRbacEntity.class)
                .withIdentityView(SQL.projection("caption"))
                .withUpdatableColumns("caption", "postalAddress", "emailAddresses", "phoneNumbers")
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
                })
                .toRole(GLOBAL, GUEST).grantPermission(INSERT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/501-contact/5013-hs-office-contact-rbac");
    }
}
