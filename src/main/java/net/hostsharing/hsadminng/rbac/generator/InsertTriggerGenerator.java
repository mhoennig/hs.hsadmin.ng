package net.hostsharing.hsadminng.rbac.generator;

import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.generator.PostgresTriggerReference.NEW;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition.GrantType.PERM_TO_ROLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.GUEST;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;
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
        if (isInsertPermissionGrantedToGlobalGuest()) {
            // any user is allowed to insert new rows => no insert check needed
            return;
        }

        generateInsertGrants(plPgSql);
        generateInsertPermissionChecks(plPgSql);
    }

    private void generateInsertGrants(final StringWriter plPgSql) {
        if (isInsertPermissionIsNotGrantedAtAll()) {
            generateInsertPermissionTriggerAlwaysDisallow(plPgSql);
        } else {
            generateInsertPermissionGrants(plPgSql);
        }
    }

    private void generateInsertPermissionGrants(final StringWriter plPgSql) {
        plPgSql.writeLn("""
            -- ============================================================================
            --changeset InsertTriggerGenerator:${liquibaseTagPrefix}-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
            -- ----------------------------------------------------------------------------
            """,
                with("liquibaseTagPrefix", liquibaseTagPrefix));

        getInsertGrants().forEach( g -> {
            plPgSql.writeLn("""
                    -- granting INSERT permission to ${rawSubTable} ----------------------------
                    """,
                with("rawSubTable", g.getSuperRoleDef().getEntityAlias().getRawTableNameWithSchema()));

            if (isGrantToADifferentTable(g)) {
                plPgSql.writeLn(
                    """
                    /*
                        Grants INSERT INTO ${rawSubTable} permissions to specified role of pre-existing ${rawSuperTable} rows.
                     */
                    do language plpgsql $$
                        declare
                            row ${rawSuperTable};
                        begin
                            call base.defineContext('create INSERT INTO ${rawSubTable} permissions for pre-exising ${rawSuperTable} rows');
                                
                            FOR row IN SELECT * FROM ${rawSuperTable}
                                ${whenCondition}
                                LOOP
                                    call rbac.grantPermissionToRole(
                                            rbac.createPermission(row.uuid, 'INSERT', '${rawSubTable}'),
                                            ${superRoleRef});
                                END LOOP;
                        end;
                    $$;
                    """,
                    with("whenCondition", g.getSuperRoleDef().getEntityAlias().isCaseDependent()
                            // TODO.impl: 'type' needs to be dynamically generated
                            ? "WHERE type = '${value}'"
                            .replace("${value}", g.getSuperRoleDef().getEntityAlias().usingCase().value)
                            : "-- unconditional for all rows in that table"),
                    with("rawSuperTable", g.getSuperRoleDef().getEntityAlias().getRawTableNameWithSchema()),
                    with("rawSubTable", g.getPermDef().getEntityAlias().getRawTableNameWithSchema()),
                    with("superRoleRef", toRoleDescriptor(g.getSuperRoleDef(), "row")));
            } else {
                plPgSql.writeLn("""
                    -- Granting INSERT INTO hs_hosting_asset permissions to specified role of pre-existing hs_hosting_asset rows slipped,
                    -- because there cannot yet be any pre-existing rows in the same table yet.
                    """,
                    with("rawSuperTable", g.getSuperRoleDef().getEntityAlias().getRawTableNameWithSchema()),
                    with("rawSubTable", g.getPermDef().getEntityAlias().getRawTableNameWithSchema()));
            }

            plPgSql.writeLn("""    
                /**
                    Grants ${rawSubTable} INSERT permission to specified role of new ${rawSuperTable} rows.
                */
                create or replace function ${rawSubTableSchemaPrefix}new_${rawSubTableShortName}_grants_insert_to_${rawSuperTableShortName}_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    ${ifConditionThen}
                        call rbac.grantPermissionToRole(
                            rbac.createPermission(NEW.uuid, 'INSERT', '${rawSubTable}'),
                            ${superRoleRef});
                    ${ifConditionEnd}
                    return NEW;
                end; $$;
                
                -- z_... is to put it at the end of after insert triggers, to make sure the roles exist
                create trigger z_new_${rawSubTableName}_grants_after_insert_tg
                    after insert on ${rawSuperTableWithSchema}
                    for each row
                execute procedure ${rawSubTableSchemaPrefix}new_${rawSubTableShortName}_grants_insert_to_${rawSuperTableShortName}_tf();
                """,
                    with("ifConditionThen", g.getSuperRoleDef().getEntityAlias().isCaseDependent()
                            // TODO.impl: .type needs to be dynamically generated
                            ? "if NEW.type = '" + g.getSuperRoleDef().getEntityAlias().usingCase().value + "' then"
                            : "-- unconditional for all rows in that table"),
                    with("ifConditionEnd", g.getSuperRoleDef().getEntityAlias().isCaseDependent()
                            ? "end if;"
                            : "-- end."),
                    with("superRoleRef", toRoleDescriptor(g.getSuperRoleDef(), NEW.name())),
                    with("rawSuperTableWithSchema", g.getSuperRoleDef().getEntityAlias().getRawTableNameWithSchema()),
                    with("rawSuperTableShortName", g.getSuperRoleDef().getEntityAlias().getRawTableShortName()),
                    with("rawSuperTable", g.getSuperRoleDef().getEntityAlias().getRawTableName()),
                    with("rawSubTable", g.getPermDef().getEntityAlias().getRawTableNameWithSchema()),
                    with("rawSubTableSchemaPrefix", g.getPermDef().getEntityAlias().getRawTableSchemaPrefix()),
                    with("rawSubTableName", g.getPermDef().getEntityAlias().getRawTableName()),
                    with("rawSubTableShortName", g.getPermDef().getEntityAlias().getRawTableShortName()));

        });
    }

    private void generateInsertPermissionTriggerAlwaysDisallow(final StringWriter plPgSql) {
        plPgSql.writeLn("""
            -- ============================================================================
            --changeset InsertTriggerGenerator:${liquibaseTagPrefix}-rbac-ALWAYS-DISALLOW-INSERT endDelimiter:--//
            -- ----------------------------------------------------------------------------
            """,
            with("liquibaseTagPrefix", liquibaseTagPrefix));

        plPgSql.writeLn("""
            /**
                Checks if the user or assumed roles are allowed to insert a row to ${rawSubTable},
                where only global-admin has that permission.
            */
            create or replace function ${rawSubTable}_insert_permission_missing_tf()
                returns trigger
                language plpgsql as $$
            begin
                raise exception '[403] insert into ${rawSubTableWithSchema} values(%) not allowed regardless of current subject, no insert permissions granted at all', NEW;
            end; $$;

            create trigger ${rawSubTable}_insert_permission_check_tg
                before insert on ${rawSubTable}
                for each row
                    execute procedure ${rawSubTableWithSchema}_insert_permission_missing_tf();
            """,
            with("rawSubTableWithSchema", rbacDef.getRootEntityAlias().getRawTableNameWithSchema()),
            with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));

        plPgSql.writeLn("--//");
    }

    private void generateInsertPermissionChecks(final StringWriter plPgSql) {
        generateInsertPermissionsCheckHeader(plPgSql);

        plPgSql.indented(1, () -> {
            getInsertGrants().forEach(g -> {
                generateInsertPermissionChecksForSingleGrant(plPgSql, g);
            });
            plPgSql.chopTail(" or\n");
        });

        generateInsertPermissionsChecksFooter(plPgSql);
    }

    private void generateInsertPermissionsCheckHeader(final StringWriter plPgSql) {
        plPgSql.writeLn("""
            -- ============================================================================
            --changeset InsertTriggerGenerator:${liquibaseTagPrefix}-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
            -- ----------------------------------------------------------------------------

            /**
                Checks if the user respectively the assumed roles are allowed to insert a row to ${rawSubTable}.
            */
            create or replace function ${rawSubTable}_insert_permission_check_tf()
                returns trigger
                language plpgsql as $$
            declare
                superObjectUuid uuid;
            begin
            """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableNameWithSchema()));
        plPgSql.chopEmptyLines();
    }

    private void generateInsertPermissionChecksForSingleGrant(final StringWriter plPgSql, final RbacView.RbacGrantDefinition g) {
        final RbacView.EntityAlias superRoleEntityAlias = g.getSuperRoleDef().getEntityAlias();

        final var caseCondition = g.isConditional()
                ? ("NEW.type in (" + toStringList(g.getForCases()) + ") and ")
                : "";

        if (g.getSuperRoleDef().isGlobal(GUEST)) {
            plPgSql.writeLn(
                    """
                    -- check INSERT permission for rbac.global anyone
                    if ${caseCondition}true then
                        return NEW;
                    end if;
                    """,
                    with("caseCondition", caseCondition));
        } else if (g.getSuperRoleDef().isGlobal(ADMIN)) {
            plPgSql.writeLn(
                    """
                    -- check INSERT permission if rbac.global ADMIN
                    if ${caseCondition}rbac.isGlobalAdmin() then
                        return NEW;
                    end if;
                    """,
                    with("caseCondition", caseCondition));
        } else if (g.getSuperRoleDef().getEntityAlias().isFetchedByDirectForeignKey()) {
                plPgSql.writeLn(
                    """
                    -- check INSERT permission via direct foreign key: NEW.${refColumn}
                    if ${caseCondition}rbac.hasInsertPermission(NEW.${refColumn}, '${rawSubTable}') then
                        return NEW;
                    end if;
                    """,
                    with("caseCondition", caseCondition),
                    with("refColumn", superRoleEntityAlias.dependsOnColumName()),
                    with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableNameWithSchema()));
        } else {
            plPgSql.writeLn(
                """
                -- check INSERT permission via indirect foreign key: NEW.${refColumn}
                superObjectUuid := (${fetchSql});
                assert superObjectUuid is not null, 'object uuid fetched depending on ${rawSubTable}.${refColumn} must not be null, also check fetchSql in RBAC DSL';
                if ${caseCondition}rbac.hasInsertPermission(superObjectUuid, '${rawSubTable}') then
                    return NEW;
                end if;
                """,
                with("caseCondition", caseCondition),
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableNameWithSchema()),
                with("refColumn", superRoleEntityAlias.dependsOnColumName()),
                with("fetchSql", g.getSuperRoleDef().getEntityAlias().fetchSql().sql),
                with("columns", g.getSuperRoleDef().getEntityAlias().aliasName() + ".uuid"),
                with("ref", NEW.name()));
        }
    }

    private void generateInsertPermissionsChecksFooter(final StringWriter plPgSql) {
        plPgSql.writeLn();
        plPgSql.writeLn("""
                raise exception '[403] insert into ${rawSubTableWithSchema} values(%) not allowed for current subjects % (%)',
                        NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
            end; $$;
            
            create trigger ${rawSubTable}_insert_permission_check_tg
                before insert on ${rawSubTableWithSchema}
                for each row
                    execute procedure ${rawSubTableWithSchema}_insert_permission_check_tf();
            --//
            """,
                with("rawSubTableWithSchema", rbacDef.getRootEntityAlias().getRawTableNameWithSchema()),
                with("rawSubTable", rbacDef.getRootEntityAlias().getRawTableName()));
    }

    private String toStringList(final Set<RbacView.CaseDef> cases) {
        return cases.stream().map(c -> "'" + c.value + "'").collect(joining(", "));
    }

    private boolean isGrantToADifferentTable(final RbacView.RbacGrantDefinition g) {
        return !rbacDef.getRootEntityAlias().getRawTableNameWithSchema().equals(g.getSuperRoleDef().getEntityAlias().getRawTableNameWithSchema());
    }

    private Stream<RbacView.RbacGrantDefinition> getInsertGrants() {
        return rbacDef.getGrantDefs().stream()
                .filter(g -> g.grantType() == PERM_TO_ROLE)
                .filter(g -> g.getPermDef().toCreate && g.getPermDef().getPermission() == INSERT);
    }

    private boolean isInsertPermissionIsNotGrantedAtAll() {
        return getInsertGrants().findAny().isEmpty();
    }

    private boolean isInsertPermissionGrantedToGlobalGuest() {
         return getInsertGrants().anyMatch(g ->
                 g.getSuperRoleDef().getEntityAlias().isGlobal() && g.getSuperRoleDef().getRole() == GUEST);
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
            if ( !x.equals(y) ) {
                return x;
                // throw new IllegalStateException("only a single INSERT permission grant allowed");
            }
            return x;
        };
    }

    private static String toVar(final RbacView.RbacRoleDefinition roleDef) {
        return uncapitalize(roleDef.getEntityAlias().simpleName()) + capitalize(roleDef.getRole().name());
    }


    private String toRoleDescriptor(final RbacView.RbacRoleDefinition roleDef, final String ref) {
        final var functionName = toVar(roleDef);
        if (roleDef.getEntityAlias().isGlobal()) {
            return functionName + "()";
        }
        return functionName + "(" + ref + ")";
    }
}
