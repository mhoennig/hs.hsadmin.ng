package net.hostsharing.hsadminng.context;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Service
public class Context {

    @PersistenceContext
    private EntityManager em;

    @Transactional(Transactional.TxType.MANDATORY)
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

    @Transactional(Transactional.TxType.MANDATORY)
    public void assumeRoles(final String roles) {
        em.createNativeQuery(
            String.format(
                "set local hsadminng.assumedRoles = '%s';",
                roles
            )
        ).executeUpdate();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void assumeNoSpecialRole() {
        em.createNativeQuery(
            "set local hsadminng.assumedRoles = '';"
        ).executeUpdate();
    }

    public String[] getAssumedRoles() {
        return (String[]) em.createNativeQuery("select assumedRoles()").getSingleResult();
    }

}
