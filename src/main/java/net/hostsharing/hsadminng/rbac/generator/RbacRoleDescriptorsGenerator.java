package net.hostsharing.hsadminng.rbac.generator;

import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;

public class RbacRoleDescriptorsGenerator {

    private final String liquibaseTagPrefix;
    private final String simpleEntityVarName;
    private final String rawTableName;

    public RbacRoleDescriptorsGenerator(final RbacSpec rbacDef, final String liquibaseTagPrefix) {
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacRoleDescriptorsGenerator:${liquibaseTagPrefix}-rbac-ROLE-DESCRIPTORS endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call rbac.generateRbacRoleDescriptors('${rawTableName}');
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("rawTableName", rawTableName));
    }
}
