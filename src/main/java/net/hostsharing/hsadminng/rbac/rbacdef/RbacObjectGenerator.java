package net.hostsharing.hsadminng.rbac.rbacdef;

import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;

public class RbacObjectGenerator {

    private final String liquibaseTagPrefix;
    private final String rawTableName;

    public RbacObjectGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableName();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset ${liquibaseTagPrefix}-rbac-OBJECT:1 endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call generateRelatedRbacObject('${rawTableName}');
                --//
                                
                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("rawTableName", rawTableName));
    }
}
