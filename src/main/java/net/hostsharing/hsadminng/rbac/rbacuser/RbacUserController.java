package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacusersApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserPermissionResource;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class RbacUserController implements RbacusersApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManager em;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @Override
    @Transactional
    public ResponseEntity<RbacUserResource> createUser(
            @RequestBody final RbacUserResource body
    ) {
        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }
        final var saved = map(body, RbacUserEntity.class);
        rbacUserRepository.create(saved);
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac-users/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(map(saved, RbacUserResource.class));
    }

    @Override
    public ResponseEntity<List<RbacUserPermissionResource>> getUserById(
            final String currentUser,
            final String assumedRoles,
            final String userName) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserResource>> listUsers(
            @RequestHeader(name = "current-user") final String currentUserName,
            @RequestHeader(name = "assumed-roles", required = false) final String assumedRoles,
            @RequestParam(name = "name", required = false) final String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findByOptionalNameLike(userName), RbacUserResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserPermissionResource>> listUserPermissions(
            @RequestHeader(name = "current-user") final String currentUserName,
            @RequestHeader(name = "assumed-roles", required = false) final String assumedRoles,
            @PathVariable(name = "userName") final String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findPermissionsOfUser(userName), RbacUserPermissionResource.class));
    }
}
