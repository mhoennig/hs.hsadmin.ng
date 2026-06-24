package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacSubjectsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.SubjectTypeResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generated.api.v1.model.SubjectTypeResource.USER;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class RbacSubjectController implements RbacSubjectsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    private RbacSubjectListService rbacSubjectListService;

    @Override
    @Transactional
    @PreAuthorize("permitAll()")
    @Timed("app.rbac.subjects.api.postNewSubject")
    public ResponseEntity<RbacSubjectResource> postNewSubject(
            final RbacSubjectResource body
    ) {
        context.define(null);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }
        if (body.getType() == null) {
            body.setType(USER);
        }
        final var saved = rbacSubjectRepository.create(mapper.map(body, RbacSubjectEntity.class));
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac/subjects/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(saved, RbacSubjectResource.class));
    }

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.deleteSubjectByUuid")
    public ResponseEntity<Void> deleteSubjectByUuid(
            final String assumedRoles,
            final UUID subjectUuid
    ) {
        context.assumeRoles(assumedRoles);

        rbacSubjectRepository.deleteByUuid(subjectUuid);

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.subjects.api.getSingleSubjectByUuid")
    public ResponseEntity<RbacSubjectResource> getSingleSubjectByUuid(
            final String assumedRoles,
            final UUID subjectUuid) {

        context.assumeRoles(assumedRoles);

        final var result = rbacSubjectRepository.findByUuid(subjectUuid);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result, RbacSubjectResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.subjects.api.getListOfSubjects")
    public ResponseEntity<List<RbacSubjectResource>> getListOfSubjects(
            final String assumedRoles,
            final String userName,
            final SubjectTypeResource type
    ) {
        context.assumeRoles(assumedRoles);

        final var subjectType = type != null ? SubjectType.valueOf(type.name()) : null;
        return ResponseEntity.ok(mapper.mapList(
                rbacSubjectListService.findByOptionalNameLikeAndOptionalType(userName, subjectType),
                RbacSubjectResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.subjects.api.getListOfSubjectPermissions")
    public ResponseEntity<List<RbacSubjectPermissionResource>> getListOfSubjectPermissions(
            final String assumedRoles,
            final UUID subjectUuid
    ) {
        context.assumeRoles(assumedRoles);

        return ResponseEntity.ok(mapper.mapList(
                rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid),
                RbacSubjectPermissionResource.class));
    }
}
