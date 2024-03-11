package net.hostsharing.hsadminng.rbac.rbacdef;

import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacGrantDefinition.GrantType.PERM_TO_ROLE;
import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

public class InsertTriggerGenerator {

    private final RbacView rbacDef;
    private final String liquibaseTagPrefix;

    public InsertTriggerGenerator(final RbacView rbacDef, final String liqibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liqibaseTagPrefix;
    }

    void generateTo(final StringWriter plPgSql) {
        generateLiquibaseChangesetHeader(plPgSql);
        generateGrantInsertRoleToExistingCustomers(plPgSql);
        generateInsertPermissionGrantTrigger(plPgSql);
        generateInsertCheckTrigger(plPgSql);
        plPgSql.writeLn("--//");
    }

    private void generateLiquibaseChangesetHeader(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset ${liquibaseTagPrefix}-rbac-INSERT:1 endDelimiter:--//
                -- ----------------------------------------------------------------------------
                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix));
    }

    private void generateGrantInsertRoleToExistingCustomers(final StringWriter plPgSql) {
        getOptionalInsertSuperRole().ifPresent( superRoleDef -> {
            plPgSql.writeLn("""
                /*
                    Creates INSERT INTO ${rawSubTableName} permissions for the related ${rawSuperTableName} rows.
                 */
                do language plpgsql $$
                    declare
                        row ${rawSuperTableName};
                        permissionUuid uuid;
                        roleUuid uuid;
                    begin
                        call defineContext('create INSERT INTO ${rawSubTableName} permissions for the related ${rawSuperTableName} rows');
                    
                        FOR row IN SELECT * FROM ${rawSuperTableName}
                            LOOP
                                roleUuid := findRoleId(${rawSuperRoleDescriptor}(row));
                                permissionUuid := createPermission(row.uuid, 'INSERT', '${rawSubTableName}');
                                call grantPermissionToRole(roleUuid, permissionUuid);
                            END LOOP;
                    END;
                $$;
                """,
                with("rawSubTableName", rbacDef.getRootEntityAlias().getRawTableName()),
                with("rawSuperTableName", superRoleDef.getEntityAlias().getRawTableName()),
                with("rawSuperRoleDescriptor", toVar(superRoleDef))
                );
        });
    }

    private void generateInsertPermissionGrantTrigger(final StringWriter plPgSql) {
        getOptionalInsertSuperRole().ifPresent( superRoleDef -> {
            plPgSql.writeLn("""
                /**
                    Adds ${rawSubTableName} INSERT permission to specified role of new ${rawSuperTableName} rows.
                */
                create or replace function ${rawSubTableName}_${rawSuperTableName}_insert_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    call grantPermissionToRole(
                            ${rawSuperRoleDescriptor}(NEW),
                            createPermission(NEW.uuid, 'INSERT', '${rawSubTableName}'));
                    return NEW;
                end; $$;
                                
                create trigger ${rawSubTableName}_${rawSuperTableName}_insert_tg
                    after insert on ${rawSuperTableName}
                    for each row
                execute procedure ${rawSubTableName}_${rawSuperTableName}_insert_tf();
                """,
                with("rawSubTableName", rbacDef.getRootEntityAlias().getRawTableName()),
                with("rawSuperTableName", superRoleDef.getEntityAlias().getRawTableName()),
                with("rawSuperRoleDescriptor", toVar(superRoleDef))
            );
        });
    }

    private void generateInsertCheckTrigger(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                    /**
                        Checks if the user or assumed roles are allowed to insert a row to ${rawSubTable}.
                    */
                    create or replace function ${rawSubTable}_insert_permission_missing_tf()
                        returns trigger
                        language plpgsql as $$
                    begin
                        raise exception '[403] insert into ${rawSubTable} not allowed for current subjects % (%)',
                            currentSubjects(), currentSubjectsUuids();
                    end; $$;
                    """,
                    with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
        getOptionalInsertGrant().ifPresentOrElse(g -> {
            plPgSql.writeLn("""            
                create trigger ${rawSubTable}_insert_permission_check_tg
                    before insert on ${rawSubTable}
                    for each row
                    when ( not hasInsertPermission(NEW.${referenceColumn}, 'INSERT', '${rawSubTable}') )
                        execute procedure ${rawSubTable}_insert_permission_missing_tf();
                """,
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()),
                with("referenceColumn", g.getSuperRoleDef().getEntityAlias().dependsOnColumName() ));
        },
        () -> {
            plPgSql.writeLn("""            
                create trigger ${rawSubTable}_insert_permission_check_tg
                    before insert on ${rawSubTable}
                    for each row
                    -- As there is no explicit INSERT grant specified for this table,
                    -- only global admins are allowed to insert any rows.
                    when ( not isGlobalAdmin() )
                        execute procedure ${rawSubTable}_insert_permission_missing_tf();
                """,
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
        });
    }

    private Stream<RbacView.RbacGrantDefinition> getInsertGrants() {
        return rbacDef.getGrantDefs().stream()
                .filter(g -> g.grantType() == PERM_TO_ROLE)
                .filter(g -> g.getPermDef().toCreate && g.getPermDef().getPermission() == INSERT);
    }

    private Optional<RbacView.RbacGrantDefinition> getOptionalInsertGrant() {
        return getInsertGrants()
                .reduce(singleton());
    }

    private Optional<RbacView.RbacRoleDefinition> getOptionalInsertSuperRole() {
        return getInsertGrants()
                .map(RbacView.RbacGrantDefinition::getSuperRoleDef)
                .reduce(singleton());
    }

    private static <T> BinaryOperator<T> singleton() {
        return (x, y) -> {
            throw new IllegalStateException("only a single INSERT permission grant allowed");
        };
    }

    private static String toVar(final RbacView.RbacRoleDefinition roleDef) {
        return uncapitalize(roleDef.getEntityAlias().simpleName()) + capitalize(roleDef.getRole().roleName());
    }

}
