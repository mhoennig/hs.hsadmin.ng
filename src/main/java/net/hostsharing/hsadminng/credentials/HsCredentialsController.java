package net.hostsharing.hsadminng.credentials;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.credentials.generated.api.v1.api.CredentialsApi;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsInsertResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;

import static java.util.Optional.ofNullable;
import static java.util.Optional.of;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsCredentialsController implements CredentialsApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManagerWrapper em;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private RbacSubjectRepository subjectRepo;

    @Autowired
    private CredentialContextResourceToEntityMapper contextMapper;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private HsOfficePersonRbacRepository rbacPersonRepo;

    @Autowired
    private HsCredentialsRepository credentialsRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.credentials.credentials.getSingleCredentialsByUuid")
    public ResponseEntity<CredentialsResource> getSingleCredentialsByUuid(
            final String assumedRoles,
            final UUID credentialsUuid) {
        context.assumeRoles(assumedRoles);

        final var credentials = credentialsRepo.findByUuid(credentialsUuid);
        if (credentials.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final var result = mapper.map(
                credentials.get(), CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.credentials.credentials.getListOfCredentialsByPersonUuid")
    public ResponseEntity<List<CredentialsResource>> getListOfCredentialsByPersonUuid(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        final var person = rbacPersonRepo.findByUuid(personUuid).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("{0} \"{1}\" not found or not accessible", "personUuid", personUuid)
                )

        );
        final var credentials = credentialsRepo.findByPerson(person);
        final var result = mapper.mapList(
                credentials, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.postNewCredentials")
    public ResponseEntity<CredentialsResource> postNewCredentials(
            final String assumedRoles,
            final CredentialsInsertResource body
    ) {
        context.assumeRoles(assumedRoles);

        // first create and save the subject to get its UUID
        final var newlySavedSubject = createSubject(body.getNickname());

        // afterward, create and save the credentials entity with the subject's UUID
        final var newCredentialsEntity = mapper.map(
                body, HsCredentialsEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        newCredentialsEntity.setSubject(newlySavedSubject);
        em.persist(newCredentialsEntity); // newCredentialsEntity.uuid == newlySavedSubject.uuid => do not use repository!

        // return the new credentials as a resource
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/credentials/credentials/{id}")
                        .buildAndExpand(newCredentialsEntity.getUuid())
                        .toUri();
        final var newCredentialsResource = mapper.map(
                newCredentialsEntity, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(newCredentialsResource);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.deleteCredentialsByUuid")
    public ResponseEntity<Void> deleteCredentialsByUuid(final String assumedRoles, final UUID credentialsUuid) {
        context.assumeRoles(assumedRoles);
        final var credentialsEntity = em.getReference(HsCredentialsEntity.class, credentialsUuid);
        em.remove(credentialsEntity);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.patchCredentials")
    public ResponseEntity<CredentialsResource> patchCredentials(
            final String assumedRoles,
            final UUID credentialsUuid,
            final CredentialsPatchResource body
    ) {
        context.assumeRoles(assumedRoles);

        final var current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        new HsCredentialsEntityPatcher(contextMapper, current).apply(body);

        final var saved = credentialsRepo.save(current);
        final var mapped = mapper.map(
                saved, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    @Override
    @Timed("app.credentials.credentials.credentialsUsed")
    public ResponseEntity<CredentialsResource> credentialsUsed(
            final String assumedRoles,
            final UUID credentialsUuid) {
        context.assumeRoles(assumedRoles);

        final var current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        current.setOnboardingToken(null);
        current.setLastUsed(LocalDateTime.now());

        final var saved = credentialsRepo.save(current);
        final var mapped = mapper.map(saved, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    private RbacSubjectEntity createSubject(final String nickname) {
        final var newRbacSubject = subjectRepo.create(new RbacSubjectEntity(null, nickname));
        if(context.fetchCurrentSubject() == null) {
            context.define("activate newly created self-servie subject", null, nickname, null);
        }
        return subjectRepo.findByUuid(newRbacSubject.getUuid()); // attached to EM
    }

    final BiConsumer<HsCredentialsEntity, CredentialsResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        ofNullable(entity.getLastUsed()).ifPresent(
            dt -> resource.setLastUsed(dt.atOffset(ZoneOffset.UTC)));
        of(entity.getSubject()).ifPresent(
                subject -> resource.setNickname(subject.getName())
        );
        of(entity.getPerson()).ifPresent(
                person -> resource.setPerson(
                        mapper.map(person, HsOfficePersonResource.class)
                )
        );
    };

    final BiConsumer<CredentialsInsertResource, HsCredentialsEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {

        // TODO.impl: we need to make sure that the current subject is OWNER (or ADMIN?) of the person
        final var person = rbacPersonRepo.findByUuid(resource.getPersonUuid()).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("{0} \"{1}\" not found or not accessible", "personUuid", resource.getPersonUuid())
                )
        );

        entity.setLoginContexts(contextMapper.mapCredentialsToContextEntities(resource.getContexts()));

        entity.setPerson(person);
    };
}
