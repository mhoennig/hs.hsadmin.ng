package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
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

    protected void context(final String currentSubject, final String assumedRoles) {
        context.define(test.getDisplayName(), null, currentSubject, assumedRoles);
    }

    protected void context(final String currentSubject) {
        context(currentSubject, null);
    }

    @Test
    void allGrantsTocurrentSubject() {
        context("superuser-alex@hostsharing.net", "rbactest.domain#xxx00-aaaa:OWNER");
        final var graph = grantsMermaidService.allGrantsTocurrentSubject(EnumSet.of(Include.TEST_ENTITIES));

        assertThat(graph).isEqualTo("""
                flowchart TB
                       
                role:rbactest.domain#xxx00-aaaa:ADMIN --> role:rbactest.package#xxx00:TENANT
                role:rbactest.domain#xxx00-aaaa:OWNER --> role:rbactest.domain#xxx00-aaaa:ADMIN
                role:rbactest.domain#xxx00-aaaa:OWNER --> role:rbactest.package#xxx00:TENANT
                role:rbactest.package#xxx00:TENANT --> role:rbactest.customer#xxx:TENANT
                """.trim());
    }

    @Test
    void allGrantsTocurrentSubjectIncludingPermissions() {
        context("superuser-alex@hostsharing.net", "rbactest.domain#xxx00-aaaa:OWNER");
        final var graph = grantsMermaidService.allGrantsTocurrentSubject(EnumSet.of(Include.TEST_ENTITIES, Include.PERMISSIONS));

        assertThat(graph).isEqualTo("""
                flowchart TB
                      
                role:rbactest.customer#xxx:TENANT --> perm:rbactest.customer#xxx:SELECT
                role:rbactest.domain#xxx00-aaaa:ADMIN --> perm:rbactest.domain#xxx00-aaaa:SELECT
                role:rbactest.domain#xxx00-aaaa:ADMIN --> role:rbactest.package#xxx00:TENANT
                role:rbactest.domain#xxx00-aaaa:OWNER --> perm:rbactest.domain#xxx00-aaaa:DELETE
                role:rbactest.domain#xxx00-aaaa:OWNER --> perm:rbactest.domain#xxx00-aaaa:UPDATE
                role:rbactest.domain#xxx00-aaaa:OWNER --> role:rbactest.domain#xxx00-aaaa:ADMIN
                role:rbactest.domain#xxx00-aaaa:OWNER --> role:rbactest.package#xxx00:TENANT
                role:rbactest.package#xxx00:TENANT --> perm:rbactest.package#xxx00:SELECT
                role:rbactest.package#xxx00:TENANT --> role:rbactest.customer#xxx:TENANT
                """.trim());
    }

    @Test
    @Disabled // enable to generate from a real database
    void print() throws IOException {
        //context("superuser-alex@hostsharing.net", "hs_office.person#FirbySusan:ADMIN");
        context("superuser-alex@hostsharing.net");

        //final var graph = grantsMermaidService.allGrantsTocurrentSubject(EnumSet.of(Include.NON_TEST_ENTITIES, Include.PERMISSIONS));

        final var targetObject = (UUID) em.createNativeQuery("SELECT uuid FROM hs_office.coopassettx WHERE reference='ref 1000101-1'").getSingleResult();
        final var graph = grantsMermaidService.allGrantsFrom(targetObject, "view", EnumSet.of(Include.USERS));

        RbacGrantsDiagramService.writeToFile(join(";", context.fetchAssumedRoles()), graph, "doc/all-grants.md");
    }
}
