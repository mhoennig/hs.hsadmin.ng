package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.Pattern;
import lombok.val;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import net.hostsharing.hsadminng.mapper.StrictBodyConverter;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacSubjectsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.SubjectTypeResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static net.hostsharing.hsadminng.errors.Validate.validate;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
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
    private StrictBodyConverter strictBodyConverter;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    private RealSubjectRepository realSubjectRepository; // visibility bypasses RBAC, suvjects are no objects anyway

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.postNewSubject")
    public ResponseEntity<RbacSubjectResource> postNewSubject(
            final Object body // anyOf in OpenAPI is generated as a Map in an Object, ugly, but it is as it is
    ) {
        context.define();
        if (!context.isGlobalAdmin()) {
            throw new ForbiddenException("only a global-admin may create subjects");
        }

        final var entity = toValidatedSubjectEntity(body);
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }

        final var saved = rbacSubjectRepository.create(entity);
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac/subjects/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(saved, RbacSubjectResource.class));
    }

    // The `RbacSubjectInsert` request body is an `anyOf` which the generator emits as a bare `Object`,
    // thus `@Valid` cannot run; this dispatches on the `type` discriminator to the generated member
    // resources and applies the OpenAPI schema constraints via the `StrictBodyConverter`.
    // The USER default for a missing `type` is implemented here and only here.
    private RbacSubjectEntity toValidatedSubjectEntity(final Object body) {
        if (body instanceof Map<?, ?> properties && GROUP.name().equals(properties.get("type"))) {
            final var resource = strictBodyConverter.convertAndValidate(body, RbacGroupSubjectInsertResource.class,
                    violation -> subjectNameViolationMessage(violation,
                            "rbac.group-subject-name-{0}-does-not-match-required-pattern"));
            return RbacSubjectEntity.builder().uuid(resource.getUuid()).name(resource.getName()).type(GROUP).build();
        }
        final var resource = strictBodyConverter.convertAndValidate(body, RbacUserSubjectInsertResource.class,
                violation -> subjectNameViolationMessage(violation,
                        "rbac.user-subject-name-{0}-does-not-match-required-pattern"));
        return RbacSubjectEntity.builder().uuid(resource.getUuid()).name(resource.getName()).type(USER).build();
    }

    private String subjectNameViolationMessage(final ConstraintViolation<?> violation, final String namePatternMessageKey) {
        return violation.getConstraintDescriptor().getAnnotation() instanceof Pattern
                ? messageTranslator.translate(namePatternMessageKey, violation.getInvalidValue())
                : StrictBodyConverter.defaultViolationMessage(violation);
    }

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.putSubjectByUuid")
    public ResponseEntity<RbacSubjectResource> putSubjectByUuid(
            final UUID subjectUuid,
            final Object body // anyOf in OpenAPI is generated as a Map in an Object, ugly, but it is as it is
    ) {
        context.requireGlobalAdmin("only a global-admin may upsert subjects");

        val incoming = toValidatedSubjectEntity(body);
        if (incoming.getUuid() != null) {
            validate("UUID from URI-path, UUID from body").areEqual(subjectUuid, incoming.getUuid());
        } else {
            incoming.setUuid(subjectUuid); // default to UUID from URI-path
        }

        // single idempotent upsert keyed by uuid; the type is immutable and validated in the DB function
        val created = "created".equals(
                rbacSubjectRepository.upsert(subjectUuid, incoming.getName(), incoming.getType().name()));
        val resource = mapper.map(incoming, RbacSubjectResource.class);
        if (created) {
            val uri = MvcUriComponentsBuilder.fromController(getClass())
                    .path("/api/rbac/subjects/{id}")
                    .buildAndExpand(subjectUuid)
                    .toUri();
            return ResponseEntity.created(uri).body(resource);
        }
        return ResponseEntity.ok(resource);
    }

    @Override
    @Transactional
    @Timed("app.rbac.subjects.api.deleteSubjectByUuid")
    public ResponseEntity<Void> deleteSubjectByUuid(
            final UUID subjectUuid,
            final Boolean purge
    ) {
        context.requireGlobalAdmin("only a global-admin may delete subjects");

        if (TRUE.equals(purge)) {
            // physical delete: removing the rbac.reference row cascades to the subject and, via the
            // rbac.subject delete triggers, to all of its grants
            rbacSubjectRepository.deleteByUuid(subjectUuid);
        } else {
            // default: soft-delete/deactivation, retained for reactivation on a replayed Keycloak event
            rbacSubjectRepository.deactivateByUuid(subjectUuid);
        }

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
