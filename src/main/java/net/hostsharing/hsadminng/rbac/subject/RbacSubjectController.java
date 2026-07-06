package net.hostsharing.hsadminng.rbac.subject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacSubjectsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.SubjectTypeResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.subject.SubjectType.USER;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class RbacSubjectController implements RbacSubjectsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    private RealSubjectRepository realSubjectRepository; // visibility bypasses RBAC, suvjects are no objects anyway

    @Override
    @Transactional
    @PreAuthorize("permitAll()")
    @Timed("app.rbac.subjects.api.postNewSubject")
    public ResponseEntity<RbacSubjectResource> postNewSubject(
            final Object body // anyOf in OpenAPI is generated as a Map in an Object, ugly, but it is as it is
    ) {
        context.define(null);

        final var entity = toSubjectEntity(body);
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        if (entity.getType() == null) {
            entity.setType(USER);
        }

        final var saved = rbacSubjectRepository.create(entity);
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac/subjects/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(saved, RbacSubjectResource.class));
    }

    private RbacSubjectEntity toSubjectEntity(final Object body) {
        try {
            return objectMapper.convertValue(body, RbacSubjectEntity.class);
        } catch (final IllegalArgumentException exc) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
        }
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

        final var result = realSubjectRepository.findVisibleSubjectByUuid(subjectUuid)
                .<Subject<?>>map(subject -> subject);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.map(result.get(), RbacSubjectResource.class));
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
                realSubjectRepository.findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                                userName,
                                subjectType),
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
