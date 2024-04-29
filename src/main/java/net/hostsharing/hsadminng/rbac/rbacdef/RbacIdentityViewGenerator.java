package net.hostsharing.hsadminng.rbac.rbacdef;

import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;

public class RbacIdentityViewGenerator {
    private final RbacView rbacDef;
    private final String liquibaseTagPrefix;
    private final String simpleEntityVarName;
    private final String rawTableName;

    public RbacIdentityViewGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableName();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset ${liquibaseTagPrefix}-rbac-IDENTITY-VIEW:1 endDelimiter:--//
                -- ----------------------------------------------------------------------------
                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix));

        plPgSql.writeLn(
            switch (rbacDef.getIdentityViewSqlQuery().part) {
            case SQL_PROJECTION -> """
                    call generateRbacIdentityViewFromProjection('${rawTableName}', 
                        $idName$
                    ${identityViewSqlPart}
                        $idName$);
                    """;
            case SQL_QUERY -> """
                call generateRbacIdentityViewFromQuery('${rawTableName}', 
                    $idName$
                ${identityViewSqlPart}
                    $idName$);
                """;
            default -> throw new IllegalStateException("illegal SQL part given");
            },
            with("identityViewSqlPart", StringWriter.indented(2, rbacDef.getIdentityViewSqlQuery().sql)),
            with("rawTableName", rawTableName));

        plPgSql.writeLn("--//");
        plPgSql.writeLn();
    }
}
