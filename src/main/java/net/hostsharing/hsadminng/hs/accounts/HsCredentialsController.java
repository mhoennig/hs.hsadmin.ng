package net.hostsharing.hsadminng.hs.accounts;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.val;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CurrentLoginUserResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.CredentialsApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;

import static java.util.Optional.ofNullable;
import static java.util.Optional.of;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
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
    private HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    private HsCredentialsRepository credentialsRepo;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.credentials.credentials.getSingleCredentialsByUuid")
    public ResponseEntity<CredentialsResource> getSingleCredentialsByUuid(final UUID credentialsUuid) {

        context.define(); // without assumed roles, otherwise we cannot access the subject anymore

        val credentialsEntity = credentialsRepo.findByUuid(credentialsUuid);
        if (credentialsEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        val result = mapper.map(
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

        val credentials = personUuid == null
                ? credentialsRepo.findByCurrentSubject()
                : findByPersonUuid(personUuid);
        val result = mapper.mapList(
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
        val newlySavedSubject = createSubject(body.getNickname());

        // afterward, create and save the credentials entity with the subject's UUID
        val newCredentialsEntity = mapper.map(
                body, HsCredentialsEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        validateOnCreate(newCredentialsEntity);

        // switch to the new subject to get access to its own subject RBAC object
        context.define("activate newly created self-service subject", null, body.getNickname(), null);
        newCredentialsEntity.setSubject(em.merge(newlySavedSubject)); // attached to EM by the new subject
        em.persist(newCredentialsEntity); // newCredentialsEntity.uuid == newlySavedSubject.uuid => do not use repository!

        // return the new credentials as a resource
        val uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/accounts/credentials/{id}")
                        .buildAndExpand(newCredentialsEntity.getUuid())
                        .toUri();
        val newCredentialsResource = mapper.map(
                newCredentialsEntity, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(newCredentialsResource);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.deleteCredentialsByUuid")
    public ResponseEntity<Void> deleteCredentialsByUuid(final UUID credentialsUuid) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        val credentialsEntity = em.getReference(HsCredentialsEntity.class, credentialsUuid);
        credentialsEntity.getLoginContexts().clear();
        validateOnDelete(credentialsEntity);
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

        val current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        new HsCredentialsEntityPatcher(contextMapper, current).apply(body);
        validateOnUpdate(current);

        val saved = credentialsRepo.save(current);
        val mapped = mapper.map(
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
        val currentSubjectUuid = context.fetchCurrentSubjectUuid();
        val currentSubject = rbacSubjectRepo.findByUuid(currentSubjectUuid);
        val person = credentialsRepo.findByUuid(currentSubjectUuid).orElseThrow().getPerson();

        final boolean isGlobalAdmin = context.isGlobalAdmin();

        // finally, return the result
        val result = currentLoginUserResponse(currentSubject, person, isGlobalAdmin);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @Timed("app.credentials.credentials.credentialsUsed")
    public ResponseEntity<CredentialsResource> credentialsUsed(final UUID credentialsUuid) {
        context.define();

        val current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        current.setOnboardingToken(null);
        current.setLastUsed(LocalDateTime.now());

        val saved = credentialsRepo.save(current);
        val mapped = mapper.map(saved, CredentialsResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    private void validateOnCreate(final HsCredentialsEntity newCredentialsEntity) {
        validateReferencedPersonToBeRepresentedByLoginUserPerson(newCredentialsEntity);
        validateNormalUsersOnlyAccessPublicContexts(newCredentialsEntity);
        validateNaturalPersonRequirementOfContexts(newCredentialsEntity);
    }

    private void validateOnUpdate(final HsCredentialsEntity current) {
        validateNormalUsersOnlyAccessPublicContexts(current);
        validateNaturalPersonRequirementOfContexts(current);
        validateOwnHsadminCredentialsMustNotBeRemoved(current);
    }

    private void validateOnDelete(final HsCredentialsEntity credentialsEntity) {
        validateOwnHsadminCredentialsMustNotBeRemoved(credentialsEntity);
    }

    private void validateReferencedPersonToBeRepresentedByLoginUserPerson(final HsCredentialsEntity newCredentialsEntity) {
        if (context.isGlobalAdmin()) {
            return;
        }
        val referredPersonUuid = newCredentialsEntity.getPerson().getUuid();
        val currentSubjectUuid = context.fetchCurrentSubjectUuid();
        val loginPersonUuid = credentialsRepo.findByUuid(currentSubjectUuid)
                .map(HsCredentialsEntity::getPerson)
                .map(HsOfficePerson::getUuid)
                .orElseThrow();
        val representedPersonUuids = realPersonRepo.findPersonsRepresentedByPersonWithUuid(loginPersonUuid)
                .stream().map(HsOfficePerson::getUuid).toList();
        if ( !representedPersonUuids.contains(referredPersonUuid)) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "credentials.access-denied-to-person-with-uuid-{0}-not-represented-by-currently-logged-in-person",
                            loginPersonUuid));
        }
    }

    private void validateNormalUsersOnlyAccessPublicContexts(final HsCredentialsEntity newCredentialsEntity) {
        val forbiddenContexts = newCredentialsEntity.getLoginContexts().stream()
                .filter(c -> !c.isPublicAccess() && !context.isGlobalAdmin() )
                .toList();
        if (!forbiddenContexts.isEmpty()) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "credentials.access-denied-for-contexts-{0}",
                            toDisplay(forbiddenContexts)
                    ));
        }
    }

    private void validateNaturalPersonRequirementOfContexts(final HsCredentialsEntity newCredentialsEntity) {
        if (newCredentialsEntity.getPerson().getPersonType().equals(HsOfficePersonType.NATURAL_PERSON)) {
            return;
        }
        val contextsWhichRequireNaturalPerson = newCredentialsEntity.getLoginContexts().stream()
                .filter(HsCredentialsContext::isOnlyForNaturalPersons)
                .toList();
        if (!contextsWhichRequireNaturalPerson.isEmpty()) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "credentials.context-requires-natural-person-{0}",
                            toDisplay(contextsWhichRequireNaturalPerson)
                    ));
        }
    }

    private void validateOwnHsadminCredentialsMustNotBeRemoved(final HsCredentialsEntity newCredentialsEntity) {
        if (!newCredentialsEntity.getSubject().getUuid().equals(context.fetchCurrentSubjectUuid())) {
            return;
        }
        val hsadminCredentialsContext = newCredentialsEntity.getLoginContexts().stream()
                .filter(HsCredentialsContext::isHsadminContext)
                .toList();
        if (hsadminCredentialsContext.isEmpty()) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "credentials.own-hsadmin-credentials-must-not-be-removed"
                    ));
        }
    }

    private static String toDisplay(final List<HsCredentialsContextRealEntity> contextsWhichRequireNaturalPerson) {
        return contextsWhichRequireNaturalPerson.stream()
                .map(HsCredentialsContext::toShortString)
                .sorted()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", "));
    }

    private RbacSubjectEntity createSubject(final String nickname) {
        val rbacSubjectEntity = new RbacSubjectEntity(null, nickname);
        val newRbacSubject = subjectRepo.create(rbacSubjectEntity);
        return newRbacSubject;
    }

    private List<HsCredentialsEntity> findByPersonUuid(final UUID personUuid) {
        val person = realPersonRepo.findByUuid(personUuid).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("general.{0}-{1}-not-found-or-not-accessible", "personUuid", personUuid)
                )

        );
        return credentialsRepo.findByPerson(person);
    }


    private CurrentLoginUserResource currentLoginUserResponse(
            final RbacSubjectEntity currentSubject,
            final HsOfficePerson<?> person,
            final boolean isGlobalAdmin) {
        val result = new CurrentLoginUserResource();
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
        val person = realPersonRepo.findByUuid(resource.getPersonUuid()).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("general.{0}-{1}-not-found-or-not-accessible", "personUuid", resource.getPersonUuid())
                )
        );

        entity.setLoginContexts(contextMapper.mapCredentialsToContextEntities(resource.getContexts()));

        entity.setPerson(person);
    };
}
