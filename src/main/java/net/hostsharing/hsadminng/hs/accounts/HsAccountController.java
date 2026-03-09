package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.val;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.AccountApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CurrentLoginUserResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.AccountInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.AccountResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.RbacSubjectResource;
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
import net.hostsharing.hsadminng.rbac.grant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantService;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.role.RbacRoleService;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
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

import static java.util.Optional.of;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class HsAccountController implements AccountApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManagerWrapper em;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    private HsAccountRepository accountRepo;

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Autowired
    private RbacRoleRepository rbacRoleRepo;

    @Autowired
    private RbacGrantRepository rbacGrantRepo;

    @Autowired
    private RbacRoleService rbacRoleService;

    @Autowired
    private RbacGrantService rbacGrantService;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.accounts.account.getSingleAccountByUuid")
    public ResponseEntity<AccountResource> getSingleAccountByUuid(final UUID accountUuid) {

        context.define(); // without assumed roles, otherwise we cannot access the real subject anymore

        val accountEntity = accountRepo.findByUuid(accountUuid);
        if (accountEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        val result = mapper.map(
                accountEntity.get(), AccountResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Timed("app.accounts.account.getListIfAccountByPersonUuid")
    public ResponseEntity<List<AccountResource>> getListIfAccount(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        val account = personUuid == null
                ? accountRepo.findByCurrentSubject()
                : findByPersonUuid(personUuid);
        val result = mapper.mapList(
                account, AccountResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @Timed("app.accounts.account.postNewAccount")
    public ResponseEntity<AccountResource> postNewAccount(
            final AccountInsertResource body
    ) {
        // Only with exactly 1 assumed role, we can do explicit grants, because the assumed role is used as 'grantor'.
        // Otherwise, only the granting user could revoke the grant.
        context.assumeRoles("rbac.global#global:ADMIN");

        val originalLoginContext = new LoginContext(context);

        // TODO.spec: for now, only global admins can create new accounts, auto-creation has to be specified. which person?
        if (!originalLoginContext.isGlobalAdmin) {
            throw new ForbiddenException(
                    messageTranslator.translate(
                            //"account.access-denied-to-person-with-uuid-{0}-not-represented-by-currently-logged-in-person",
                            "account.access-denied-to-create-new-account-subject-{0}-is-not-a-global-admin",
                            originalLoginContext.subjectUuid));
        }

        // first create and save the subject to get its UUID
        val newlySavedSubject = createSubject(body.getSubjectName());

        // determine the assigned person while we still have global-admin privileges
        val newAccountEntity = mapper.map(
                body, HsAccountEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        validateOnCreate(originalLoginContext, newAccountEntity);

        // grant the person's ADMIN role to the new subject
        rbacGrantService.grant(rbacRoleService.rbacRole(newAccountEntity.getPerson(), RbacRoleType.ADMIN))
                .to(newlySavedSubject);

        // switch to the new subject to get access to its own subject RBAC object
        context.define("activate newly created self-service subject", null, body.getSubjectName(), null);

        // afterward, create and save the account entity with the subject's UUID
        newAccountEntity.setSubject(em.merge(newlySavedSubject)); // attached to EM by the new subject
        em.persist(newAccountEntity); // newAccountEntity.uuid == newlySavedSubject.uuid => do not use repository!

        // return the new account as a resource
        val uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/accounts/accounts/{id}")
                        .buildAndExpand(newAccountEntity.getUuid())
                        .toUri();
        val newAccountResource = mapper.map(
                newAccountEntity, AccountResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(newAccountResource);
    }

    @Override
    @Transactional
    @Timed("app.accounts.account.deleteAccountByUuid")
    public ResponseEntity<Void> deleteAccountByUuid(final UUID accountUuid) {
        context.define(); // without assumed roles, otherwise we cannot access the subject anymore
        val accountEntity = em.getReference(HsAccountEntity.class, accountUuid);
        validateOnDelete(accountEntity);
        em.flush();
        em.remove(accountEntity);
        em.remove(accountEntity.getSubject());
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.accounts.account.getCurrentLoginUser")
    public ResponseEntity<CurrentLoginUserResource> getCurrentLoginUser() {

        // define a context without assumed roles, otherwise we cannot access the subject anymore
        context.define();

        // fetch the data
        val currentSubjectUuid = context.fetchCurrentSubjectUuid();
        val currentSubject = rbacSubjectRepo.findByUuid(currentSubjectUuid);
        val person = accountRepo.findByUuid(currentSubjectUuid).orElseThrow().getPerson();

        final boolean isGlobalAdmin = context.isGlobalAdmin();

        // finally, return the result
        val result = currentLoginUserResponse(currentSubject, person, isGlobalAdmin);
        return ResponseEntity.ok(result);
    }

    private void validateOnCreate(final LoginContext originalLoginContext, final HsAccountEntity newAccountEntity) {
        validateReferencedPersonToBeANaturalPerson(newAccountEntity);
    }

    private void validateOnDelete(final HsAccountEntity current) {
        // TODO.spec Task#5637: still needed? can the own account even be removed, even the last one?
    }

    private void validateReferencedPersonToBeANaturalPerson(final HsAccountEntity accountEntity) {
        val referredPerson = accountEntity.getPerson();
        if ( referredPerson.getPersonType() != HsOfficePersonType.NATURAL_PERSON) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "account.only-natural-persons-allowed-but-{0}-is-{1}",
                            referredPerson.getUuid(), referredPerson.getPersonType().name()));
        }
    }

    private RealSubjectEntity createSubject(final String subjectName) {
        val rbacSubjectEntity = RbacSubjectEntity.builder().name(subjectName).build();
        val newRbacSubject = rbacSubjectRepo.create(rbacSubjectEntity);
        return em.find(RealSubjectEntity.class, newRbacSubject.getUuid());
    }

    private List<HsAccountEntity> findByPersonUuid(final UUID personUuid) {
        val person = realPersonRepo.findByUuid(personUuid).orElseThrow(
                () -> new EntityNotFoundException(
                        messageTranslator.translate("general.{0}-{1}-not-found-or-not-accessible", "personUuid", personUuid)
                )

        );
        return accountRepo.findByPerson(person);
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

    final BiConsumer<HsAccountEntity, AccountResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        of(entity.getSubject()).ifPresent(
                subject -> resource.setSubjectName(subject.getName())
        );
        of(entity.getPerson()).ifPresent(
                person -> resource.setPerson(
                    mapper.map(person, HsOfficePersonResource.class)
            )
        );
    };

    final BiConsumer<AccountInsertResource, HsAccountEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {

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
    };

    @AllArgsConstructor
    private class LoginContext {
        final HsAccountEntity account;
        final boolean isGlobalAdmin;
        final UUID subjectUuid;

        public LoginContext(final Context context) {
            subjectUuid = context.fetchCurrentSubjectUuid();
            account = accountRepo.findByUuid(subjectUuid)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "subject " + context.fetchCurrentSubject() + " has no account"));
            isGlobalAdmin = context.isGlobalAdmin();
        }
    }
}
