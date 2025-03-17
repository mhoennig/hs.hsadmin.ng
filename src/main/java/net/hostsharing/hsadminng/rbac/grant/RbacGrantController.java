package net.hostsharing.hsadminng.rbac.grant;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacGrantsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGrantResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "casTicket")
public class RbacGrantController implements RbacGrantsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private RbacGrantRepository rbacGrantRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.grants.api.getListOfGrantsByUuid")
    public ResponseEntity<RbacGrantResource> getListOfGrantsByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeSubjectUuid) {

        context.define(currentSubject, assumedRoles);

        final var id = new RbacGrantId(granteeSubjectUuid, grantedRoleUuid);
        final var result = rbacGrantRepository.findById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result, RbacGrantResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.grants.api.getListOfSubjectGrants")
    public ResponseEntity<List<RbacGrantResource>> getListOfSubjectGrants(
            final String currentSubject,
            final String assumedRoles) {

        context.define(currentSubject, assumedRoles);

        return ResponseEntity.ok(mapper.mapList(rbacGrantRepository.findAll(), RbacGrantResource.class));
    }

    @Override
    @Transactional
    @Timed("app.rbac.grants.api.postNewRoleGrantToSubject")
    public ResponseEntity<RbacGrantResource> postNewRoleGrantToSubject(
            final String currentSubject,
            final String assumedRoles,
            final RbacGrantResource body) {

        context.define(currentSubject, assumedRoles);

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
    @Timed("app.rbac.grants.api.deleteRoleGrantFromSubject")
    public ResponseEntity<Void> deleteRoleGrantFromSubject(
            final String currentSubject,
            final String assumedRoles,
            final UUID grantedRoleUuid,
            final UUID granteeSubjectUuid) {

        context.define(currentSubject, assumedRoles);

        rbacGrantRepository.deleteByRbacGrantId(new RbacGrantId(granteeSubjectUuid, grantedRoleUuid));

        return ResponseEntity.noContent().build();
    }

// TODO.feat: implement an endpoint to create a Mermaid flowchart with all grants of a given user
//    @GetMapping(
//            path = "/api/rbac/subjects/{subjectUuid}/grants",
//            produces = {"text/vnd.mermaid"})
//    @Transactional(readOnly = true)
//    public ResponseEntity<String> allGrantsOfUserAsMermaid(
//            @RequestHeader(name = "current-subject") String currentSubject,
//            @RequestHeader(name = "assumed-roles", required = false) String assumedRoles) {
//        final var graph = RbacGrantsDiagramService.allGrantsToUser(currentSubject);
//        return ResponseEntity.ok(graph);
//    }

}
