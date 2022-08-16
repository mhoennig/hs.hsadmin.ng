package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacgrantsApi;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacrolesApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacGrantResource;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacRoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.List;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController

public class RbacGrantController implements RbacgrantsApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacGrantRepository rbacGrantRepository;

    @Override
    @Transactional
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

}
