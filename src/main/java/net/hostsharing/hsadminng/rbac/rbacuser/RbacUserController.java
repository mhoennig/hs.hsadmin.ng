package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;

@RestController
public class RbacUserController {

    @Autowired
    private Context context;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @GetMapping(value = "/api/rbacuser")
    @Transactional
    public Iterable<RbacUserEntity> listUsers(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @RequestParam(name="name", required = false) String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return rbacUserRepository.findByOptionalNameLike(userName);
    }

    @GetMapping(value = "/api/rbacuser/{userName}/permissions")
    @Transactional
    public Iterable<RbacUserPermission> listUserPermissions(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @PathVariable(name= "userName") String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return rbacUserRepository.findPermissionsOfUser(userName);
    }
}
