package net.hostsharing.hsadminng.hs.office.person;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.*;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_office", name = "person_rv")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@DisplayAs("RbacPerson")
public class HsOfficePersonRbacEntity extends HsOfficePerson<HsOfficePersonRbacEntity> {

    public static RbacSpec rbac() {
        return rbacViewFor("person", HsOfficePersonRbacEntity.class)
                .withIdentityView(SQL.projection("concat(tradeName, familyName, givenName)"))
                .withUpdatableColumns("personType", "title", "salutation", "tradeName", "givenName", "familyName")
                .toRole(GLOBAL, GUEST).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.permission(DELETE);
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(REFERRER, (with) -> {
                    with.permission(SELECT);
                });
    }


    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/502-person/5023-hs-office-person-rbac");
    }
}
