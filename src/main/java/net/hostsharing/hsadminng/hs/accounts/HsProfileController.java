package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.val;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.ProfileApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CurrentLoginUserResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ProfileInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ProfilePatchResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ProfileResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ScopeResource;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import net.hostsharing.hsadminng.errors.Validate;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Optional.of;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class HsProfileController implements ProfileApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManagerWrapper em;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private ScopeResourceToEntityMapper scopeMapper;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    private HsProfileRepository profileRepo;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.accounts.profile.getSingleProfileByUuid")
    public ResponseEntity<ProfileResource> getSingleProfileByUuid(final UUID profileUuid) {

        context.define(); // without assumed roles, otherwise we cannot access the subject anymore

        val profileEntity = profileRepo.findByUuid(profileUuid);
        if (profileEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        val result = mapper.map(
                profileEntity.get(), ProfileResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.accounts.profile.getListOfProfileByPersonUuid")
    public ResponseEntity<List<ProfileResource>> getListOfProfile(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        val profile = personUuid == null
                ? profileRepo.findByCurrentSubject()
                : findByPersonUuid(personUuid);
        val result = mapper.mapList(
                profile, ProfileResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @Timed("app.accounts.profile.postNewProfile")
    public ResponseEntity<ProfileResource> postNewProfile(
            final ProfileInsertResource body
    ) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        final LoginContext originalLoginContext = new LoginContext(context);

        // first create and save the subject to get its UUID
        val newlySavedSubject = createSubject(body.getNickname());

        // switch to the new subject to get access to its own subject RBAC object
        context.define("activate newly created self-service subject", null, body.getNickname(), null);

        // afterward, create and save the profile entity with the subject's UUID
        val newProfileEntity = mapper.map(
                body, HsProfileEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        validateOnCreate(originalLoginContext, newProfileEntity);

        newProfileEntity.setSubject(em.merge(newlySavedSubject)); // attached to EM by the new subject
        em.persist(newProfileEntity); // newProfileEntity.uuid == newlySavedSubject.uuid => do not use repository!

        // return the new profile as a resource
        val uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/accounts/profiles/{id}")
                        .buildAndExpand(newProfileEntity.getUuid())
                        .toUri();
        val newProfileResource = mapper.map(
                newProfileEntity, ProfileResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(newProfileResource);
    }

    @Override
    @Transactional
    @Timed("app.accounts.profile.deleteProfileByUuid")
    public ResponseEntity<Void> deleteProfileByUuid(final UUID profileUuid) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        val profileEntity = em.getReference(HsProfileEntity.class, profileUuid);
        profileEntity.getScopes().clear();
        validateOnDelete(profileEntity);
        em.flush();
        em.remove(profileEntity);
        em.remove(profileEntity.getSubject());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.accounts.profile.patchProfile")
    public ResponseEntity<ProfileResource> patchProfile(
            final UUID profileUuid,
            final ProfilePatchResource body
    ) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        final LoginContext originalLoginContext = new LoginContext(context);

        val current = profileRepo.findByUuid(profileUuid).orElseThrow();

        validateBeforePatch(originalLoginContext, current, body);
        new HsProfileEntityPatcher(scopeMapper, current).apply(body);
        validateOnUpdate(current);

        val saved = profileRepo.save(current);
        val mapped = mapper.map(
                saved, ProfileResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    @Override
    @Transactional
    @Timed("app.accounts.profile.getCurrentLoginUser")
    public ResponseEntity<CurrentLoginUserResource> getCurrentLoginUser() {

        // define a context without assumed roles, otherwise we cannot access the subject anymore
        context.define();

        // fetch the data
        val currentSubjectUuid = context.fetchCurrentSubjectUuid();
        val currentSubject = rbacSubjectRepo.findByUuid(currentSubjectUuid);
        val person = profileRepo.findByUuid(currentSubjectUuid).orElseThrow().getPerson();

        final boolean isGlobalAdmin = context.isGlobalAdmin();

        // finally, return the result
        val result = currentLoginUserResponse(currentSubject, person, isGlobalAdmin);
        return ResponseEntity.ok(result);
    }

    private void validateBeforePatch(final LoginContext originalLoginContext, final HsProfileEntity current, final ProfilePatchResource body) {
        validateReferencedPersonToBeRepresentedByLoginUserPerson(originalLoginContext, current);

        if (!context.isGlobalAdmin() && !current.isActive() && body.getActive())
            throw new ForbiddenException("Only global admins are allowed to activate an inactive profile");
    }

    private void validateOnCreate(final LoginContext originalLoginContext, final HsProfileEntity newProfileEntity) {
        validateReferencedPersonToBeRepresentedByLoginUserPerson(originalLoginContext, newProfileEntity);
        validateNormalUsersOnlyAccessPublicScopes(newProfileEntity);
        validateNaturalPersonRequirementOfScopes(newProfileEntity);
    }

    private void validateOnUpdate(final HsProfileEntity current) {
        validateNormalUsersOnlyAccessPublicScopes(current);
        validateNaturalPersonRequirementOfScopes(current);
        validateOwnHsadminProfileMustNotBeRemoved(current);
    }

    private void validateOnDelete(final HsProfileEntity profileEntity) {
        validateOwnHsadminProfileMustNotBeRemoved(profileEntity);
    }

    private void validateReferencedPersonToBeRepresentedByLoginUserPerson(final LoginContext originalLoginContext, final HsProfileEntity profileEntity) {
        if (originalLoginContext.isGlobalAdmin) {
            return;
        }
        val referredPersonUuid = profileEntity.getPerson().getUuid();
        val loginPersonUuid = originalLoginContext.profile.getPerson().getUuid();
        val representedPersonUuids = realPersonRepo.findPersonsRepresentedByPersonWithUuid(loginPersonUuid)
                .stream().map(HsOfficePerson::getUuid).toList();
        if ( !representedPersonUuids.contains(referredPersonUuid)) {
            throw new ForbiddenException(
                    messageTranslator.translate(
                            "profile.access-denied-to-person-with-uuid-{0}-not-represented-by-currently-logged-in-person",
                            loginPersonUuid));
        }
    }

    private void validateNormalUsersOnlyAccessPublicScopes(final HsProfileEntity newProfileEntity) {
        val forbiddenScopes = newProfileEntity.getScopes().stream()
                .filter(c -> !c.isPublicAccess() && !context.isGlobalAdmin() )
                .toList();
        if (!forbiddenScopes.isEmpty()) {
            throw new ForbiddenException(
                    messageTranslator.translate(
                            "profile.access-denied-for-scopes-{0}",
                            toDisplay(forbiddenScopes)
                    ));
        }
    }

    private void validateNaturalPersonRequirementOfScopes(final HsProfileEntity newProfileEntity) {
        if (newProfileEntity.getPerson().getPersonType().equals(HsOfficePersonType.NATURAL_PERSON)) {
            return;
        }
        val scopesWhichRequireNaturalPerson = newProfileEntity.getScopes().stream()
                .filter(HsProfileScope::isOnlyForNaturalPersons)
                .toList();
        if (!scopesWhichRequireNaturalPerson.isEmpty()) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "profile.scope-requires-natural-person-{0}",
                            toDisplay(scopesWhichRequireNaturalPerson)
                    ));
        }
    }

    private void validateOwnHsadminProfileMustNotBeRemoved(final HsProfileEntity newProfileEntity) {
        if (!newProfileEntity.getSubject().getUuid().equals(context.fetchCurrentSubjectUuid())) {
            return;
        }
        val hsadminProfileScope = newProfileEntity.getScopes().stream()
                .filter(HsProfileScope::isHsadminScope)
                .toList();
        if (hsadminProfileScope.isEmpty()) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "profile.own-hsadmin-profile-must-not-be-removed"
                    ));
        }
    }

    private static String toDisplay(final List<HsProfileScopeRealEntity> scopesWhichRequireNaturalPerson) {
        return scopesWhichRequireNaturalPerson.stream()
                .map(HsProfileScope::toShortString)
                .sorted()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", "));
    }

    private RealSubjectEntity createSubject(final String nickname) {
        val rbacSubjectEntity = RbacSubjectEntity.builder().name(nickname).build();
        val newRbacSubject = rbacSubjectRepo.create(rbacSubjectEntity);
        return em.find(RealSubjectEntity.class, newRbacSubject.getUuid());
    }

    private List<HsProfileEntity> findByPersonUuid(final UUID personUuid) {
        val person = realPersonRepo.findByUuid(personUuid).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("general.{0}-{1}-not-found-or-not-accessible", "personUuid", personUuid)
                )

        );
        return profileRepo.findByPerson(person);
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

    final BiConsumer<HsProfileEntity, ProfileResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        of(entity.getSubject()).ifPresent(
                subject -> resource.setNickname(subject.getName())
        );
        of(entity.getPerson()).ifPresent(
                person -> resource.setPerson(
                    mapper.map(person, HsOfficePersonResource.class)
            )
        );

        resource.setScopes(mapToValidScopeResources(entity));
    };

    private List<ScopeResource> mapToValidScopeResources(final HsProfileEntity entity) {
        var allScopes = mapper.mapList(entity.getScopes().stream().toList(), ScopeResource.class);
        return allScopes.stream()
            .filter(scope -> !scope.getOnlyForNaturalPersons() ||
                          entity.getPerson().getPersonType() == HsOfficePersonType.NATURAL_PERSON)
        .toList();
    }

    final BiConsumer<ProfileInsertResource, HsProfileEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {

        Validate.validate("person, person.uuid").exactlyOne(resource.getPerson(), resource.getPersonUuid());
        if ( resource.getPersonUuid() != null) {
            entity.setPerson(realPersonRepo.findByUuid(resource.getPersonUuid()).orElseThrow(
                    () -> new NoSuchElementException("cannot find Person by 'person.uuid': " + resource.getPersonUuid())
            ));
        } else {
            entity.setPerson(realPersonRepo.save(
                    mapper.map(resource.getPerson(), HsOfficePersonRealEntity.class)
            ) );
        }

        val person = realPersonRepo.findByUuid(entity.getPerson().getUuid()).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("general.{0}-{1}-not-found-or-not-accessible", "personUuid", resource.getPersonUuid())
                )
        );
        entity.setPerson(person);
        entity.setScopes(scopeMapper.mapProfileToScopeEntities(resource.getScopes()));
        entity.setPassword(resource.getPassword());
    };

    @AllArgsConstructor
    private class LoginContext {
        final HsProfileEntity profile;
        final boolean isGlobalAdmin;

        public LoginContext(final Context context) {
            val subjectUuid = context.fetchCurrentSubjectUuid();
            profile = profileRepo.findByUuid(subjectUuid)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "subject " + context.fetchCurrentSubject() + " has no profile"));
            isGlobalAdmin = context.isGlobalAdmin();
        }
    }
}
