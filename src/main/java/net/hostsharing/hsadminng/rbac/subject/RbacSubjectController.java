package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Pattern;
import lombok.val;
import net.hostsharing.hsadminng.config.ApiKey;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import net.hostsharing.hsadminng.mapper.StrictBodyConverter;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacSubjectsApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.ApiKeyScopeResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacApiKeySubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectUpsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectWithOrganizationInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectWithOrganizationUpsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectCreatedResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectPermissionResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectUpsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectWithOrganizationInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectWithOrganizationUpsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.SubjectTypeResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static net.hostsharing.hsadminng.errors.Validate.validate;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.API_KEY;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.USER;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class RbacSubjectController implements RbacSubjectsApi {

    private static final String USER_SUBJECT_NAME_PATTERN_MESSAGE_KEY =
            "rbac.user-subject-name-{0}-does-not-match-required-pattern";
    private static final String GROUP_SUBJECT_NAME_PATTERN_MESSAGE_KEY =
            "rbac.group-subject-name-{0}-does-not-match-required-pattern";
    private static final String API_KEY_SUBJECT_NAME_PATTERN_MESSAGE_KEY =
            "rbac.api-key-subject-name-{0}-does-not-match-required-pattern";

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
    public ResponseEntity<RbacSubjectCreatedResource> postNewSubject(
            final Object body // anyOf in OpenAPI is generated as a Map in an Object, ugly, but it is as it is
    ) {
        context.define();
        if (!context.isGlobalAdmin()) {
            throw new ForbiddenException("only a global-admin may create subjects");
        }

        final var validated = toValidatedSubjectInsert(body);
        final var entity = validated.entity();
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }

        final var saved = rbacSubjectRepository.create(entity);
        final var resource = mapper.map(saved, RbacSubjectCreatedResource.class);
        if (saved.getType() == API_KEY) {
            // the clear-text API-key is only returned once, in this response; just its hash is stored
            final var apiKey = ApiKey.generate(saved.getName());
            rbacSubjectRepository.createApiKey(
                    saved.getUuid(), ApiKey.hash(apiKey), validated.scopeWireNames(), validated.expiresAt());
            resource.setApiKey(apiKey);
            resource.setScopes(validated.scopes());
            resource.setExpiresAt(validated.expiresAt());
        }
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/rbac/subjects/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(resource);
    }

    // the validated insert resource reduced to the entity plus, for API_KEY subjects,
    // the named endpoint-scopes and the optional expiry timestamp
    private record ValidatedSubjectInsert(
            RbacSubjectEntity entity, List<ApiKeyScopeResource> scopes, OffsetDateTime expiresAt) {

        String[] scopeWireNames() {
            return scopes == null
                    ? new String[0]
                    : scopes.stream().map(ApiKeyScopeResource::getValue).toArray(String[]::new);
        }
    }

    // The `RbacSubjectInsert` request body is an `anyOf`, which the generator emits as a bare `Object`
    // where `@Valid` cannot run. Thus, we validate based on the `type` discriminator (a missing `type`
    // at the API level defaults to 'USER') and the presence of an explicit `organization` to the
    // matching generated member resource, whose OpenAPI schema constraints are then applied.
    private ValidatedSubjectInsert toValidatedSubjectInsert(final Object body) {
        val properties = body instanceof Map<?, ?> map ? map : Map.of();
        if (API_KEY.name().equals(properties.get("type"))) {
            return toApiKeySubjectInsert(body);
        }
        val hasExplicitOrganization = properties.containsKey("organization");
        if (GROUP.name().equals(properties.get("type"))) {
            return new ValidatedSubjectInsert(
                    hasExplicitOrganization
                            ? toGroupSubjectEntityWithExplicitOrganization(body)
                            : toGroupSubjectEntityWithDerivedOrganization(body),
                    null, null);
        }
        return new ValidatedSubjectInsert(
                hasExplicitOrganization
                        ? toUserSubjectEntityWithExplicitOrganization(body)
                        : toUserSubjectEntityWithDerivedOrganization(body),
                null, null);
    }

    private ValidatedSubjectInsert toApiKeySubjectInsert(final Object body) {
        val resource = convertAndValidate(
                body, RbacApiKeySubjectInsertResource.class, API_KEY_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        // global API_KEY subjects do not belong to a realm; their organization defaults to the
        // subject name (which cannot contain the '-' realm-prefix delimiter), mirroring the
        // DB-level default trigger
        return new ValidatedSubjectInsert(
                subjectEntity(resource.getUuid(), resource.getName(), Subject.organizationFromName(resource.getName()), API_KEY),
                resource.getScopes(), resource.getExpiresAt());
    }

    private RbacSubjectEntity toUserSubjectEntityWithDerivedOrganization(final Object body) {
        val resource = convertAndValidate(body, RbacUserSubjectInsertResource.class, USER_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return subjectEntity(resource.getUuid(), resource.getName(), Subject.organizationFromName(resource.getName()), USER);
    }

    private RbacSubjectEntity toUserSubjectEntityWithExplicitOrganization(final Object body) {
        val resource = convertAndValidate(
                body, RbacUserSubjectWithOrganizationInsertResource.class, USER_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return subjectEntity(resource.getUuid(), resource.getName(), resource.getOrganization(), USER);
    }

    private RbacSubjectEntity toGroupSubjectEntityWithDerivedOrganization(final Object body) {
        val resource = convertAndValidate(body, RbacGroupSubjectInsertResource.class, GROUP_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return subjectEntity(resource.getUuid(), resource.getName(), Subject.organizationFromName(resource.getName()), GROUP);
    }

    private RbacSubjectEntity toGroupSubjectEntityWithExplicitOrganization(final Object body) {
        // JWTs reference groups just by name (Keycloak default), no UUIDs; thus a GROUP subject's
        // organization must remain derivable from the group-name prefix and is validated against it
        val resource = convertAndValidate(
                body, RbacGroupSubjectWithOrganizationInsertResource.class, GROUP_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        validate("organization, organization derived from the group-name prefix")
                .areEqual(resource.getOrganization(), Subject.organizationFromName(resource.getName()));
        return subjectEntity(resource.getUuid(), resource.getName(), resource.getOrganization(), GROUP);
    }

    // the validated upsert resource reduced to the entity plus the desired activation state
    private record ValidatedSubjectUpsert(RbacSubjectEntity entity, boolean deactivated) {
    }

    // Like `toValidatedSubjectInsert`, but for the `RbacSubjectUpsert` request body of HTTP PUT,
    // which deliberately has no API_KEY variant: API_KEY subjects never stem from Keycloak.
    private ValidatedSubjectUpsert toValidatedSubjectUpsert(final Object body) {
        val properties = body instanceof Map<?, ?> map ? map : Map.of();
        if (API_KEY.name().equals(properties.get("type"))) {
            throw new ValidationException(
                    "subjects of type API_KEY are not subject to synchronization and cannot be created-or-updated"
                            + " via HTTP PUT; create API_KEY subjects via HTTP POST /api/rbac/subjects");
        }
        val hasExplicitOrganization = properties.containsKey("organization");
        if (GROUP.name().equals(properties.get("type"))) {
            return hasExplicitOrganization
                    ? toGroupSubjectUpsertWithExplicitOrganization(body)
                    : toGroupSubjectUpsertWithDerivedOrganization(body);
        }
        return hasExplicitOrganization
                ? toUserSubjectUpsertWithExplicitOrganization(body)
                : toUserSubjectUpsertWithDerivedOrganization(body);
    }

    private ValidatedSubjectUpsert toUserSubjectUpsertWithDerivedOrganization(final Object body) {
        val resource = convertAndValidate(body, RbacUserSubjectUpsertResource.class, USER_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return new ValidatedSubjectUpsert(
                subjectEntity(resource.getUuid(), resource.getName(), Subject.organizationFromName(resource.getName()), USER),
                TRUE.equals(resource.getDeactivated()));
    }

    private ValidatedSubjectUpsert toUserSubjectUpsertWithExplicitOrganization(final Object body) {
        val resource = convertAndValidate(
                body, RbacUserSubjectWithOrganizationUpsertResource.class, USER_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return new ValidatedSubjectUpsert(
                subjectEntity(resource.getUuid(), resource.getName(), resource.getOrganization(), USER),
                TRUE.equals(resource.getDeactivated()));
    }

    private ValidatedSubjectUpsert toGroupSubjectUpsertWithDerivedOrganization(final Object body) {
        val resource = convertAndValidate(body, RbacGroupSubjectUpsertResource.class, GROUP_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        return new ValidatedSubjectUpsert(
                subjectEntity(resource.getUuid(), resource.getName(), Subject.organizationFromName(resource.getName()), GROUP),
                TRUE.equals(resource.getDeactivated()));
    }

    private ValidatedSubjectUpsert toGroupSubjectUpsertWithExplicitOrganization(final Object body) {
        // JWTs reference groups just by name (Keycloak default), no UUIDs; thus a GROUP subject's
        // organization must remain derivable from the group-name prefix and is validated against it
        val resource = convertAndValidate(
                body, RbacGroupSubjectWithOrganizationUpsertResource.class, GROUP_SUBJECT_NAME_PATTERN_MESSAGE_KEY);
        validate("organization, organization derived from the group-name prefix")
                .areEqual(resource.getOrganization(), Subject.organizationFromName(resource.getName()));
        return new ValidatedSubjectUpsert(
                subjectEntity(resource.getUuid(), resource.getName(), resource.getOrganization(), GROUP),
                TRUE.equals(resource.getDeactivated()));
    }

    private <R> R convertAndValidate(final Object body, final Class<R> resourceClass, final String namePatternMessageKey) {
        return strictBodyConverter.convertAndValidate(body, resourceClass,
                violation -> subjectNameViolationMessage(violation, namePatternMessageKey));
    }

    private static RbacSubjectEntity subjectEntity(
            final UUID uuid, final String name, final String organization, final SubjectType type) {
        return RbacSubjectEntity.builder().uuid(uuid).name(name).organization(organization).type(type).build();
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
        context.requireGlobalAdmin("only a global-admin may create or update subjects");

        val validated = toValidatedSubjectUpsert(body);
        val incoming = validated.entity();
        if (incoming.getUuid() != null) {
            validate("UUID from URI-path, UUID from body").areEqual(subjectUuid, incoming.getUuid());
        } else {
            incoming.setUuid(subjectUuid); // default to UUID from URI-path
        }

        // a single call to the DB-level upsert, keyed by uuid; the type is immutable and validated in the DB function
        val created = "created".equals(
                rbacSubjectRepository.upsert(
                        subjectUuid, incoming.getName(), incoming.getOrganization(), incoming.getType().name(),
                        validated.deactivated()));
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
            final String name,
            final SubjectTypeResource type
    ) {
        context.requireGlobalAdmin("only a global-admin may delete subjects");

        // the subject is identified by the UUID alone; the given name+type are a safeguard against
        // deleting the wrong subject and must match; idempotent no-op for an unknown UUID
        realSubjectRepository.findSubjectByUuidIncludingDeactivated(subjectUuid).ifPresent(subject -> {
            validate("name from query-parameter, name of the subject to delete")
                    .areEqual(name, subject.getName());
            validate("type from query-parameter, type of the subject to delete")
                    .areEqual(SubjectType.valueOf(type.name()), subject.getType());

            // physical delete: removing the rbac.reference row cascades to the subject and, via the
            // rbac.subject delete triggers, to all of its grants; for an API_KEY subject the FK cascade
            // also removes the stored key hash, which permanently revokes the API-key
            rbacSubjectRepository.deleteByUuid(subjectUuid);
        });

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
            final String organization,
            final SubjectTypeResource type
    ) {
        context.assumeRoles(assumedRoles);

        final var subjectType = type != null ? SubjectType.valueOf(type.name()) : null;
        return ResponseEntity.ok(mapper.mapList(
                realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(
                                userName,
                                organization,
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
