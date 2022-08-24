package net.hostsharing.hsadminng.context;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.springframework.transaction.annotation.Propagation.*;

@Service
public class Context {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = MANDATORY)
    public void setCurrentTask(final String task) {
        em.createNativeQuery(
                String.format(
                        "set local hsadminng.currentTask = '%s';",
                        task
                )
        ).executeUpdate();
    }

    @Transactional(propagation = MANDATORY)
    public void setCurrentUser(final String userName) {
        em.createNativeQuery(
            String.format(
                "set local hsadminng.currentUser = '%s';",
                userName
            )
        ).executeUpdate();
        assumeNoSpecialRole();
    }

    public String getCurrentUser() {
        return String.valueOf(em.createNativeQuery("select currentUser()").getSingleResult());
    }

    @Transactional(propagation = MANDATORY)
    public void assumeRoles(final String roles) {
        em.createNativeQuery(
            String.format(
                "set local hsadminng.assumedRoles = '%s';",
                roles
            )
        ).executeUpdate();
    }

    @Transactional(propagation = MANDATORY)
    public void assumeNoSpecialRole() {
        em.createNativeQuery(
            "set local hsadminng.assumedRoles = '';"
        ).executeUpdate();
    }

    public String[] getAssumedRoles() {
        return (String[]) em.createNativeQuery("select assumedRoles()").getSingleResult();
    }

}
