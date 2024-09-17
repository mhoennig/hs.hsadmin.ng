package net.hostsharing.hsadminng.rbac.generator;

import net.hostsharing.hsadminng.rbac.generator.RbacView.CaseDef;
import net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition;
import net.hostsharing.hsadminng.rbac.generator.RbacView.RbacPermissionDefinition;

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
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition.GrantType.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class RolesGrantsAndPermissionsGenerator {

    private final RbacView rbacDef;
    private final Set<RbacGrantDefinition> rbacGrants = new HashSet<>();
    private final String liquibaseTagPrefix;
    private final String simpleEntityName;
    private final String simpleEntityVarName;
    private final String rawTableName;

    RolesGrantsAndPermissionsGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.rbacGrants.addAll(rbacDef.getGrantDefs().stream()
                .filter(RbacGrantDefinition::isToCreate)
                .collect(toSet()));
        this.liquibaseTagPrefix = liquibaseTagPrefix;

        simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        simpleEntityName = capitalize(simpleEntityVarName);
        rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
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

                create or replace procedure buildRbacSystemFor${simpleEntityName}(
                    NEW ${rawTableName}
                )
                    language plpgsql as $$
                """
                .replace("${simpleEntityName}", simpleEntityName)
                .replace("${rawTableName}", rawTableName));

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
                .map(RbacView.EntityAlias::dependsOnColumName)
                .distinct()
                .map(columnName -> "NEW." + columnName + " is distinct from OLD." + columnName)
                .collect(joining( "\n    or "));
        plPgSql.writeLn("""
                    /*
                        Called from the AFTER UPDATE TRIGGER to re-wire the grants.
                     */
    
                    create or replace procedure updateRbacRulesFor${simpleEntityName}(
                        OLD ${rawTableName},
                        NEW ${rawTableName}
                    )
                        language plpgsql as $$
                    begin
                    
                        if ${updateConditions} then
                            delete from rbac.grants g where g.grantedbytriggerof = OLD.uuid;
                            call buildRbacSystemFor${simpleEntityName}(NEW);
                        end if;
                    end; $$;
                    """,
                with("simpleEntityName", simpleEntityName),
                with("rawTableName", rawTableName),
                with("updateConditions", updateConditions));
    }

    private void generateUpdateTriggerFunction(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                /*
                    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
                 */

                create or replace procedure updateRbacRulesFor${simpleEntityName}(
                    OLD ${rawTableName},
                    NEW ${rawTableName}
                )
                    language plpgsql as $$

                declare
                """
                .replace("${simpleEntityName}", simpleEntityName)
                .replace("${rawTableName}", rawTableName));

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
                .filter(ea -> ea.nullable() == RbacView.Nullable.NULLABLE)
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

    private Stream<RbacView.EntityAlias> referencedEntityAliases() {
        return rbacDef.getEntityAliases().values().stream()
                .filter(ea -> !rbacDef.isRootEntityAlias(ea))
                .filter(ea -> ea.dependsOnColum() != null)
                .filter(ea -> ea.entityClass() != null)
                .filter(ea -> ea.fetchSql() != null);
    }

    private Stream<RbacView.EntityAlias> updatableEntityAliases() {
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
                .map(RbacView.EntityAlias::dependsOnColum)
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
            final RbacView.EntityAlias ea,
            final PostgresTriggerReference old) {
        plPgSql.writeLn(
                ea.fetchSql().sql + "    INTO " + entityRefVar(old, ea) + ";",
                with("columns", ea.aliasName() + ".*"),
                with("ref", old.name()));
        if (ea.nullable() == RbacView.Nullable.NOT_NULL) {
            plPgSql.writeLn(
                    "assert ${entityRefVar}.uuid is not null, format('${entityRefVar} must not be null for ${REF}.${dependsOnColumn} = %s', ${REF}.${dependsOnColumn});",
                    with("entityRefVar", entityRefVar(old, ea)),
                    with("dependsOnColumn", ea.dependsOnColumName()),
                    with("ref", old.name()));
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

    private String refVarName(final PostgresTriggerReference ref, final RbacView.EntityAlias entityAlias) {
        return ref.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }

    private String roleRef(final PostgresTriggerReference rootRefVar, final RbacView.RbacRoleDefinition roleDef) {
        if (roleDef == null) {
            System.out.println("null");
        }
        if (roleDef.getEntityAlias().isGlobal()) {
            return "rbac.globalAdmin()";
        }
        final String entityRefVar = entityRefVar(rootRefVar, roleDef.getEntityAlias());
        return roleDef.getEntityAlias().simpleName() + capitalize(roleDef.getRole().name())
                + "(" + entityRefVar + ")";
    }

    private String entityRefVar(
            final PostgresTriggerReference rootRefVar,
            final RbacView.EntityAlias entityAlias) {
        return rbacDef.isRootEntityAlias(entityAlias)
                ? rootRefVar.name()
                : rootRefVar.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }

    private void createRolesWithGrantsSql(final StringWriter plPgSql, final RbacView.Role role) {

        final var isToCreate = rbacDef.getRoleDefs().stream()
                .filter(roleDef -> rbacDef.isRootEntityAlias(roleDef.getEntityAlias()) && roleDef.getRole() == role)
                .findFirst().map(RbacView.RbacRoleDefinition::isToCreate).orElse(false);
        if (!isToCreate) {
            return;
        }

        plPgSql.writeLn();
        plPgSql.writeLn("perform rbac.defineRoleWithGrants(");
        plPgSql.indented(() -> {
            plPgSql.writeLn("${simpleVarName)${roleSuffix}(NEW),"
                    .replace("${simpleVarName)", simpleEntityVarName)
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

    private void generateUserGrantsForRole(final StringWriter plPgSql, final RbacView.Role role) {
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

    private void generatePermissionsForRole(final StringWriter plPgSql, final RbacView.Role role) {
        final var permissionGrantsForRole = findPermissionsGrantsForRole(rbacDef.getRootEntityAlias(), role);
        if (!permissionGrantsForRole.isEmpty()) {
            final var arrayElements = permissionGrantsForRole.stream()
                    .map(RbacGrantDefinition::getPermDef)
                    .map(RbacPermissionDefinition::getPermission)
                    .map(RbacView.Permission::name)
                    .map(p -> "'" + p + "'")
                    .sorted()
                    .toList();
            plPgSql.indented(() ->
                    plPgSql.writeLn("permissions => array[" + joinArrayElements(arrayElements, 3) + "],\n"));
            rbacGrants.removeAll(permissionGrantsForRole);
        }
    }

    private void generateIncomingSuperRolesForRole(final StringWriter plPgSql, final RbacView.Role role) {
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

    private void generateOutgoingSubRolesForRole(final StringWriter plPgSql, final RbacView.Role role) {
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
            final RbacView.EntityAlias entityAlias,
            final RbacView.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == PERM_TO_ROLE && g.getSuperRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findGrantsToUserForRole(
            final RbacView.EntityAlias entityAlias,
            final RbacView.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == ROLE_TO_USER && g.getSubRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findIncomingSuperRolesForRole(
            final RbacView.EntityAlias entityAlias,
            final RbacView.Role role) {
        final var roleDef = rbacDef.findRbacRole(entityAlias, role);
        return rbacGrants.stream()
                .filter(g -> g.grantType() == ROLE_TO_ROLE && g.getSubRoleDef() == roleDef)
                .collect(toSet());
    }

    private Set<RbacGrantDefinition> findOutgoingSuperRolesForRole(
            final RbacView.EntityAlias entityAlias,
            final RbacView.Role role) {
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
                    AFTER INSERT TRIGGER to create the role+grant structure for a new ${rawTableName} row.
                 */

                create or replace function insertTriggerFor${simpleEntityName}_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    call buildRbacSystemFor${simpleEntityName}(NEW);
                    return NEW;
                end; $$;

                create trigger insertTriggerFor${simpleEntityName}_tg
                    after insert on ${rawTableName}
                    for each row
                execute procedure insertTriggerFor${simpleEntityName}_tf();
                """
                .replace("${simpleEntityName}", simpleEntityName)
                .replace("${rawTableName}", rawTableName)
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
                    AFTER INSERT TRIGGER to re-wire the grant structure for a new ${rawTableName} row.
                 */
                                
                create or replace function updateTriggerFor${simpleEntityName}_tf()
                    returns trigger
                    language plpgsql
                    strict as $$
                begin
                    call updateRbacRulesFor${simpleEntityName}(OLD, NEW);
                    return NEW;
                end; $$;
                                
                create trigger updateTriggerFor${simpleEntityName}_tg
                    after update on ${rawTableName}
                    for each row
                execute procedure updateTriggerFor${simpleEntityName}_tf();
                """
                .replace("${simpleEntityName}", simpleEntityName)
                .replace("${rawTableName}", rawTableName)
        );

        generateFooter(plPgSql);
    }

    private static void generateFooter(final StringWriter plPgSql) {
        plPgSql.writeLn("--//");
        plPgSql.writeLn();
    }

    private String toPlPgSqlReference(final RbacView.RbacSubjectReference userRef) {
        return switch (userRef.role) {
            case CREATOR -> "rbac.currentSubjectUuid()";
            default -> throw new IllegalArgumentException("unknown user role: " + userRef);
        };
    }

    private String toPlPgSqlReference(
            final PostgresTriggerReference triggerRef,
            final RbacView.RbacRoleDefinition roleDef,
            final boolean assumed) {
        final var assumedArg = assumed ? "" : ", rbac.unassumed()";
        return toRoleRef(roleDef) +
                (roleDef.getEntityAlias().isGlobal() ? ( assumed ? "()" : "(rbac.unassumed())")
                        : rbacDef.isRootEntityAlias(roleDef.getEntityAlias()) ? ("(" + triggerRef.name() + ")")
                        : "(" + toTriggerReference(triggerRef, roleDef.getEntityAlias()) + assumedArg + ")");
    }

    private static String toRoleRef(final RbacView.RbacRoleDefinition roleDef) {
        return uncapitalize(roleDef.getEntityAlias().simpleName()) + capitalize(roleDef.getRole().name());
    }

    private static String toTriggerReference(
            final PostgresTriggerReference triggerRef,
            final RbacView.EntityAlias entityAlias) {
        return triggerRef.name().toLowerCase() + capitalize(entityAlias.aliasName());
    }
}
