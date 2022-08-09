package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacusersApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserPermissionResource;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;

import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class RbacUserController implements RbacusersApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @Override
    @Transactional
    public ResponseEntity<List<RbacUserResource>> listUsers(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @RequestParam(name="name", required = false) String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findByOptionalNameLike(userName), RbacUserResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<List<RbacUserPermissionResource>> listUserPermissions(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @PathVariable(name= "userName") String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findPermissionsOfUser(userName), RbacUserPermissionResource.class));
    }
}
