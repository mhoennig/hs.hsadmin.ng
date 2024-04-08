package net.hostsharing.hsadminng.rbac.rbacdef;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.hostsharing.hsadminng.rbac.rbacdef.PostgresTriggerReference.NEW;
import static net.hostsharing.hsadminng.rbac.rbacdef.StringWriter.with;

public class RbacViewPostgresGenerator {

    private final RbacView rbacDef;
    private final String liqibaseTagPrefix;
    private final StringWriter plPgSql = new StringWriter();

    public RbacViewPostgresGenerator(final RbacView forRbacDef) {
        rbacDef = forRbacDef;
        liqibaseTagPrefix = rbacDef.getRootEntityAlias().getRawTableName().replace("_", "-");
        plPgSql.writeLn("""
                --liquibase formatted sql
                -- This code generated was by ${generator}, do not amend manually.
                """,
                with("generator", getClass().getSimpleName()),
                with("ref", NEW.name()));

        new RbacObjectGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
        new RbacRoleDescriptorsGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
        new RolesGrantsAndPermissionsGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
        new InsertTriggerGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
        new RbacIdentityViewGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
        new RbacRestrictedViewGenerator(rbacDef, liqibaseTagPrefix).generateTo(plPgSql);
    }

    @Override
    public String toString() {
        return plPgSql.toString()
                .replace("\n\n\n", "\n\n")
                .replace("-- ====", "\n-- ====")
                .replace("\n\n--//", "\n--//");
    }

    @SneakyThrows
    public void generateToChangeLog(final Path outputPath) {
        Files.writeString(
                outputPath,
                toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println(outputPath.toAbsolutePath());
    }
}
