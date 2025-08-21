package net.hostsharing.hsadminng.hs.accounts;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CurrentLoginUserResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.CredentialsApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
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
import jakarta.validation.ValidationException;

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

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.credentials.credentials.getSingleCredentialsByUuid")
    public ResponseEntity<CredentialsResource> getSingleCredentialsByUuid(final UUID credentialsUuid) {

        context.define(); // without assumed roles, otherwise we cannot access the subject anymore

        final var credentialsEntity = credentialsRepo.findByUuid(credentialsUuid);
        if (credentialsEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final var result = mapper.map(
                credentialsEntity.get(), CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.credentials.credentials.getListOfCredentialsByPersonUuid")
    public ResponseEntity<List<CredentialsResource>> getListOfCredentials(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        final var credentials = personUuid == null
                ? credentialsRepo.findByCurrentSubject()
                : findByPersonUuid(personUuid);
        final var result = mapper.mapList(
                credentials, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.postNewCredentials")
    public ResponseEntity<CredentialsResource> postNewCredentials(
            final CredentialsInsertResource body
    ) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore

        // first create and save the subject to get its UUID
        final var newlySavedSubject = createSubject(body.getNickname());

        // afterward, create and save the credentials entity with the subject's UUID
        final var newCredentialsEntity = mapper.map(
                body, HsCredentialsEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        validate(newCredentialsEntity);
        newCredentialsEntity.setSubject(newlySavedSubject);
        em.persist(newCredentialsEntity); // newCredentialsEntity.uuid == newlySavedSubject.uuid => do not use repository!

        // return the new credentials as a resource
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/accounts/credentials/{id}")
                        .buildAndExpand(newCredentialsEntity.getUuid())
                        .toUri();
        final var newCredentialsResource = mapper.map(
                newCredentialsEntity, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(newCredentialsResource);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.deleteCredentialsByUuid")
    public ResponseEntity<Void> deleteCredentialsByUuid(final UUID credentialsUuid) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        final var credentialsEntity = em.getReference(HsCredentialsEntity.class, credentialsUuid);
        credentialsEntity.getLoginContexts().clear();
        em.flush();
        em.remove(credentialsEntity);
        em.remove(credentialsEntity.getSubject());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.patchCredentials")
    public ResponseEntity<CredentialsResource> patchCredentials(
            final UUID credentialsUuid,
            final CredentialsPatchResource body
    ) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore

        final var current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        new HsCredentialsEntityPatcher(contextMapper, current).apply(body);

        final var saved = credentialsRepo.save(current);
        final var mapped = mapper.map(
                saved, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.getCurrentLoginUser")
    public ResponseEntity<CurrentLoginUserResource> getCurrentLoginUser() {

        // define a context without assumed roles, otherwise we cannot access the subject anymore
        context.define();

        // fetch the data
        final var currentSubjectUuid = context.fetchCurrentSubjectUuid();
        final var currentSubject = rbacSubjectRepo.findByUuid(currentSubjectUuid);
        final boolean isGlobalAdmin = context.isGlobalAdmin();
        final var person = credentialsRepo.findByUuid(currentSubjectUuid).orElseThrow().getPerson();

        // finally, return the result
        final var result = currentLoginUserResponse(currentSubject, person, isGlobalAdmin);
        return ResponseEntity.ok(result);
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

    private void validate(final HsCredentialsEntity newCredentialsEntity) {
        // the referenced person must be represented by currently logged in person
        final var personUuid = newCredentialsEntity.getPerson().getUuid();
        final var representedPersonUuids = rbacPersonRepo.findPersonsrepresentedByPersonWithUuid(personUuid)
                .stream().map(HsOfficePerson::getUuid).toList();
        if ( !representedPersonUuids.contains(personUuid)) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "access-denied-personUuid-{0}-not-represented-by-currently-logged-in-person",
                            personUuid));
        }
    }

    private RbacSubjectEntity createSubject(final String nickname) {
        final var newRbacSubject = subjectRepo.create(new RbacSubjectEntity(null, nickname));
        if(context.fetchCurrentSubject() == null) {
            context.define("activate newly created self-service subject", null, nickname, null);
        }
        return subjectRepo.findByUuid(newRbacSubject.getUuid()); // now attached to EM
    }

    private List<HsCredentialsEntity> findByPersonUuid(final UUID personUuid) {
        final var person = rbacPersonRepo.findByUuid(personUuid).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("{0} \"{1}\" not found or not accessible", "personUuid", personUuid)
                )

        );
        return credentialsRepo.findByPerson(person);
    }


    private CurrentLoginUserResource currentLoginUserResponse(
            final RbacSubjectEntity currentSubject,
            final HsOfficePerson<?> person,
            final boolean isGlobalAdmin) {
        final var result = new CurrentLoginUserResource();
        result.setSubject(mapper.map(currentSubject, RbacSubjectResource.class));
        result.setPerson(mapper.map(person, HsOfficePersonResource.class));
        result.setGlobalAdmin(isGlobalAdmin);
        return result;
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

        resource.setContexts(mapToValidContextResources(entity));
    };

    private List<ContextResource> mapToValidContextResources(final HsCredentialsEntity entity) {
        var allContexts = mapper.mapList(entity.getLoginContexts().stream().toList(), ContextResource.class);
        return allContexts.stream()
            .filter(context -> !context.getOnlyForNaturalPersons() ||
                          entity.getPerson().getPersonType() == HsOfficePersonType.NATURAL_PERSON)
        .toList();
    }

    final BiConsumer<CredentialsInsertResource, HsCredentialsEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        final var person = rbacPersonRepo.findByUuid(resource.getPersonUuid()).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("{0} \"{1}\" not found or not accessible", "personUuid", resource.getPersonUuid())
                )
        );

        entity.setLoginContexts(contextMapper.mapCredentialsToContextEntities(resource.getContexts()));

        entity.setPerson(person);
    };
}
