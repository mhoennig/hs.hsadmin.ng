package net.hostsharing.hsadminng.context;

import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantsDiagramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
         diagramService.allGrantsToCurrentUser(of(RbacGrantsDiagramService.Include.USERS, RbacGrantsDiagramService.Include.TEST_ENTITIES, RbacGrantsDiagramService.Include.NOT_ASSUMED, RbacGrantsDiagramService.Include.DETAILS, RbacGrantsDiagramService.Include.PERMISSIONS)),
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

    protected void context(final String currentUser, final String assumedRoles) {
        context.define(test.getDisplayName(), null, currentUser, assumedRoles);
    }

    protected void context(final String currentUser) {
        context(currentUser, null);
    }
}
