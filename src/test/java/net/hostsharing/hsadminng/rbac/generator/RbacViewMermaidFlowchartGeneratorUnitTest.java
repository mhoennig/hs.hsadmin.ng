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

class RbacViewMermaidFlowchartGeneratorUnitTest {

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
     * Without a declared default case, the base diagram correctly omits case-bound grants;
     * they must show up in a case diagram instead of vanishing from all diagrams.
     */
    @Test
    void generatesCaseDiagramsForSingleCaseSwitch() {
        val spec = rbacViewFor("testItem", TestItemRbacEntity.class)
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
                        inCaseOf("FANCY", then -> then.toRole("parentItem", ADMIN).grantRole("testItem", ADMIN)));

        val diagrams = RbacViewMermaidFlowchartGenerator.allByFileName(spec, "test-item-rbac");

        assertThat(diagrams.keySet()).containsExactly("test-item-rbac.md", "test-item-rbac-FANCY.md");
        assertThat(diagrams.get("test-item-rbac.md").toString())
                .as("the base diagram must not show the case-bound grant")
                .doesNotContain("role:parentItem:ADMIN ==> role:testItem:ADMIN");
        assertThat(diagrams.get("test-item-rbac-FANCY.md").toString())
                .as("the case diagram must show the case-bound grant")
                .contains("role:parentItem:ADMIN ==> role:testItem:ADMIN");
    }
}
