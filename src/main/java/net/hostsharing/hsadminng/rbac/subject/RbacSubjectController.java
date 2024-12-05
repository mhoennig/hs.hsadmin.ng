package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacSubjectsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
public class RbacSubjectController implements RbacSubjectsApi {

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepository;

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.postNewSubject")
    public ResponseEntity<RbacSubjectResource> postNewSubject(
            final RbacSubjectResource body
    ) {
        context.define(null);

        if (body.getUuid() == null) {
            body.setUuid(UUID.randomUUID());
        }
        final var saved = mapper.map(body, RbacSubjectEntity.class);
        rbacSubjectRepository.create(saved);
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac.yaml/users/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(saved, RbacSubjectResource.class));
    }

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.deleteSubjectByUuid")
    public ResponseEntity<Void> deleteSubjectByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID subjectUuid
    ) {
        context.define(currentSubject, assumedRoles);

        rbacSubjectRepository.deleteByUuid(subjectUuid);

        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.subjects.api.getSingleSubjectByUuid")
    public ResponseEntity<RbacSubjectResource> getSingleSubjectByUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID subjectUuid) {

        context.define(currentSubject, assumedRoles);

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
            final String currentSubject,
            final String assumedRoles,
            final String userName
    ) {
        context.define(currentSubject, assumedRoles);

        return ResponseEntity.ok(mapper.mapList(rbacSubjectRepository.findByOptionalNameLike(userName), RbacSubjectResource.class));
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.subjects.api.getListOfSubjectPermissions")
    public ResponseEntity<List<RbacSubjectPermissionResource>> getListOfSubjectPermissions(
            final String currentSubject,
            final String assumedRoles,
            final UUID subjectUuid
    ) {
        context.define(currentSubject, assumedRoles);

        return ResponseEntity.ok(mapper.mapList(
                rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid),
                RbacSubjectPermissionResource.class));
    }
}
