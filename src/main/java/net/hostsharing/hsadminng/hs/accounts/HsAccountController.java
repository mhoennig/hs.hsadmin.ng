package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.val;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.AccountApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CurrentLoginUserResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.HsOfficePersonResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.AccountInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.AccountResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.AccountSubjectInsertResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.RbacSubjectResource;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.errors.ConflictException;
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
import net.hostsharing.hsadminng.rbac.subject.RealSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.SubjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
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
    private RealSubjectRepository realSubjectRepo;

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
    @Timed("app.accounts.account.getListOfAccountsByPersonUuid")
    public ResponseEntity<List<AccountResource>> getListOfAccounts(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        final List<HsAccountEntity> accounts;
        if (personUuid != null) {
            accounts = findByPersonUuid(personUuid);
        } else if (context.hasGlobalAdminRole()) {
            accounts = accountRepo.findAll();
        } else {
            accounts = accountRepo.findByCurrentSubject();
        }
        val result = mapper.mapList(
                accounts, AccountResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
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

        // determine the assigned person while we still have global-admin privileges
        val newAccountEntity = mapper.map(
                body, HsAccountEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);
        validateOnCreate(newAccountEntity);

        // fetch the existing subject or create and save a new one to get its UUID
        val accountSubject = fetchOrCreateSubject(body);

        // grant the person's ADMIN role to the account subject
        rbacGrantService.grant(rbacRoleService.rbacRole(newAccountEntity.getPerson(), RbacRoleType.ADMIN))
                .to(accountSubject);

        // switch to the account subject to get access to its own subject RBAC object
        context.define("activate the account subject", null, accountSubject.getName(), null);

        // afterward, create and save the account entity with the subject's UUID
        newAccountEntity.setSubject(em.merge(accountSubject)); // attached to EM by the account subject
        em.persist(newAccountEntity); // newAccountEntity.uuid == accountSubject.uuid => do not use repository!

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

        // fetch the data, the person is null if the current subject has no account
        val currentSubjectUuid = context.fetchCurrentSubjectUuid();
        val currentSubject = realSubjectRepo.findCurrentSubject();
        val person = accountRepo.findByUuid(currentSubjectUuid).map(HsAccountEntity::getPerson).orElse(null);

        final boolean isGlobalAdmin = context.isGlobalAdmin();

        // finally, return the result
        val result = currentLoginUserResponse(currentSubject, person, isGlobalAdmin);
        return ResponseEntity.ok(result);
    }

    private void validateOnCreate(final HsAccountEntity newAccountEntity) {
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

    private RealSubjectEntity fetchOrCreateSubject(final AccountInsertResource body) {
        Validate.validate("subject, subject.uuid").exactlyOne(body.getSubject(), body.getSubjectUuid());

        if (body.getSubjectUuid() != null) {
            return fetchExistingUserSubject(body.getSubjectUuid());
        }
        return createSubject(body.getSubject());
    }

    private RealSubjectEntity fetchExistingUserSubject(final UUID subjectUuid) {
        val subject = realSubjectRepo.findVisibleSubjectByUuid(subjectUuid).orElseThrow(
                () -> new ValidationException(
                        messageTranslator.translate("account.no-subject-with-uuid-{0}-found", subjectUuid)));
        if (subject.getType() != SubjectType.USER) {
            throw new ValidationException(
                    messageTranslator.translate(
                            "account.subject-{0}-is-not-a-user-subject-but-{1}",
                            subject.getUuid(), subject.getType().name()));
        }
        if (accountRepo.findByUuid(subject.getUuid()).isPresent()) {
            throw new ConflictException(
                    messageTranslator.translate(
                            "account.subject-{0}-already-has-an-account",
                            subject.getUuid()));
        }
        return subject;
    }

    private RealSubjectEntity createSubject(final AccountSubjectInsertResource newSubject) {
        // newSubject.uuid+name are @NotNull-validated and type can only be USER, all enforced by the API definition
        if (realSubjectRepo.findVisibleSubjectByUuid(newSubject.getUuid()).isPresent()) {
            throw new ConflictException(
                    messageTranslator.translate(
                            "account.subject-with-uuid-{0}-already-exists",
                            newSubject.getUuid()));
        }
        val rbacSubjectEntity = RbacSubjectEntity.builder().uuid(newSubject.getUuid()).name(newSubject.getName()).build();
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
            final RealSubjectEntity currentSubject,
            final HsOfficePerson<?> person,
            final boolean isGlobalAdmin) {
        val result = new CurrentLoginUserResource();
        result.setSubject(mapper.map(currentSubject, RbacSubjectResource.class));
        if (person != null) {
            result.setPerson(mapper.map(person, HsOfficePersonResource.class));
        }
        result.setGlobalAdmin(isGlobalAdmin);
        return result;
    }

    final BiConsumer<HsAccountEntity, AccountResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        of(entity.getSubject()).ifPresent(
                subject -> resource.setSubject(mapper.map(subject, RbacSubjectResource.class))
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

}
