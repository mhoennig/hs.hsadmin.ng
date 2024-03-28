package net.hostsharing.hsadminng.rbac.rbacdef;

import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.rbac.rbacdef.PostgresTriggerReference.NEW;
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
        generateGrantInsertRoleToExistingObjects(plPgSql);
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

    private void generateGrantInsertRoleToExistingObjects(final StringWriter plPgSql) {
        getOptionalInsertSuperRole().ifPresent( superRoleDef -> {
            plPgSql.writeLn("""
                /*
                    Creates INSERT INTO ${rawSubTableName} permissions for the related ${rawSuperTableName} rows.
                 */
                do language plpgsql $$
                    declare
                        row ${rawSuperTableName};
                    begin
                        call defineContext('create INSERT INTO ${rawSubTableName} permissions for the related ${rawSuperTableName} rows');
                    
                        FOR row IN SELECT * FROM ${rawSuperTableName}
                            LOOP
                                call grantPermissionToRole(
                                    createPermission(row.uuid, 'INSERT', '${rawSubTableName}'),
                                    ${rawSuperRoleDescriptor});
                            END LOOP;
                    END;
                $$;
                """,
                with("rawSubTableName", rbacDef.getRootEntityAlias().getRawTableName()),
                with("rawSuperTableName", superRoleDef.getEntityAlias().getRawTableName()),
                with("rawSuperRoleDescriptor", toRoleDescriptor(superRoleDef, "row"))
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
                            createPermission(NEW.uuid, 'INSERT', '${rawSubTableName}'),
                            ${rawSuperRoleDescriptor});
                    return NEW;
                end; $$;
                                
                -- z_... is to put it at the end of after insert triggers, to make sure the roles exist                
                create trigger z_${rawSubTableName}_${rawSuperTableName}_insert_tg
                    after insert on ${rawSuperTableName}
                    for each row
                execute procedure ${rawSubTableName}_${rawSuperTableName}_insert_tf();
                """,
                with("rawSubTableName", rbacDef.getRootEntityAlias().getRawTableName()),
                with("rawSuperTableName", superRoleDef.getEntityAlias().getRawTableName()),
                with("rawSuperRoleDescriptor", toRoleDescriptor(superRoleDef, NEW.name()))
            );
        });
    }

    private void generateInsertCheckTrigger(final StringWriter plPgSql) {
        getOptionalInsertGrant().ifPresentOrElse(g -> {
            if (g.getSuperRoleDef().getEntityAlias().isGlobal()) {
                switch (g.getSuperRoleDef().getRole()) {
                    case ADMIN -> {
                        generateInsertPermissionTriggerAllowOnlyGlobalAdmin(plPgSql);
                    }
                    case GUEST -> {
                        // no permission check trigger generated, as anybody can insert rows into this table
                    }
                    default -> {
                        throw new IllegalArgumentException(
                                "invalid global role for INSERT permission: " + g.getSuperRoleDef().getRole());
                    }
                }
            } else {
                if (g.getSuperRoleDef().getEntityAlias().isFetchedByDirectForeignKey()) {
                    generateInsertPermissionTriggerAllowByRoleOfDirectForeignKey(plPgSql, g);
                } else {
                    generateInsertPermissionTriggerAllowByRoleOfIndirectForeignKey(plPgSql, g);
                }
            }
        },
        () -> {
            System.err.println("WARNING: no explicit INSERT grant for " + rbacDef.getRootEntityAlias().simpleName() + " => implicitly grant INSERT to global.admin");
            generateInsertPermissionTriggerAllowOnlyGlobalAdmin(plPgSql);
        });
    }

    private void generateInsertPermissionTriggerAllowByRoleOfDirectForeignKey(final StringWriter plPgSql, final RbacView.RbacGrantDefinition g) {
        plPgSql.writeLn("""
                /**
                    Checks if the user or assumed roles are allowed to insert a row to ${rawSubTable},
                    where the check is performed by a direct role.
                    
                    A direct role is a role depending on a foreign key directly available in the NEW row.
                */
                create or replace function ${rawSubTable}_insert_permission_missing_tf()
                    returns trigger
                    language plpgsql as $$
                begin
                    raise exception '[403] insert into ${rawSubTable} not allowed for current subjects % (%)',
                        currentSubjects(), currentSubjectsUuids();
                end; $$;

                create trigger ${rawSubTable}_insert_permission_check_tg
                    before insert on ${rawSubTable}
                    for each row
                    when ( not hasInsertPermission(NEW.${referenceColumn}, 'INSERT', '${rawSubTable}') )
                        execute procedure ${rawSubTable}_insert_permission_missing_tf();
                """,
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()),
                with("referenceColumn", g.getSuperRoleDef().getEntityAlias().dependsOnColumName()));
    }

    private void generateInsertPermissionTriggerAllowByRoleOfIndirectForeignKey(
            final StringWriter plPgSql,
            final RbacView.RbacGrantDefinition g) {
        plPgSql.writeLn("""
                /**
                    Checks if the user or assumed roles are allowed to insert a row to ${rawSubTable},
                    where the check is performed by an indirect role.
                    
                    An indirect role is a role which depends on an object uuid which is not a direct foreign key
                    of the source entity, but needs to be fetched via joined tables.
                */
                create or replace function ${rawSubTable}_insert_permission_check_tf()
                    returns trigger
                    language plpgsql as $$

                declare
                    superRoleObjectUuid uuid;

                begin
                """,
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
        plPgSql.chopEmptyLines();
        plPgSql.indented(2, () ->  {
            plPgSql.writeLn(
                    "superRoleObjectUuid := (" + g.getSuperRoleDef().getEntityAlias().fetchSql().sql + ");\n" +
                    "assert superRoleObjectUuid is not null, 'superRoleObjectUuid must not be null';",
                    with("columns", g.getSuperRoleDef().getEntityAlias().aliasName() + ".uuid"),
                    with("ref", NEW.name()));
        });
        plPgSql.writeLn();
        plPgSql.writeLn("""
                        if ( not hasInsertPermission(superRoleObjectUuid, 'INSERT', '${rawSubTable}') ) then
                            raise exception
                                '[403] insert into ${rawSubTable} not allowed for current subjects % (%)',
                                currentSubjects(), currentSubjectsUuids();
                    end if;
                    return NEW;
                end; $$;

                create trigger ${rawSubTable}_insert_permission_check_tg
                    before insert on ${rawSubTable}
                    for each row
                        execute procedure ${rawSubTable}_insert_permission_check_tf();
                    
                """,
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
    }

    private void generateInsertPermissionTriggerAllowOnlyGlobalAdmin(final StringWriter plPgSql) {
        plPgSql.writeLn("""
            /**
                Checks if the user or assumed roles are allowed to insert a row to ${rawSubTable},
                where only global-admin has that permission.
            */
            create or replace function ${rawSubTable}_insert_permission_missing_tf()
                returns trigger
                language plpgsql as $$
            begin
                raise exception '[403] insert into ${rawSubTable} not allowed for current subjects % (%)',
                    currentSubjects(), currentSubjectsUuids();
            end; $$;
                       
            create trigger ${rawSubTable}_insert_permission_check_tg
                before insert on ${rawSubTable}
                for each row
                when ( not isGlobalAdmin() )
                    execute procedure ${rawSubTable}_insert_permission_missing_tf();
            """,
            with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
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


    private String toRoleDescriptor(final RbacView.RbacRoleDefinition roleDef, final String ref) {
        final var functionName = toVar(roleDef);
        if (roleDef.getEntityAlias().isGlobal()) {
            return functionName + "()";
        }
        return functionName + "(" + ref + ")";
    }
}
