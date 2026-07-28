package net.hostsharing.hsadminng.rbac.generator;


import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.indented;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.withQuoted;

public class RbacRestrictedViewGenerator {

    // bump to force re-execution of the generated runOnChange changesets,
    // e.g. after structural changes to rbac.generateRbacRestrictedView
    private static final int GENERATOR_VERSION = 2;

    private final RbacSpec rbacDef;
    private final String liquibaseTagPrefix;
    private final String rawTableName;

    public RbacRestrictedViewGenerator(final RbacSpec rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacRestrictedViewGenerator:${liquibaseTagPrefix}-rbac-RESTRICTED-VIEW runOnChange:true endDelimiter:--//
                --validCheckSum: ANY
                -- ----------------------------------------------------------------------------
                -- generator version: ${generatorVersion}
                call rbac.generateRbacRestrictedView('${rawTableName}',
                    ${orderBy},
                    ${updates}
                );
                --//

                """,
                with("generatorVersion", String.valueOf(GENERATOR_VERSION)),
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                withQuoted("orderBy", indented(2, rbacDef.getOrderBySqlExpression().sql)),
                withQuoted("updates",
                        rbacDef.getUpdatableColumns().isEmpty()
                                ? null
                                : indented(2, rbacDef.getUpdatableColumns().stream()
                                        .map(c -> c + " = new." + c)
                                        .collect(joining(",\n")))
                ),
                with("rawTableName", rawTableName));
    }
}
