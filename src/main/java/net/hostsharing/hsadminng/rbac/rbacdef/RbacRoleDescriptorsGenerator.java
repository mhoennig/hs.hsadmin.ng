package net.hostsharing.hsadminng.rbac.rbacdef;

import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;

public class RbacRoleDescriptorsGenerator {

    private final String liquibaseTagPrefix;
    private final String simpleEntityVarName;
    private final String rawTableName;

    public RbacRoleDescriptorsGenerator(final RbacView rbacDef, final String liquibaseTagPrefix) {
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.simpleEntityVarName = rbacDef.getRootEntityAlias().simpleName();
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableName();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset ${liquibaseTagPrefix}-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
                -- ----------------------------------------------------------------------------
                call generateRbacRoleDescriptors('${simpleEntityVarName}', '${rawTableName}');
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("simpleEntityVarName", simpleEntityVarName),
                with("rawTableName", rawTableName));
    }
}
