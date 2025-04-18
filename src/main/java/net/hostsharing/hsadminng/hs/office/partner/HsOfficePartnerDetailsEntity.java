package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;

import jakarta.persistence.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "partner_details_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("PartnerDetails")
public class HsOfficePartnerDetailsEntity implements BaseEntity<HsOfficePartnerDetailsEntity>, Stringifyable {

    private static Stringify<HsOfficePartnerDetailsEntity> stringify = stringify(
            HsOfficePartnerDetailsEntity.class,
            "partnerDetails")
            .withProp(HsOfficePartnerDetailsEntity::getRegistrationOffice)
            .withProp(HsOfficePartnerDetailsEntity::getRegistrationNumber)
            .withProp(HsOfficePartnerDetailsEntity::getBirthPlace)
            .withProp(HsOfficePartnerDetailsEntity::getBirthday)
            .withProp(HsOfficePartnerDetailsEntity::getBirthName)
            .withProp(HsOfficePartnerDetailsEntity::getDateOfDeath)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    private @Version int version;
    private @Column(name = "registrationoffice") String registrationOffice;
    private @Column(name = "registrationnumber") String registrationNumber;
    private @Column(name = "birthname") String birthName;
    private @Column(name = "birthplace") String birthPlace;
    private @Column(name = "birthday") LocalDate birthday;
    private @Column(name = "dateofdeath") LocalDate dateOfDeath;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return registrationNumber != null ? registrationNumber
                : birthName != null ? birthName
                : birthday != null ? birthday.toString()
                : dateOfDeath != null ? dateOfDeath.toString()
                : "<empty details>";
    }


    public static RbacSpec rbac() {
        return rbacViewFor("partnerDetails", HsOfficePartnerDetailsEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT partnerDetails.uuid as uuid, partner_iv.idName as idName
                            FROM hs_office.partner_details AS partnerDetails
                            JOIN hs_office.partner partner ON partner.detailsUuid = partnerDetails.uuid
                            JOIN hs_office.partner_iv partner_iv ON partner_iv.uuid = partner.uuid
                        """))
                .withRestrictedViewOrderBy(SQL.expression("uuid"))
                .withUpdatableColumns(
                        "registrationOffice",
                        "registrationNumber",
                        "birthPlace",
                        "birthName",
                        "birthday",
                        "dateOfDeath")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT)

                // The grants are defined in HsOfficePartnerEntity.rbac()
                // because they have to be changed when its partnerRel changes,
                // not when anything in partner details changes.
                ;
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/504-partner/5044-hs-office-partner-details-rbac");
    }
}
