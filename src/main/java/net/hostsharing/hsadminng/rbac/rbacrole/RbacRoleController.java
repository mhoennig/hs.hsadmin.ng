package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;

@RestController

public class RbacRoleController {

    @Autowired
    private Context context;

    @Autowired
    private RbacRoleRepository rbacRoleRepository;

    @GetMapping(value = "/api/rbacroles")
    @Transactional
    public Iterable<RbacRoleEntity> listCustomers(
        @RequestHeader(value = "current-user") String userName,
        @RequestHeader(value = "assumed-roles", required = false) String assumedRoles
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return rbacRoleRepository.findAll();
    }

}
