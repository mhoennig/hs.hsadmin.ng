package net.hostsharing.hsadminng.rbac.generator;

import lombok.val;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;
import static org.assertj.core.api.Assertions.assertThat;

class InsertTriggerGeneratorUnitTest {

    // no @Entity annotation, otherwise Hibernate's entity scan of integration tests picks it up
    @Table(schema = "test_schema", name = "test_asset_rv")
    static class TestAssetRbacEntity implements BaseEntity<TestAssetRbacEntity> {

        @Version
        private int version;

        @Override
        public UUID getUuid() {
            return null;
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    /**
     * Two INSERT-grants to the same role of the same super-table, imported via different aliases,
     * must not end up in colliding trigger/function names, which would silently overwrite each other.
     */
    @Test
    void generatesDistinctNamesForInsertGrantsToSameRoleOfSameSuperTable() {
        val generated = generateTestAssetSql();

        val grantFunctionNames = Pattern.compile("create or replace function ([\\w.]+_grants_insert_to_\\w+_tf)")
                .matcher(generated).results()
                .map(m -> m.group(1))
                .toList();
        assertThat(grantFunctionNames).hasSize(2).doesNotHaveDuplicates();
    }

    /**
     * PostgreSQL fires after-insert triggers in name order, so the granting triggers must sort
     * behind the role-building trigger of the same table; this guards the shared trigger name
     * against renames which would silently break that ordering.
     */
    @Test
    void grantTriggersFireAfterTheRoleBuildingTriggerOfTheSameTable() {
        val generated = generateTestAssetSql();

        val roleBuildingTriggerName = Pattern
                .compile("create or replace trigger (\\w+)\\s+after insert on test_schema\\.test_asset\\b")
                .matcher(generated).results()
                .map(m -> m.group(1))
                .reduce(Reducer::toSingleElement)
                .orElseThrow();
        val grantTriggerNames = Pattern
                .compile("create trigger (\\w+)\\s+after insert on test_schema\\.test_asset\\b")
                .matcher(generated).results()
                .map(m -> m.group(1))
                .toList();
        assertThat(grantTriggerNames).isNotEmpty().allSatisfy(
                grantTriggerName -> assertThat(grantTriggerName).isGreaterThan(roleBuildingTriggerName));
    }

    private static String generateTestAssetSql() {
        return new RbacViewPostgresGenerator(
                rbacViewFor("testAsset", TestAssetRbacEntity.class)
                        .withIdentityView(RbacSpec.SQL.projection("uuid"))
                        .withRestrictedViewOrderBy(RbacSpec.SQL.expression("uuid"))
                        .withUpdatableColumns("version")
                        .importEntityAlias("parentAsset", TestAssetRbacEntity.class, usingDefaultCase(),
                                dependsOnColumn("parentAssetUuid"), directlyFetchedByDependsOnColumn(), NULLABLE)
                        .toRole("parentAsset", ADMIN).grantPermission(INSERT)
                        .importEntityAlias("assignedToAsset", TestAssetRbacEntity.class, usingDefaultCase(),
                                dependsOnColumn("assignedToAssetUuid"), directlyFetchedByDependsOnColumn(), NULLABLE)
                        .toRole("assignedToAsset", ADMIN).grantPermission(INSERT)
                        .createRole(OWNER, (with) -> {
                            with.owningUser(CREATOR);
                            with.permission(DELETE);
                        })
                        .createSubRole(ADMIN, (with) -> with.permission(UPDATE)))
                .toString();
    }
}
