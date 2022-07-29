package net.hostsharing.hsadminng;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Controller
public class TestController {

    @PersistenceContext
    private EntityManager em;

    @ResponseBody
    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping() {
        return "pong\n";
    }

    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/currentUser", method = RequestMethod.GET)
    public String currentUser() {
        em.createNativeQuery("SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';").executeUpdate();
        em.createNativeQuery("SET LOCAL hsadminng.assumedRoles = '';").executeUpdate();
        final var query = em.createNativeQuery("select currentUser()");
        return query.getSingleResult() + "\n";
    }
}
