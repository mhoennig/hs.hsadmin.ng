package net.hostsharing.hsadminng;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@DataJpaTest
class RbacIntegrationTests {

    @PersistenceContext
    private EntityManager em;

    @Test
    @Transactional
    void currentUser() {
        em.createNativeQuery("SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';").executeUpdate();
        em.createNativeQuery("SET LOCAL hsadminng.assumedRoles = '';").executeUpdate();

        final var result = em.createNativeQuery("select currentUser()").getSingleResult();
        Assertions.assertThat(result).isEqualTo("mike@hostsharing.net");
    }
}
