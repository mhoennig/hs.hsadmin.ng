package net.hostsharing.hsadminng.rbac.generator;

import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;

public class RbacIdentityViewGenerator {
    private final RbacSpec rbacDef;
    private final String liquibaseTagPrefix;
    private final String simpleEntityVarName;
    private final String rawTableName;

    public RbacIdentityViewGenerator(final RbacSpec rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacIdentityViewGenerator:${liquibaseTagPrefix}-rbac-IDENTITY-VIEW endDelimiter:--//
                -- ----------------------------------------------------------------------------
                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix));

        plPgSql.writeLn(
            switch (rbacDef.getIdentityViewSqlQuery().part) {
            case SQL_PROJECTION -> """
                    call rbac.generateRbacIdentityViewFromProjection('${rawTableName}', 
                        $idName$
                    ${identityViewSqlPart}
                        $idName$);
                    """;
            case SQL_QUERY -> """
                call rbac.generateRbacIdentityViewFromQuery('${rawTableName}', 
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
