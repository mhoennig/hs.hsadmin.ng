package net.hostsharing.hsadminng;

import org.hibernate.type.PostgresUUIDType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Controller
public class TestController {

    @PersistenceContext
    private EntityManager em;

    @RequestMapping(value = "/api/test", method = RequestMethod.GET)
    public List<Object> test() {
        final var query = em.createNativeQuery("select * from public.rbacuser")
            .unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("uuidColumn", PostgresUUIDType.INSTANCE);
        return query.getResultList();
    }

    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/currentUser", method = RequestMethod.GET)
    public String currentUser() {
        em.createNativeQuery("SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';").executeUpdate();
        em.createNativeQuery("SET LOCAL hsadminng.assumedRoles = '';").executeUpdate();
        final var query = em.createNativeQuery("select currentUser()");
        return query.getSingleResult().toString();
    }
}
