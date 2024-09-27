package net.hostsharing.hsadminng.hs.office.person;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.rbac.object.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.rbac.generator.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

// TODO.refa: split HsOfficePersonEntity into Real+Rbac-Entity
@Entity
@Table(schema = "hs_office", name = "person_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayAs("Person")
public class HsOfficePersonEntity implements BaseEntity<HsOfficePersonEntity>, Stringifyable {

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
