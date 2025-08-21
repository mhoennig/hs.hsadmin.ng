package net.hostsharing.hsadminng.rbac.context;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacContextApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacContextResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacRoleResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "casTicket")
public class RbacContextController implements RbacContextApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Autowired
    private RbacRoleRepository roleRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.current.api.getContext")
    public ResponseEntity<RbacContextResource> getContext(final String roleNamesToAssume) {

        // fetch subject data before assuming any roles; otherwise we might have no SELECT permission anymore
        context.define();
        final var currentSubjectUuid = context.fetchCurrentSubjectUuid();
        final var currentSubject = rbacSubjectRepo.findByUuid(currentSubjectUuid);
        if (currentSubject == null) {
            return ResponseEntity.notFound().build();
        }
        final boolean isGlobalAdmin = context.isGlobalAdmin();

        // now we can assume the roles
        context.assumeRoles(roleNamesToAssume);
        final var assumedRoles = roleRepo.fetchAssumedRoles();

        // finally, return the result
        final var result = rbacContextResponse(currentSubjectUuid, currentSubject, assumedRoles, isGlobalAdmin);
        return ResponseEntity.ok(result);
    }

    private RbacContextResource rbacContextResponse(
            final UUID currentSubjectUuid,
            final RbacSubjectEntity currentSubject,
            final List<RbacRoleEntity> assumedRoles,
            final boolean isGlobalAdmin) {
        final var result = new RbacContextResource();
        final var currentSubjectResource = new RbacSubjectResource();
        currentSubjectResource.setUuid(currentSubjectUuid);
        currentSubjectResource.setName(currentSubject.getName());
        result.setSubject(currentSubjectResource);
        result.setGlobalAdmin(isGlobalAdmin);
        final var assumedRolesResource = mapper.mapList(assumedRoles, RbacRoleResource.class);
        result.setAssumedRoles(assumedRolesResource);
        return result;
    }
}
