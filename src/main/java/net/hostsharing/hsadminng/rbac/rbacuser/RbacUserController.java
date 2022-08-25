package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacusersApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacGrantResource;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserPermissionResource;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacUserResource;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class RbacUserController implements RbacusersApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @Override
    @Transactional
    public ResponseEntity<RbacUserResource> createUser(
            final RbacUserResource body
    ) {
        context.setCurrentTask("creating new user: " + body.getName());
        context.setCurrentUser(body.getName());

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
    @Transactional(readOnly = true)
    public ResponseEntity<RbacUserResource> getUserById(
            final String currentUser,
            final String assumedRoles,
            final UUID userUuid) {

        context.setCurrentUser(currentUser);
        if (!StringUtils.isBlank(assumedRoles)) {
            context.assumeRoles(assumedRoles);
        }

        final var result = rbacUserRepository.findByUuid(userUuid);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result, RbacUserResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserResource>> listUsers(
            final String currentUserName,
            final String assumedRoles,
            final String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (!StringUtils.isBlank(assumedRoles)) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findByOptionalNameLike(userName), RbacUserResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacUserPermissionResource>> listUserPermissions(
            final String currentUserName,
            final String assumedRoles,
            final UUID userUuid
    ) {
        context.setCurrentUser(currentUserName);
        if (!StringUtils.isBlank(assumedRoles)) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacUserRepository.findPermissionsOfUserByUuid(userUuid), RbacUserPermissionResource.class));
    }
}
