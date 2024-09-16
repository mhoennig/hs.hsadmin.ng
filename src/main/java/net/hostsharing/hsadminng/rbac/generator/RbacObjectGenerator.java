package net.hostsharing.hsadminng.rbac.generator;

import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;

public class RbacObjectGenerator {

    private final String liquibaseTagPrefix;
    private final String rawTableName;

    public RbacObjectGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacObjectGenerator:${liquibaseTagPrefix}-rbac-OBJECT endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call rbac.generateRelatedRbacObject('${rawTableName}');
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("rawTableName", rawTableName));
    }
}
