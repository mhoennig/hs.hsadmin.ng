package net.hostsharing.hsadminng.rbac.generator;

import net.hostsharing.hsadminng.rbac.generator.RbacSpec.CaseDef;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacGrantDefinition;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacPermissionDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static net.hostsharing.hsadminng.rbac.generator.PostgresTriggerReference.NEW;
import static net.hostsharing.hsadminng.rbac.generator.PostgresTriggerReference.OLD;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacGrantDefinition.GrantType.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;
import static org.apache.commons.lang3.StringUtils.capitalize;

class RolesGrantsAndPermissionsGenerator {

    private final RbacSpec rbacDef;
    private final Set<RbacGrantDefinition> rbacGrants = new HashSet<>();
    private final String liquibaseTagPrefix;
    private final String simpleEntityName;
    private final String simpleEntityVarName;
    private final String qualifiedRawTableName;

    RolesGrantsAndPermissionsGenerator(final RbacSpec rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.rbacGrants.addAll(rbacDef.getGrantDefs().stream()
                .filter(RbacGrantDefinition::isToCreate)
                .collect(toSet()));
        this.liquibaseTagPrefix = liquibaseTagPrefix;

        simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        simpleEntityName = capitalize(simpleEntityVarName);
        qualifiedRawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        generateInsertTrigger(plPgSql);
        if (hasAnyUpdatableEntityAliases()) {
            generateUpdateTrigger(plPgSql);
        }
    }

    private void generateHeader(final StringWriter plPgSql, final String triggerType) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RolesGrantsAndPermissionsGenerator:${liquibaseTagPrefix}-rbac-${triggerType}-trigger endDelimiter:--//
                -- ----------------------------------------------------------------------------
                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("triggerType", triggerType));
    }

    private void generateInsertTriggerFunction(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                /*
                    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
                 */

                create or replace procedure ${rawTableQualifiedName}_build_rbac_system(
                    NEW ${rawTableQualifiedName}
                )
                    language plpgsql as $$
                """
                .replace("${rawTableQualifiedName}", qualifiedRawTableName));

        plPgSql.writeLn("declare");
        plPgSql.indented(() -> {
            referencedEntityAliases()
                    .forEach((ea) -> plPgSql.writeLn(entityRefVar(NEW, ea) + " " + ea.getRawTableNameWithSchema() + ";"));
        });

        plPgSql.writeLn();
        plPgSql.writeLn("begin");
        plPgSql.indented(() -> {
            plPgSql.writeLn("call rbac.enterTriggerForObjectUuid(NEW.uuid);");
            plPgSql.writeLn();
            generateCreateRolesAndGrantsAfterInsert(plPgSql);
            plPgSql.ensureSingleEmptyLine();
            plPgSql.writeLn("call rbac.leaveTriggerForObjectUuid(NEW.uuid);");
        });
        plPgSql.writeLn("end; $$;");
        plPgSql.writeLn();
    }


    private void generateSimplifiedUpdateTriggerFunction(final StringWriter plPgSql) {

        final var updateConditions = updatableEntityAliases()
                .map(RbacSpec.EntityAlias::dependsOnColumName)
                .distinct()
                .map(columnName -> "NEW." + columnName + " is distinct from OLD." + columnName)
                .collect(joining( "\n    or "));
        plPgSql.writeLn("""
                    /*
                        Called from the AFTER UPDATE TRIGGER to re-wire the grants.
                     */
    
                    create or replace procedure ${rawTableQualifiedName}_update_rbac_system(
                        OLD ${rawTableQualifiedName},
                        NEW ${rawTableQualifiedName}
                    )
                        language plpgsql as $$
                    begin
                    
                        if ${updateConditions} then
                            delete from rbac.grants g where g.grantedbytriggerof = OLD.uuid;
                            call ${rawTableQualifiedName}_build_rbac_system(NEW);
                        end if;
                    end; $$;
                    """,
                with("simpleEntityName", simpleEntityName),
                with("rawTableQualifiedName", qualifiedRawTableName),
                with("updateConditions", updateConditions));
    }

    private void generateUpdateTriggerFunction(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                /*
                    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
                 */

                create or replace procedure ${rawTableQualifiedName}_update_rbac_system(
                    OLD ${rawTableQualifiedName},
                    NEW ${rawTableQualifiedName}
                )
                    language plpgsql as $$

                declare
                """,
                with("rawTableQualifiedName", qualifiedRawTableName));

        plPgSql.chopEmptyLines();
        plPgSql.indented(() -> {
            referencedEntityAliases()
                    .forEach((ea) -> {
                        plPgSql.writeLn(entityRefVar(OLD, ea) + " " + ea.getRawTableNameWithSchema() + ";");
                        plPgSql.writeLn(entityRefVar(NEW, ea) + " " + ea.getRawTableNameWithSchema() + ";");
                    });
        });

        plPgSql.writeLn();
        plPgSql.writeLn("begin");
        plPgSql.indented(() -> {
            plPgSql.writeLn("call rbac.enterTriggerForObjectUuid(NEW.uuid);");
            plPgSql.writeLn();
            generateUpdateRolesAndGrantsAfterUpdate(plPgSql);
            plPgSql.ensureSingleEmptyLine();
            plPgSql.writeLn("call rbac.leaveTriggerForObjectUuid(NEW.uuid);");
        });
        plPgSql.writeLn("end; $$;");
        plPgSql.writeLn();
    }

    private boolean hasAnyUpdatableEntityAliases() {
        return updatableEntityAliases().anyMatch(e -> true);
    }

    private boolean hasAnyUpdatableAndNullableEntityAliases() {
        return updatableEntityAliases()
                .filter(ea -> ea.nullable() == RbacSpec.Nullable.NULLABLE)
                .anyMatch(e -> true);
    }

    private boolean hasAnyConditionalGrants() {
        return rbacDef.getGrantDefs().stream().anyMatch(RbacGrantDefinition::isConditional);
    }

    private void generateCreateRolesAndGrantsAfterInsert(final StringWriter plPgSql) {
        referencedEntityAliases()
                .forEach((ea) -> {
                    generateFetchedVars(plPgSql, ea, NEW);
                    plPgSql.writeLn();
                });

        createRolesWithGrantsSql(plPgSql, OWNER);
        createRolesWithGrantsSql(plPgSql, ADMIN);
        createRolesWithGrantsSql(plPgSql, AGENT);
        createRolesWithGrantsSql(plPgSql, TENANT);
        createRolesWithGrantsSql(plPgSql, REFERRER);

        generateGrants(plPgSql, ROLE_TO_USER);

        generateGrants(plPgSql, ROLE_TO_ROLE);
        if (!rbacDef.getAllCases().isEmpty()) {
            plPgSql.writeLn();
            final var ifOrElsIf = new AtomicReference<>("IF ");
            rbacDef.getAllCases().forEach(caseDef -> {
                if (caseDef.value != null) {
                    plPgSql.writeLn(ifOrElsIf + "NEW." + rbacDef.getDiscriminatorColumName() + " = '" + caseDef.value + "' THEN");
                } else {
                    plPgSql.writeLn("ELSE");
                }
                plPgSql.indented(() -> {
                    generateGrants(plPgSql, ROLE_TO_ROLE, caseDef);
                });
                ifOrElsIf.set("ELSIF ");
            });
            plPgSql.writeLn("END IF;");
        }

        generateGrants(plPgSql, PERM_TO_ROLE);
    }

    private Stream<RbacSpec.EntityAlias> referencedEntityAliases() {
        return rbacDef.getEntityAliases().values().stream()
                .filter(ea -> !rbacDef.isRootEntityAlias(ea))
                .filter(ea -> ea.dependsOnColum() != null)
                .filter(ea -> ea.entityClass() != null)
                .filter(ea -> ea.fetchSql() != null);
    }

    private Stream<RbacSpec.EntityAlias> updatableEntityAliases() {
        return referencedEntityAliases()
                .filter(ea -> rbacDef.getUpdatableColumns().contains(ea.dependsOnColum().column));
    }

    private void generateUpdateRolesAndGrantsAfterUpdate(final StringWriter plPgSql) {
        plPgSql.ensureSingleEmptyLine();

        referencedEntityAliases()
                .forEach((ea) -> {
                    generateFetchedVars(plPgSql, ea, OLD);
                    generateFetchedVars(plPgSql, ea, NEW);
                    plPgSql.writeLn();
                });

        updatableEntityAliases()
                .map(RbacSpec.EntityAlias::dependsOnColum)
                .map(c -> c.column)
                .sorted()
                .distinct()
                .forEach(columnName -> {
                    plPgSql.writeLn();
                    plPgSql.writeLn("if NEW." + columnName + " <> OLD." + columnName + " then");
                    plPgSql.indented(() -> {
                        updateGrantsDependingOn(plPgSql, columnName);
                    });
                    plPgSql.writeLn("end if;");
                });
    }

    private void generateFetchedVars(
            final StringWriter plPgSql,
            final RbacSpec.EntityAlias ea,
            final PostgresTriggerReference old) {
        plPgSql.writeLn(
                ea.fetchSql().sql + "    INTO " + entityRefVar(old, ea) + ";",
                with("columns", ea.aliasName() + ".*"),
                with("ref", old.name()));
        if (ea.nullable() == RbacSpec.Nullable.NOT_NULL) {
            plPgSql.writeLn(
                    "assert ${entityRefVar}.uuid is not null, format('${entityRefVar} must not be null for ${REF}.${dependsOnColumn} = %s of ${rawTable}', ${REF}.${dependsOnColumn});",
                    with("entityRefVar", entityRefVar(old, ea)),
                    with("dependsOnColumn", ea.dependsOnColumName()),
                    with("ref", old.name()),
                    with("rawTable", qualifiedRawTableName));
            plPgSql.writeLn();
        }
    }

    private void updateGrantsDependingOn(final StringWriter plPgSql, final String columnName) {
        rbacDef.getGrantDefs().stream()
                .filter(RbacGrantDefinition::isToCreate)
                .filter(g -> g.dependsOnColumn(columnName))
                .filter(g -> !isInsertPermissionGrant(g))
                .forEach(g -> {
                    plPgSql.ensureSingleEmptyLine();
                    plPgSql.writeLn(generateRevoke(g));
                    plPgSql.writeLn(generateGrant(g));
                    plPgSql.writeLn();
                });
    }

    private static Boolean isInsertPermissionGrant(final RbacGrantDefinition g) {
        final var isInsertPermissionGrant = ofNullable(g.getPermDef()).map(RbacPermissionDefinition::getPermission).map(p -> p == INSERT).orElse(false);
        return isInsertPermissionGrant;
    }

    private void generateGrants(final StringWriter plPgSql, final RbacGrantDefinition.GrantType grantType, final CaseDef caseDef) {
        rbacGrants.stream()
                .filter(g -> g.matchesCase(caseDef))
                .filter(g -> g.grantType() == grantType)
                .map(this::generateGrant)
                .sorted()
                .forEach(text -> plPgSql.writeLn(text, with("ref", NEW.name())));
    }

    private void generateGrants(final StringWriter plPgSql, final RbacGrantDefinition.GrantType grantType) {
        plPgSql.ensureSingleEmptyLine();
        rbacGrants.stream()
                .filter(g -> !g.isConditional())
                .filter(g -> g.grantType() == grantType)
                .map(this::generateGrant)
                .sorted()
                .forEach(text -> plPgSql.writeLn(text, with("ref", NEW.name())));
    }

    private String generateRevoke(RbacGrantDefinition grantDef) {
        return switch (grantDef.grantType()) {
            case ROLE_TO_USER -> throw new IllegalArgumentException("unexpected grant");
            case ROLE_TO_ROLE -> "call rbac.revokeRoleFromRole(${subRoleRef}, ${superRoleRef});"
                    .replace("${subRoleRef}", roleRef(OLD, grantDef.getSubRoleDef()))
                    .replace("${superRoleRef}", roleRef(OLD, grantDef.getSuperRoleDef()));
            case PERM_TO_ROLE -> "call rbac.revokePermissionFromRole(${permRef}, ${superRoleRef});"
                    .replace("${permRef}", getPerm(OLD, grantDef.getPermDef()))
                    .replace("${superRoleRef}", roleRef(OLD, grantDef.getSuperRoleDef()));
        };
    }

    private String generateGrant(RbacGrantDefinition grantDef) {
        final var grantSql = switch (grantDef.grantType()) {
            case ROLE_TO_USER -> throw new IllegalArgumentException("unexpected grant");
            case ROLE_TO_ROLE -> "call rbac.grantRoleToRole(${subRoleRef}, ${superRoleRef}${assumed});"
                    .replace("${assumed}", grantDef.isAssumed() ? "" : ", rbac.unassumed()")
                    .replace("${subRoleRef}", roleRef(NEW, grantDef.getSubRoleDef()))
                    .replace("${superRoleRef}", roleRef(NEW, grantDef.getSuperRoleDef()));
            case PERM_TO_ROLE ->
                    grantDef.getPermDef().getPermission() == INSERT ? ""
                    : "call rbac.grantPermissionToRole(${permRef}, ${superRoleRef});"
                        .replace("${permRef}", createPerm(NEW, grantDef.getPermDef()))
                        .replace("${superRoleRef}", roleRef(NEW, grantDef.getSuperRoleDef()));
        };
        return grantSql;
    }

    private String findPerm(final PostgresTriggerReference ref, final RbacPermissionDefinition permDef) {
        return permRef("rbac.findPermissionId", ref, permDef);
    }

    private String getPerm(final PostgresTriggerReference ref, final RbacPermissionDefinition permDef) {
        return permRef("rbac.getPermissionId", ref, permDef);
    }

    private String createPerm(final PostgresTriggerReference ref, final RbacPermissionDefinition permDef) {
        return permRef("rbac.createPermission", ref, permDef);
    }

    private String permRef(final String functionName, final PostgresTriggerReference ref, final RbacPermissionDefinition permDef) {
        return "${prefix}(${entityRef}.uuid, '${perm}')"
                .replace("${prefix}", functionName)
                .replace("${entityRef}", rbacDef.isRootEntityAlias(permDef.entityAlias)
                        ? ref.name()
                        : refVarName(ref, permDef.entityAlias))
                .replace("${perm}", permDef.permission.name());
    }

    private String refVarName(final PostgresTriggerReference ref, final RbacSpec.EntityAlias entityAlias) {
        return ref.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }

    private String roleRef(final PostgresTriggerReference rootRefVar, final RbacSpec.RbacRoleDefinition roleDef) {
        if (roleDef == null) {
            System.out.println("null");
        }
        if (roleDef.getEntityAlias().isGlobal()) {
            return "rbac.global_ADMIN()";
        }
        final String entityRefVar = entityRefVar(rootRefVar, roleDef.getEntityAlias());
        return roleDef.descriptorFunctionName() + "(" + entityRefVar + ")";
    }

    private String entityRefVar(
            final PostgresTriggerReference rootRefVar,
            final RbacSpec.EntityAlias entityAlias) {
        return rbacDef.isRootEntityAlias(entityAlias)
                ? rootRefVar.name()
                : rootRefVar.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }

    private void createRolesWithGrantsSql(final StringWriter plPgSql, final RbacSpec.Role role) {

        final var isToCreate = rbacDef.getRoleDefs().stream()
                .filter(roleDef -> rbacDef.isRootEntityAlias(roleDef.getEntityAlias()) && roleDef.getRole() == role)
                .findFirst().map(RbacSpec.RbacRoleDefinition::isToCreate).orElse(false);
        if (!isToCreate) {
            return;
        }

        plPgSql.writeLn();
        plPgSql.writeLn("perform rbac.defineRoleWithGrants(");
        plPgSql.indented(() -> {
            plPgSql.writeLn("${qualifiedRawTableName)_${roleSuffix}(NEW),"
                    .replace("${qualifiedRawTableName)", qualifiedRawTableName)
                    .replace("${roleSuffix}", capitalize(role.name())));

            generatePermissionsForRole(plPgSql, role);
            generateIncomingSuperRolesForRole(plPgSql, role);
            generateOutgoingSubRolesForRole(plPgSql, role);
            generateUserGrantsForRole(plPgSql, role);

            plPgSql.chopTail(",\n");
            plPgSql.writeLn();
        });

        plPgSql.writeLn(");");
    }

    private void generateUserGrantsForRole(final StringWriter plPgSql, final RbacSpec.Role role) {
        final var grantsToUsers = findGrantsToUserForRole(rbacDef.getRootEntityAlias(), role);
        if (!grantsToUsers.isEmpty()) {
            final var arrayElements = grantsToUsers.stream()
                    .map(RbacGrantDefinition::getUserDef)
                    .map(this::toPlPgSqlReference)
                    .toList();
            plPgSql.indented(() ->
                    plPgSql.writeLn("subjectUuids => array[" + joinArrayElements(arrayElements, 2) + "],\n"));
            rbacGrants.removeAll(grantsToUsers);
        }
    }

    private void generatePermissionsForRole(final StringWriter plPgSql, final RbacSpec.Role role) {
        final var permissionGrantsForRole = findPermissionsGrantsForRole(rbacDef.getRootEntityAlias(), role);
        if (!permissionGrantsForRole.isEmpty()) {
            final var arrayElements = permissionGrantsForRole.stream()
                    .map(RbacGrantDefinition::getPermDef)
                    .map(RbacPermissionDefinition::getPermission)
                    .map(RbacSpec.Permission::name)
                    .map(p -> "'" + p + "'")
                    .sorted()
                    .toList();
            plPgSql.indented(() ->
                    plPgSql.writeLn("permissions => array[" + joinArrayElements(arrayElements, 3) + "],\n"));
            rbacGrants.removeAll(permissionGrantsForRole);
        }
    }

    private void generateIncomingSuperRolesForRole(final StringWriter plPgSql, final RbacSpec.Role role) {
        final var unconditionalIncomingGrants = findIncomingSuperRolesForRole(rbacDef.getRootEntityAlias(), role).stream()
                .filter(g -> !g.isConditional())
                .toList();
        if (!unconditionalIncomingGrants.isEmpty()) {
            final var arrayElements = unconditionalIncomingGrants.stream()
                    .map(g -> toPlPgSqlReference(NEW, g.getSuperRoleDef(), g.isAssumed()))
                    .sorted().toList();
            plPgSql.indented(() ->
                    plPgSql.writeLn("incomingSuperRoles => array[" + joinArrayElements(arrayElements, 1) + "],\n"));
            rbacGrants.removeAll(unconditionalIncomingGrants);
        }
    }

    private void generateOutgoingSubRolesForRole(final StringWriter plPgSql, final RbacSpec.Role role) {
        final var unconditionalOutgoingGrants = findOutgoingSuperRolesForRole(rbacDef.getRootEntityAlias(), role).stream()
                .filter(g -> !g.isConditional())
                .toList();
        if (!unconditionalOutgoingGrants.isEmpty()) {
            final var arrayElements = unconditionalOutgoingGrants.stream()
                    .map(g -> toPlPgSqlReference(NEW, g.getSubRoleDef(), g.isAssumed()))
                    .sorted().toList();
            plPgSql.indented(() ->
                    plPgSql.writeLn("outgoingSubRoles => array[" + joinArrayElements(arrayElements, 1) + "],\n"));
            rbacGrants.removeAll(unconditionalOutgoingGrants);
        }
    }

    private String joinArrayElements(final List<String> arrayElements, final int singleLineLimit) {
        return arrayElements.size() <= singleLineLimit
                ? String.join(", ", arrayElements)
                : arrayElements.stream().collect(joining(",\n\t", "\n\t", ""));
    }

    private Set<RbacGrantDefinition> findPermissionsGrantsForRole(
            final RbacSpec.EntityAlias entityAlias,
            final RbacSpec.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == PERM_TO_ROLE && g.getSuperRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findGrantsToUserForRole(
            final RbacSpec.EntityAlias entityAlias,
            final RbacSpec.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == ROLE_TO_USER && g.getSubRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findIncomingSuperRolesForRole(
            final RbacSpec.EntityAlias entityAlias,
            final RbacSpec.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == ROLE_TO_ROLE && g.getSubRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findOutgoingSuperRolesForRole(
            final RbacSpec.EntityAlias entityAlias,
            final RbacSpec.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == ROLE_TO_ROLE && g.getSuperRoleDef() == roleDef)
                .filter(g -> g.getSubRoleDef().getEntityAlias() != entityAlias)
                .collect(toSet());
    }

    private void generateInsertTrigger(final StringWriter plPgSql) {

        generateHeader(plPgSql, "insert");
        generateInsertTriggerFunction(plPgSql);

        plPgSql.writeLn("""
                /*
                    AFTER INSERT TRIGGER to create the role+grant structure for a new ${rawTableQualifiedName} row.
                 */

                create or replace function ${rawTableQualifiedName}_build_rbac_system_after_insert_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    call ${rawTableQualifiedName}_build_rbac_system(NEW);
                    return NEW;
                end; $$;

                create trigger build_rbac_system_after_insert_tg
                    after insert on ${rawTableQualifiedName}
                    for each row
                execute procedure ${rawTableQualifiedName}_build_rbac_system_after_insert_tf();
                """
                .replace("${schemaPrefix}", schemaPrefix(qualifiedRawTableName))
                .replace("${rawTableQualifiedName}", qualifiedRawTableName)
        );

        generateFooter(plPgSql);
    }

    private void generateUpdateTrigger(final StringWriter plPgSql) {

        generateHeader(plPgSql, "update");
        if ( hasAnyUpdatableAndNullableEntityAliases() || hasAnyConditionalGrants() ) {
            generateSimplifiedUpdateTriggerFunction(plPgSql);
        } else {
            generateUpdateTriggerFunction(plPgSql);
        }

        plPgSql.writeLn("""
                /*
                    AFTER UPDATE TRIGGER to re-wire the grant structure for a new ${rawTableQualifiedName} row.
                 */

                create or replace function ${rawTableQualifiedName}_update_rbac_system_after_update_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    call ${rawTableQualifiedName}_update_rbac_system(OLD, NEW);
                    return NEW;
                end; $$;

                create trigger update_rbac_system_after_update_tg
                    after update on ${rawTableQualifiedName}
                    for each row
                execute procedure ${rawTableQualifiedName}_update_rbac_system_after_update_tf();
                """
                .replace("${rawTableQualifiedName}", qualifiedRawTableName)
        );

        generateFooter(plPgSql);
    }

    private String schemaPrefix(final String qualifiedIdentifier) {
        return qualifiedIdentifier.contains(".")
                ? qualifiedIdentifier.split("\\.")[0] + "."
                : "";
    }

    private static void generateFooter(final StringWriter plPgSql) {
        plPgSql.writeLn("--//");
        plPgSql.writeLn();
    }

    private String toPlPgSqlReference(final RbacSpec.RbacSubjectReference userRef) {
        return switch (userRef.role) {
            case CREATOR -> "rbac.currentSubjectUuid()";
            default -> throw new IllegalArgumentException("unknown user role: " + userRef);
        };
    }

    private String toPlPgSqlReference(
            final PostgresTriggerReference triggerRef,
            final RbacSpec.RbacRoleDefinition roleDef,
            final boolean assumed) {
        final var assumedArg = assumed ? "" : ", rbac.unassumed()";
        return roleDef.descriptorFunctionName() +
                (roleDef.getEntityAlias().isGlobal() ? ( assumed ? "()" : "(rbac.unassumed())")
                        : rbacDef.isRootEntityAlias(roleDef.getEntityAlias()) ? ("(" + triggerRef.name() + ")")
                        : "(" + toTriggerReference(triggerRef, roleDef.getEntityAlias()) + assumedArg + ")");
    }

    private static String toTriggerReference(
            final PostgresTriggerReference triggerRef,
            final RbacSpec.EntityAlias entityAlias) {
        return triggerRef.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }
}
