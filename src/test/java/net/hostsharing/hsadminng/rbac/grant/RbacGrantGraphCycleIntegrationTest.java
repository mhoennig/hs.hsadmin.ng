package net.hostsharing.hsadminng.rbac.grant;

import lombok.val;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the grant-graph reachability functions terminate even on a cyclic grant graph,
 * where their former per-path (`union all`) traversal recursed until statement-timeout or disk exhaustion.
 */
@RbacDataJpaTest
@Tag("generalIntegrationTest")
class RbacGrantGraphCycleIntegrationTest extends ContextBasedTest {

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    private UUID roleA;
    private UUID roleB;

    @BeforeEach
    void givenACyclicGrantGraph() {
        context("hsh-alex_superuser", null);
        roleA = rbacRoleRepository.findByRoleIdName("rbactest.customer#xxx:ADMIN").getFirst().getUuid();
        roleB = rbacRoleRepository.findByRoleIdName("rbactest.customer#yyy:ADMIN").getFirst().getUuid();

        // a cycle cannot be created via the RBAC API, thus emulating grant-data corruption directly
        em.createNativeQuery("""
                        insert into rbac.grant (ascendantuuid, descendantuuid, assumed)
                            values (:roleA, :roleB, true), (:roleB, :roleA, true)
                        """)
                .setParameter("roleA", roleA)
                .setParameter("roleB", roleB)
                .executeUpdate();

        // without this limit, a regression would recurse until the test-container's disk is full
        em.createNativeQuery("set local statement_timeout = '3s'").executeUpdate();
    }

    @Test
    public void isGrantedTerminatesAndAnswersOnACyclicGrantGraph() {
        val granted = (Boolean) em.createNativeQuery(
                        "select rbac.isGranted(array[cast(:granteeId as uuid)], :grantedId)")
                .setParameter("granteeId", roleB)
                .setParameter("grantedId", roleA)
                .getSingleResult();
        val notGranted = (Boolean) em.createNativeQuery(
                        "select rbac.isGranted(array[cast(:granteeId as uuid)], :grantedId)")
                .setParameter("granteeId", UUID.randomUUID())
                .setParameter("grantedId", roleA)
                .getSingleResult();

        assertThat(granted).isTrue();
        assertThat(notGranted).isFalse();
    }

    @Test
    public void findGranteesTerminatesAndContainsTheCyclePeerOnACyclicGrantGraph() {
        val containsPeer = (Boolean) em.createNativeQuery(
                        "select exists (select 1 from rbac.findGrantees(:grantedId) g where g.uuid = :peerId)")
                .setParameter("grantedId", roleA)
                .setParameter("peerId", roleB)
                .getSingleResult();

        assertThat(containsPeer).isTrue();
    }

    @Test
    public void queryPermissionsGrantedToSubjectIdTerminatesOnACyclicGrantGraph() {
        val permissionCount = ((Number) em.createNativeQuery(
                        "select count(*) from rbac.queryPermissionsGrantedToSubjectId(:subjectId)")
                .setParameter("subjectId", roleA)
                .getSingleResult()).longValue();

        assertThat(permissionCount).isPositive();
    }

    @Test
    public void queryAllRbacSubjectsWithPermissionsForTerminatesOnACyclicGrantGraph() {
        val containsAdminUser = (Boolean) em.createNativeQuery("""
                        select exists (
                            select 1 from rbac.queryAllRbacSubjectsWithPermissionsFor(:startId) s
                                where s.name = 'tst-customer_admin_xxx')
                        """)
                .setParameter("startId", roleA)
                .getSingleResult();

        assertThat(containsAdminUser).isTrue();
    }
}
