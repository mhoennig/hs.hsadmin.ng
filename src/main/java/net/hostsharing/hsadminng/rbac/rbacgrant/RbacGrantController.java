package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacgrantsApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacGrantResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController

public class RbacGrantController implements RbacgrantsApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacGrantRepository rbacGrantRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RbacGrantResource> getGrantById(
            final String currentUser,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeUserUuid) {

        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }

        final var id = new RbacGrantId(granteeUserUuid, grantedRoleUuid);
        final var result = rbacGrantRepository.findById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(map(result, RbacGrantResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacGrantResource>> listUserGrants(
            final String currentUser,
            final String assumedRoles) {

        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacGrantRepository.findAll(), RbacGrantResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> grantRoleToUser(
            final String currentUser,
            final String assumedRoles,
            final RbacGrantResource body) {

        context.setCurrentTask("granting role to user");
        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }

        rbacGrantRepository.save(map(body, RbacGrantEntity.class));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac-grants/{roleUuid}")
                        .buildAndExpand(body.getGrantedRoleUuid())
                        .toUri();
        return ResponseEntity.created(uri).build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> revokeRoleFromUser(
            final String currentUser,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeUserUuid) {

        context.setCurrentTask("revoking role from user");
        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }

        rbacGrantRepository.deleteByRbacGrantId(new RbacGrantId(granteeUserUuid, grantedRoleUuid));

        return ResponseEntity.noContent().build();
    }

}
