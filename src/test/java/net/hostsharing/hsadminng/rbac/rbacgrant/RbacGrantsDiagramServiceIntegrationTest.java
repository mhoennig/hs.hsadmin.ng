package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantsDiagramService.Include;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;

import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class, RbacGrantsDiagramService.class})
class RbacGrantsDiagramServiceIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    RbacGrantsDiagramService grantsMermaidService;

    @MockBean
    HttpServletRequest request;

    @Autowired
    Context context;

    @Autowired
    RbacGrantsDiagramService diagramService;

    TestInfo test;

    @BeforeEach
    void init(TestInfo testInfo) {
        this.test = testInfo;
    }

    protected void context(final String currentUser, final String assumedRoles) {
        context.define(test.getDisplayName(), null, currentUser, assumedRoles);
    }

    protected void context(final String currentUser) {
        context(currentUser, null);
    }

    @Test
    void allGrantsToCurrentUser() {
        context("superuser-alex@hostsharing.net", "test_domain#xxx00-aaaa:OWNER");
        final var graph = grantsMermaidService.allGrantsToCurrentUser(EnumSet.of(Include.TEST_ENTITIES));

        assertThat(graph).isEqualTo("""
                flowchart TB
                       
                role:test_domain#xxx00-aaaa:ADMIN --> role:test_package#xxx00:TENANT
                role:test_domain#xxx00-aaaa:OWNER --> role:test_domain#xxx00-aaaa:ADMIN
                role:test_domain#xxx00-aaaa:OWNER --> role:test_package#xxx00:TENANT
                role:test_package#xxx00:TENANT --> role:test_customer#xxx:TENANT
                """.trim());
    }

    @Test
    void allGrantsToCurrentUserIncludingPermissions() {
        context("superuser-alex@hostsharing.net", "test_domain#xxx00-aaaa:OWNER");
        final var graph = grantsMermaidService.allGrantsToCurrentUser(EnumSet.of(Include.TEST_ENTITIES, Include.PERMISSIONS));

        assertThat(graph).isEqualTo("""
                flowchart TB
                      
                role:test_customer#xxx:TENANT --> perm:test_customer#xxx:SELECT
                role:test_domain#xxx00-aaaa:ADMIN --> perm:test_domain#xxx00-aaaa:SELECT
                role:test_domain#xxx00-aaaa:ADMIN --> role:test_package#xxx00:TENANT
                role:test_domain#xxx00-aaaa:OWNER --> perm:test_domain#xxx00-aaaa:DELETE
                role:test_domain#xxx00-aaaa:OWNER --> perm:test_domain#xxx00-aaaa:UPDATE
                role:test_domain#xxx00-aaaa:OWNER --> role:test_domain#xxx00-aaaa:ADMIN
                role:test_domain#xxx00-aaaa:OWNER --> role:test_package#xxx00:TENANT
                role:test_package#xxx00:TENANT --> perm:test_package#xxx00:SELECT
                role:test_package#xxx00:TENANT --> role:test_customer#xxx:TENANT
                """.trim());
    }

    @Test
    @Disabled // enable to generate from a real database
    void print() throws IOException {
        //context("superuser-alex@hostsharing.net", "hs_office_person#FirbySusan:ADMIN");
        context("superuser-alex@hostsharing.net");

        //final var graph = grantsMermaidService.allGrantsToCurrentUser(EnumSet.of(Include.NON_TEST_ENTITIES, Include.PERMISSIONS));

        final var targetObject = (UUID) em.createNativeQuery("SELECT uuid FROM hs_office_coopassetstransaction WHERE reference='ref 1000101-1'").getSingleResult();
        final var graph = grantsMermaidService.allGrantsFrom(targetObject, "view", EnumSet.of(Include.USERS));

        RbacGrantsDiagramService.writeToFile(join(";", context.getAssumedRoles()), graph, "doc/all-grants.md");
    }
}
