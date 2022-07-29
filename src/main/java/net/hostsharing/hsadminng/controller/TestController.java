package net.hostsharing.hsadminng.controller;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private Context context;

    @ResponseBody
    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping() {
        return "pong\n";
    }

    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/currentUser", method = RequestMethod.GET)
    public String currentUser() {
        context.setCurrentUser("mike@hostsharing.net");

        final var query = em.createNativeQuery("select currentUser()");
        return query.getSingleResult() + "\n";
    }
}
