package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;

@Import(RbacGrantsDiagramService.class)
public abstract class ContextBasedTest {

    @Autowired
    protected Context context;

    @PersistenceContext
    protected EntityManager em; // just to be used in subclasses

    /**
     * To generate a flowchart diagram from the database use something like this in a defined context:

     <pre>
     RbacGrantsDiagramService.writeToFile(
         "title",
         diagramService.allGrantsTocurrentSubject(of(RbacGrantsDiagramService.Include.USERS, RbacGrantsDiagramService.Include.TEST_ENTITIES, RbacGrantsDiagramService.Include.NOT_ASSUMED, RbacGrantsDiagramService.Include.DETAILS, RbacGrantsDiagramService.Include.PERMISSIONS)),
         "filename.md
     );
    </pre>
     */
    @Autowired
    protected RbacGrantsDiagramService diagramService;  // just to be used in subclasses

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

    protected void historicalContext(final Long txId) {
        // set local cannot be used with query parameters
        em.createNativeQuery("""
                set local hsadminng.tx_history_txid to ':txid';
                """.replace(":txid", txId.toString())).executeUpdate();
        em.createNativeQuery("""
                set local hsadminng.tx_history_timestamp to '';
                """).executeUpdate();
    }


    protected void historicalContext(final Timestamp txTimestamp) {
        // set local cannot be used with query parameters
        em.createNativeQuery("""
                set local hsadminng.tx_history_timestamp to ':timestamp';
                """.replace(":timestamp", txTimestamp.toString())).executeUpdate();
        em.createNativeQuery("""
                set local hsadminng.tx_history_txid to '';
                """).executeUpdate();
    }

}
