package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacUsersApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.Mapper.map;
import static net.hostsharing.hsadminng.mapper.Mapper.mapList;

@RestController
public class RbacUserController implements RbacUsersApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @Override
    @Transactional
    public ResponseEntity<RbacUserResource> createUser(
            final RbacUserResource body
    ) {
        context.define(null);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }
        final var saved = map(body, RbacUserEntity.class);
        rbacUserRepository.create(saved);
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac.yaml/users/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(map(saved, RbacUserResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteUserByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID userUuid
    ) {
        context.define(currentUser, assumedRoles);

        rbacUserRepository.deleteByUuid(userUuid);

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RbacUserResource> getUserById(
            final String currentUser,
            final String assumedRoles,
            final UUID userUuid) {

        context.define(currentUser, assumedRoles);

        final var result = rbacUserRepository.findByUuid(userUuid);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result, RbacUserResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserResource>> listUsers(
            final String currentUser,
            final String assumedRoles,
            final String userName
    ) {
        context.define(currentUser, assumedRoles);

        return ResponseEntity.ok(mapList(rbacUserRepository.findByOptionalNameLike(userName), RbacUserResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserPermissionResource>> listUserPermissions(
            final String currentUser,
            final String assumedRoles,
            final UUID userUuid
    ) {
        context.define(currentUser, assumedRoles);

        return ResponseEntity.ok(mapList(
                rbacUserRepository.findPermissionsOfUserByUuid(userUuid),
                RbacUserPermissionResource.class));
    }
}
