package net.hostsharing.hsadminng.hs.office.person;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_person_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("Person")
public class HsOfficePersonEntity implements RbacObject, Stringifyable {

    private static Stringify<HsOfficePersonEntity> toString = stringify(HsOfficePersonEntity.class, "person")
            .withProp(Fields.personType, HsOfficePersonEntity::getPersonType)
            .withProp(Fields.tradeName, HsOfficePersonEntity::getTradeName)
            .withProp(Fields.salutation, HsOfficePersonEntity::getSalutation)
            .withProp(Fields.title, HsOfficePersonEntity::getTitle)
            .withProp(Fields.familyName, HsOfficePersonEntity::getFamilyName)
            .withProp(Fields.givenName, HsOfficePersonEntity::getGivenName);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "persontype")
    private HsOfficePersonType personType;

    @Column(name = "tradename")
    private String tradeName;

    @Column(name = "salutation")
    private String salutation;

    @Column(name = "title")
    private String title;

    @Column(name = "familyname")
    private String familyName;

    @Column(name = "givenname")
    private String givenName;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return personType + " " +
                (!StringUtils.isEmpty(tradeName) ? tradeName : (familyName + ", " + givenName));
    }

    public static RbacView rbac() {
        return rbacViewFor("person", HsOfficePersonEntity.class)
                .withIdentityView(SQL.projection("concat(tradeName, familyName, givenName)"))
                .withUpdatableColumns("personType", "title", "salutation", "tradeName", "givenName", "familyName")
                .toRole("global", GUEST).grantPermission(INSERT)

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
