package net.hostsharing.hsadminng.hs.office.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.*;
import java.io.IOException;

import static jakarta.persistence.CascadeType.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_office", name = "partner_rv")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@DisplayAs("RbacPartner")
public class HsOfficePartnerRbacEntity extends HsOfficePartner<HsOfficePartnerRbacEntity> {

    public static RbacSpec rbac() {
        return rbacViewFor("partner", HsOfficePartnerRbacEntity.class)
                .withIdentityView(SQL.projection("'P-' || partnerNumber"))
                .withUpdatableColumns("partnerRelUuid")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT)

                .importRootEntityAliasProxy("partnerRel", HsOfficeRelationRbacEntity.class,
                        usingDefaultCase(),
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("partnerRelUuid"))
                .createPermission(DELETE).grantedTo("partnerRel", OWNER)
                .createPermission(UPDATE).grantedTo("partnerRel", ADMIN)
                .createPermission(SELECT).grantedTo("partnerRel", TENANT)

                .importSubEntityAlias("partnerDetails", HsOfficePartnerDetailsEntity.class,
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("detailsUuid"))
                .createPermission("partnerDetails", DELETE).grantedTo("partnerRel", OWNER)
                .createPermission("partnerDetails", UPDATE).grantedTo("partnerRel", AGENT)
                .createPermission("partnerDetails", SELECT).grantedTo("partnerRel", AGENT); // not TENANT!
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/504-partner/5043-hs-office-partner-rbac");
    }
}
