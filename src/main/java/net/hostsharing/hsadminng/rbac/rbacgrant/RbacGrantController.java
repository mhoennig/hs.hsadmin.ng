package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacGrantsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGrantResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

@RestController
public class RbacGrantController implements RbacGrantsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private RbacGrantRepository rbacGrantRepository;

    @Autowired
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RbacGrantResource> getGrantById(
            final String currentUser,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeUserUuid) {

        context.define(currentUser, assumedRoles);

        final var id = new RbacGrantId(granteeUserUuid, grantedRoleUuid);
        final var result = rbacGrantRepository.findById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result, RbacGrantResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacGrantResource>> listUserGrants(
            final String currentUser,
            final String assumedRoles) {

        context.define(currentUser, assumedRoles);

        return ResponseEntity.ok(mapper.mapList(rbacGrantRepository.findAll(), RbacGrantResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<RbacGrantResource> grantRoleToUser(
            final String currentUser,
            final String assumedRoles,
            final RbacGrantResource body) {

        context.define(currentUser, assumedRoles);

        final var granted = rbacGrantRepository.save(mapper.map(body, RbacGrantEntity.class));
        em.flush();
        em.refresh(granted);

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac.yaml/grants/{roleUuid}")
                        .buildAndExpand(body.getGrantedRoleUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(granted, RbacGrantResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> revokeRoleFromUser(
            final String currentUser,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeUserUuid) {

        context.define(currentUser, assumedRoles);

        rbacGrantRepository.deleteByRbacGrantId(new RbacGrantId(granteeUserUuid, grantedRoleUuid));

        return ResponseEntity.noContent().build();
    }
}
