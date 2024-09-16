package net.hostsharing.hsadminng.rbac.generator;


import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.indented;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;

public class RbacRestrictedViewGenerator {
    private final RbacView rbacDef;
    private final String liquibaseTagPrefix;
    private final String rawTableName;

    public RbacRestrictedViewGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacRestrictedViewGenerator:${liquibaseTagPrefix}-rbac-RESTRICTED-VIEW endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call rbac.generateRbacRestrictedView('${rawTableName}',
                    $orderBy$
                ${orderBy}
                    $orderBy$,
                    $updates$
                ${updates}
                    $updates$);
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("orderBy", indented(2, rbacDef.getOrderBySqlExpression().sql)),
                with("updates", indented(2, rbacDef.getUpdatableColumns().stream()
                        .map(c -> c + " = new." + c)
                        .collect(joining(",\n")))),
                with("rawTableName", rawTableName));
    }
}
