package net.hostsharing.hsadminng.rbac.rbacdef;


import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.indented;
import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;

public class RbacRestrictedViewGenerator {
    private final RbacView rbacDef;
    private final String liquibaseTagPrefix;
    private final String simpleEntityVarName;
    private final String rawTableName;

    public RbacRestrictedViewGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableName();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset ${liquibaseTagPrefix}-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call generateRbacRestrictedView('${rawTableName}',
                    '${orderBy}',
                    $updates$
                ${updates}
                    $updates$);
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("orderBy", rbacDef.getOrderBySqlExpression().sql),
                with("updates", indented(rbacDef.getUpdatableColumns().stream()
                        .map(c -> c + " = new." + c)
                        .collect(joining(",\n")), 2)),
                with("rawTableName", rawTableName));
    }
}
