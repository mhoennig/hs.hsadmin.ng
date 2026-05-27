package net.hostsharing.hsadminng.hs.accounts;

import lombok.val;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantService;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.role.RbacRoleService;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsAccountController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({ "fake-jwt", "test" })
class HsAccountControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsOfficePersonRealRepository realPersonRepo;

    @MockitoBean
    HsAccountRepository accountRepo;

    @MockitoBean
    RbacSubjectRepository rbacSubjectRepo;

    @MockitoBean
    RbacRoleRepository rbacRoleRepo;

    @MockitoBean
    RbacGrantRepository rbacGrantRepo;

    @MockitoBean
    RbacRoleService rbacRoleService;

    @MockitoBean
    RbacGrantService rbacGrantService;

    @Test
    void getByUuidFetchesSingleAccount() throws Exception {
        // given
        val givenAccountUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenAccount = givenAccount(givenAccountUuid, "abc-existing", givenPerson);
        given(accountRepo.findByUuid(givenAccountUuid)).willReturn(Optional.of(givenAccount));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts/{accountUuid}", givenAccountUuid)
                        .header("Authorization", bearer("abc-existing"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).define();
        response.andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(givenAccountUuid.toString()))
                .andExpect(jsonPath("subjectName").value("abc-existing"))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("person.personType").value("NATURAL_PERSON"))
                .andExpect(jsonPath("globalUid").value(30001))
                .andExpect(jsonPath("globalGid").value(40001));
    }

    @Test
    void getByUuidForUnknownAccountUuidReturnsNotFound() throws Exception {
        // given
        val givenAccountUuid = UUID.randomUUID();
        given(accountRepo.findByUuid(givenAccountUuid)).willReturn(Optional.empty());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts/{accountUuid}", givenAccountUuid)
                        .header("Authorization", bearer("abc-existing"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).define();
        response.andExpect(status().isNotFound());
    }

    @Test
    void getListOfAccountsForCurrentSubjectReturnsOwnAccont() throws Exception {
        // given
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenAccount = givenAccount(UUID.randomUUID(), "abc-current", givenPerson);
        given(accountRepo.findByCurrentSubject()).willReturn(java.util.List.of(givenAccount));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("abc-current"))
                        .header("assumed-roles", "rbac.global#global:ADMIN")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value(givenAccount.getUuid().toString()))
                .andExpect(jsonPath("$[0].subjectName").value("abc-current"))
                .andExpect(jsonPath("$[0].person.uuid").value(givenPerson.getUuid().toString()));
    }

    @Test
    void getListOfAccountsForGivenPersonUuidReturnsThatPersonsAccounts() throws Exception {
        // given
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenAccountAbc = givenAccount(UUID.randomUUID(), "abc-person", givenPerson);
        val givenAccountXyz = givenAccount(UUID.randomUUID(), "xyz-person", givenPerson);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(accountRepo.findByPerson(givenPerson)).willReturn(java.util.List.of(givenAccountAbc, givenAccountXyz));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts")
                        .queryParam("personUuid", givenPerson.getUuid().toString())
                        .header("Authorization", bearer("abc-person"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles(null);
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uuid").value(givenAccountAbc.getUuid().toString()))
                .andExpect(jsonPath("$[0].subjectName").value("abc-person"))
                .andExpect(jsonPath("$[0].person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("$[1].uuid").value(givenAccountXyz.getUuid().toString()))
                .andExpect(jsonPath("$[1].subjectName").value("xyz-person"))
                .andExpect(jsonPath("$[1].person.uuid").value(givenPerson.getUuid().toString()));
    }

    @Test
    void getListOfAccountsForInaccessiblePersonUuidReturnsErrorMessage() throws Exception {
        // given
        val givenPersonUuid = UUID.randomUUID();
        given(realPersonRepo.findByUuid(givenPersonUuid)).willReturn(Optional.empty());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts")
                        .queryParam("personUuid", givenPersonUuid.toString())
                        .header("Authorization", bearer("abc-person"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles(null);
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "personUuid \"%s\" not found or not accessible".formatted(givenPersonUuid))));
    }

    @Test
    void getCurrentLoginUserReturnsOnwAccount() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(rbacSubjectRepo.findByUuid(givenSubjectUuid)).willReturn(RbacSubjectEntity.builder()
                .uuid(givenSubjectUuid)
                .name("abc-current")
                .build());
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(givenAccount(givenSubjectUuid, "abc-current", givenPerson)));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/current")
                        .header("Authorization", bearer("abc-current"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).define();
        response.andExpect(status().isOk())
                .andExpect(jsonPath("subject.uuid").value(givenSubjectUuid.toString()))
                .andExpect(jsonPath("subject.name").value("abc-current"))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("globalAdmin").value(true));
    }

    @Test
    void postNewAccountWithExistingPersonUuidCreatesAccount() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenNewSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenNewSubject = RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build();
        val givenPersonRole = new RbacRoleEntity();
        givenPersonRole.setUuid(UUID.randomUUID());
        val grantMock = mock(RbacGrantService.RbacRoleGranter.class);

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));
        given(rbacSubjectRepo.create(any())).willReturn(RbacSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(givenNewSubject);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(rbacRoleService.rbacRole(givenPerson, RbacRoleType.ADMIN)).willReturn(givenPersonRole);
        given(rbacGrantService.grant(givenPersonRole)).willReturn(grantMock);
        given(em.merge(givenNewSubject)).willReturn(givenNewSubject);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenNewSubjectUuid.toString()))
                .andExpect(jsonPath("subjectName").value("abc-newuser"))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("globalUid").value(30001))
                .andExpect(jsonPath("globalGid").value(40001));

        verify(contextMock).define("activate newly created self-service subject", null, "abc-newuser", null);
        verify(grantMock).to(givenNewSubject);
        verify(em).persist(any(HsAccountEntity.class));
    }

    @Test
    void postNewAccountWithNewPersonCreatesAccountAndPerson() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenNewSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("New", "Person");
        val givenNewSubject = RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build();
        val givenPersonRole = new RbacRoleEntity();
        givenPersonRole.setUuid(UUID.randomUUID());
        val grantMock = mock(RbacGrantService.RbacRoleGranter.class);

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));
        given(rbacSubjectRepo.create(any())).willReturn(RbacSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(givenNewSubject);
        given(realPersonRepo.save(any())).willReturn(givenPerson);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(rbacRoleService.rbacRole(givenPerson, RbacRoleType.ADMIN)).willReturn(givenPersonRole);
        given(rbacGrantService.grant(givenPersonRole)).willReturn(grantMock);
        given(em.merge(givenNewSubject)).willReturn(givenNewSubject);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person": {
                                        "personType": "NATURAL_PERSON",
                                        "givenName": "New",
                                        "familyName": "Person"
                                    },
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenNewSubjectUuid.toString()))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("person.givenName").value("New"))
                .andExpect(jsonPath("person.familyName").value("Person"));

        verify(realPersonRepo).save(any());
        verify(grantMock).to(givenNewSubject);
    }

    @Test
    void postNewAccountRejectsToCreateIfCurrentSubjectIsNotGlobalAdmin() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPersonUuid = UUID.randomUUID();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(false);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPersonUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isForbidden())
                .andExpect(jsonPath("message", containsString(
                        "Access denied: new accounts can only be created by global admins, %s is not"
                                .formatted(givenSubjectUuid))));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountFailsIfCurrentSubjectAccountCannotBeResolved() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.fetchCurrentSubject()).willReturn("unresolvable-subject@hostsharing.net");
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.empty());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("unresolvable-subject@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(UUID.randomUUID()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message", containsString(
                        "subject unresolvable-subject@hostsharing.net has no account")));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountRejectsCreatingAccountForNonNaturalPerson() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenNewSubjectUuid = UUID.randomUUID();
        val givenPersonUuid = UUID.randomUUID();
        val givenLegalPerson = HsOfficePersonRealEntity.builder()
                .uuid(givenPersonUuid)
                .personType(LEGAL_PERSON)
                .tradeName("Test Company")
                .build();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));
        given(rbacSubjectRepo.create(any())).willReturn(RbacSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(realPersonRepo.findByUuid(givenPersonUuid)).willReturn(Optional.of(givenLegalPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPersonUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "only natural persons allowed, but %s is LEGAL_PERSON".formatted(givenPersonUuid))));

        verify(rbacGrantService, never()).grant(any());
    }

    @Test
    void postNewAccountsRejectsCreatingAccountWithoutPersonReference() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenNewSubjectUuid = UUID.randomUUID();
        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));
        given(rbacSubjectRepo.create(any())).willReturn(RbacSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "Exactly one of (person, person.uuid) must be non-null, but are (null, null)")));
    }

    @Test
    void postNewAccountRejectsCreatingAccountForUnknownPersonUuid() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenNewSubjectUuid = UUID.randomUUID();
        val givenPersonUuid = UUID.randomUUID();
        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.of(HsAccountEntity.builder().build()));
        given(rbacSubjectRepo.create(any())).willReturn(RbacSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .build());
        given(realPersonRepo.findByUuid(givenPersonUuid)).willReturn(Optional.empty());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subjectName": "abc-newuser",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPersonUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("message", containsString("cannot find Person by 'person.uuid': " + givenPersonUuid)));
    }

    @Test
    void deleteByAccountDeletesExisingAccount() throws Exception {
        // given
        val givenAccountUuid = UUID.randomUUID();
        val givenSubject = RealSubjectEntity.builder()
                .uuid(givenAccountUuid)
                .name("abc-delete")
                .build();
        val givenAccount = HsAccountEntity.builder()
                .subject(givenSubject)
                .person(givenNaturalPerson("Delete", "Person"))
                .build();
        given(em.getReference(HsAccountEntity.class, givenAccountUuid)).willReturn(givenAccount);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/hs/accounts/accounts/{accountUuid}", givenAccountUuid)
                        .header("Authorization", bearer("abc-delete")));

        // then
        verify(contextMock).define();
        response.andExpect(status().isNoContent());
        verify(em).flush();
        verify(em).remove(givenAccount);
        verify(em).remove(givenSubject);
    }

    private static HsAccountEntity givenAccount(
            final UUID accountUuid,
            final String subjectName,
            final HsOfficePersonRealEntity person) {
        return HsAccountEntity.builder()
                .uuid(accountUuid)
                .subject(RealSubjectEntity.builder()
                        .uuid(accountUuid)
                        .name(subjectName)
                        .build())
                .person(person)
                .globalUid(30001)
                .globalGid(40001)
                .build();
    }

    private static HsOfficePersonRealEntity givenNaturalPerson(final String givenName, final String familyName) {
        return HsOfficePersonRealEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(NATURAL_PERSON)
                .givenName(givenName)
                .familyName(familyName)
                .build();
    }
}
