package net.hostsharing.hsadminng.rbac.generator;

import lombok.val;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;
import static org.assertj.core.api.Assertions.assertThat;

class RolesGrantsAndPermissionsGeneratorUnitTest {

    // no @Entity annotation, otherwise Hibernate's entity scan of integration tests picks it up
    @Table(schema = "test_schema", name = "test_item_rv")
    static class TestItemRbacEntity implements BaseEntity<TestItemRbacEntity> {

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
     * A grant bound to the only declared case of a switchOnColumn must not be generated
     * unconditionally: without a declared default case, rows of other types exist,
     * which must not receive the grant.
     */
    @Test
    void generatesCaseBoundGrantOfSingleCaseSwitchOnlyForThatCase() {
        val generated = new RbacViewPostgresGenerator(
                rbacViewFor("testItem", TestItemRbacEntity.class)
                        .withIdentityView(RbacSpec.SQL.projection("uuid"))
                        .withRestrictedViewOrderBy(RbacSpec.SQL.expression("uuid"))
                        .withUpdatableColumns("version")
                        .importEntityAlias("parentItem", TestItemRbacEntity.class, usingDefaultCase(),
                                dependsOnColumn("parentItemUuid"), directlyFetchedByDependsOnColumn(), NULLABLE)
                        .createRole(OWNER, (with) -> {
                            with.owningUser(CREATOR);
                            with.permission(DELETE);
                        })
                        .createSubRole(ADMIN, (with) -> with.permission(UPDATE))
                        .switchOnColumn(
                                "type",
                                inCaseOf("FANCY", then -> then.toRole("parentItem", ADMIN).grantRole("testItem", ADMIN))))
                .toString();

        val caseBlockStart = generated.indexOf("IF NEW.type = 'FANCY' THEN");
        assertThat(caseBlockStart).as("case block for the declared case expected").isNotNegative();
        val beforeCaseBlock = generated.substring(0, caseBlockStart);
        val caseBlock = generated.substring(caseBlockStart, generated.indexOf("END IF;", caseBlockStart));
        assertThat(beforeCaseBlock)
                .as("the case-bound grant must not be created for rows of other types")
                .doesNotContain("test_item_ADMIN(newParentItem)");
        assertThat(caseBlock)
                .as("the case-bound grant must be created for rows of the declared case")
                .contains("test_item_ADMIN(newParentItem)");
    }
}
