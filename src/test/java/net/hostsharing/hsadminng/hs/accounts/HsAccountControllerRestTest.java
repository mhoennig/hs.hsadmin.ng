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
import net.hostsharing.hsadminng.rbac.subject.RealSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.SubjectType;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    RealSubjectRepository realSubjectRepo;

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
                // the subject UUID is always the same as the account UUID
                .andExpect(jsonPath("subject.uuid").value(givenAccountUuid.toString()))
                .andExpect(jsonPath("subject.name").value("abc-existing"))
                .andExpect(jsonPath("subject.type").value("USER"))
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
    void getListOfAccountsForCurrentSubjectReturnsAccountsOfSamePerson() throws Exception {
        // given
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenAccount = givenAccount(UUID.randomUUID(), "abc-current", givenPerson);
        val givenSamePersonAccount = givenAccount(UUID.randomUUID(), "abc-other", givenPerson);
        given(contextMock.hasGlobalAdminRole()).willReturn(false);
        given(accountRepo.findByCurrentSubject()).willReturn(java.util.List.of(givenAccount, givenSamePersonAccount));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("abc-current"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles(null);
        verify(accountRepo, never()).findAll();
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uuid").value(givenAccount.getUuid().toString()))
                .andExpect(jsonPath("$[0].subject.name").value("abc-current"))
                .andExpect(jsonPath("$[0].person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("$[1].uuid").value(givenSamePersonAccount.getUuid().toString()))
                .andExpect(jsonPath("$[1].subject.name").value("abc-other"))
                .andExpect(jsonPath("$[1].person.uuid").value(givenPerson.getUuid().toString()));
    }

    @Test
    void getListOfAccountsForGlobalAdminReturnsAllAccounts() throws Exception {
        // given
        val givenAccountAbc = givenAccount(UUID.randomUUID(), "abc-user", givenNaturalPerson("Abc", "Person"));
        val givenAccountXyz = givenAccount(UUID.randomUUID(), "xyz-user", givenNaturalPerson("Xyz", "Person"));
        given(contextMock.hasGlobalAdminRole()).willReturn(true);
        given(accountRepo.findAll()).willReturn(java.util.List.of(givenAccountAbc, givenAccountXyz));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-some_superuser"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles(null);
        verify(accountRepo, never()).findByCurrentSubject();
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].subject.name").value("abc-user"))
                .andExpect(jsonPath("$[1].subject.name").value("xyz-user"));
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
                .andExpect(jsonPath("$[0].subject.name").value("abc-person"))
                .andExpect(jsonPath("$[0].person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("$[1].uuid").value(givenAccountXyz.getUuid().toString()))
                .andExpect(jsonPath("$[1].subject.name").value("xyz-person"))
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
        given(realSubjectRepo.findCurrentSubject()).willReturn(RealSubjectEntity.builder()
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
    void getCurrentLoginUserWithoutOwnAccountReturnsSubjectWithoutPerson() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findCurrentSubject()).willReturn(RealSubjectEntity.builder()
                .uuid(givenSubjectUuid)
                .name("hsh-accountless_superuser")
                .build());
        // the current subject has no account:
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.empty());

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/current")
                        .header("Authorization", bearer("hsh-accountless_superuser"))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).define();
        response.andExpect(status().isOk())
                .andExpect(jsonPath("subject.uuid").value(givenSubjectUuid.toString()))
                .andExpect(jsonPath("subject.name").value("hsh-accountless_superuser"))
                .andExpect(jsonPath("person").doesNotExist())
                .andExpect(jsonPath("globalAdmin").value(true));
    }

    @Test
    void postNewAccountReferencingAnExistingUserSubjectUsesThatSubject() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenExistingSubjectUuid = UUID.randomUUID();
        val givenExistingSubject = RealSubjectEntity.builder()
                .uuid(givenExistingSubjectUuid)
                .name("abc-existinguser")
                .type(SubjectType.USER)
                .build();
        val givenPersonRole = new RbacRoleEntity();
        givenPersonRole.setUuid(UUID.randomUUID());
        val grantMock = mock(RbacGrantService.RbacRoleGranter.class);

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenExistingSubjectUuid))
                .willReturn(Optional.of(givenExistingSubject));
        given(accountRepo.findByUuid(givenExistingSubjectUuid)).willReturn(Optional.empty());
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(rbacRoleService.rbacRole(givenPerson, RbacRoleType.ADMIN)).willReturn(givenPersonRole);
        given(rbacGrantService.grant(givenPersonRole)).willReturn(grantMock);
        given(em.merge(givenExistingSubject)).willReturn(givenExistingSubject);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenExistingSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenExistingSubjectUuid.toString()))
                .andExpect(jsonPath("subject.name").value("abc-existinguser"))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()));

        verify(rbacSubjectRepo, never()).create(any());
        verify(contextMock).define("activate the account subject", null, "abc-existinguser", null);
        verify(grantMock).to(givenExistingSubject);
        verify(em).persist(any(HsAccountEntity.class));
    }

    @Test
    void postNewAccountWithNewSubjectCreatesThatSubject() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenNewSubjectUuid = UUID.randomUUID();
        val givenNewSubject = RealSubjectEntity.builder()
                .uuid(givenNewSubjectUuid)
                .name("abc-newuser")
                .type(SubjectType.USER)
                .build();
        val givenPersonRole = new RbacRoleEntity();
        givenPersonRole.setUuid(UUID.randomUUID());
        val grantMock = mock(RbacGrantService.RbacRoleGranter.class);

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenNewSubjectUuid)).willReturn(Optional.empty());
        given(rbacSubjectRepo.create(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(em.find(RealSubjectEntity.class, givenNewSubjectUuid)).willReturn(givenNewSubject);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(rbacRoleService.rbacRole(givenPerson, RbacRoleType.ADMIN)).willReturn(givenPersonRole);
        given(rbacGrantService.grant(givenPersonRole)).willReturn(grantMock);
        given(em.merge(givenNewSubject)).willReturn(givenNewSubject);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject": {
                                        "uuid": "%s",
                                        "name": "abc-newuser",
                                        "type": "USER"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenNewSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenNewSubjectUuid.toString()))
                .andExpect(jsonPath("subject.name").value("abc-newuser"))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("globalUid").value(30001))
                .andExpect(jsonPath("globalGid").value(40001));

        verify(rbacSubjectRepo).create(argThat(subject ->
                givenNewSubjectUuid.equals(subject.getUuid()) && "abc-newuser".equals(subject.getName())));
        verify(contextMock).define("activate the account subject", null, "abc-newuser", null);
        verify(grantMock).to(givenNewSubject);
        verify(em).persist(any(HsAccountEntity.class));
    }

    @Test
    void postNewAccountReferencingAnUnknownSubjectUuidIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenUnknownSubjectUuid = UUID.randomUUID();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenUnknownSubjectUuid)).willReturn(Optional.empty());
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenUnknownSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "no subject with UUID %s found".formatted(givenUnknownSubjectUuid))));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountWithoutAnySubjectReferenceIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "Exactly one of (subject, subject.uuid) must be non-null, but are (null, null)")));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountWithBothSubjectAndSubjectUuidIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "subject": {
                                        "uuid": "%s",
                                        "name": "abc-newuser"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), UUID.randomUUID(), UUID.randomUUID()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "Exactly one of (subject, subject.uuid) must be non-null")));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountWithNewSubjectWithoutUuidIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject": {
                                        "name": "abc-newuser"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "subject.uuid must not be null")));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountWithNewSubjectOfTypeGroupIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject": {
                                        "uuid": "%s",
                                        "name": "abc-newuser",
                                        "type": "GROUP"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), UUID.randomUUID()))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("GROUP")));

        verify(rbacSubjectRepo, never()).create(any());
    }

    @Test
    void postNewAccountWithSubjectUuidOfGroupSubjectIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenGroupSubjectUuid = UUID.randomUUID();
        val givenGroupSubject = RealSubjectEntity.builder()
                .uuid(givenGroupSubjectUuid)
                .name("/abc-Team")
                .type(SubjectType.GROUP)
                .build();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenGroupSubjectUuid))
                .willReturn(Optional.of(givenGroupSubject));
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenGroupSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "the subject %s is not a USER subject, but GROUP".formatted(givenGroupSubjectUuid))));

        verify(rbacGrantService, never()).grant(any());
    }

    @Test
    void postNewAccountWithNewSubjectWhichAlreadyExistsIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenExistingSubjectUuid = UUID.randomUUID();
        val givenExistingSubject = RealSubjectEntity.builder()
                .uuid(givenExistingSubjectUuid)
                .name("abc-existinguser")
                .type(SubjectType.USER)
                .build();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenExistingSubjectUuid))
                .willReturn(Optional.of(givenExistingSubject));
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject": {
                                        "uuid": "%s",
                                        "name": "abc-existinguser"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenExistingSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("message", containsString(
                        "a subject with UUID %s already exists".formatted(givenExistingSubjectUuid))));

        verify(rbacSubjectRepo, never()).create(any());
        verify(rbacGrantService, never()).grant(any());
    }

    @Test
    void postNewAccountForSubjectWhichAlreadyHasAnAccountIsRejected() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("Test", "Person");
        val givenExistingSubjectUuid = UUID.randomUUID();
        val givenExistingSubject = RealSubjectEntity.builder()
                .uuid(givenExistingSubjectUuid)
                .name("abc-existinguser")
                .type(SubjectType.USER)
                .build();

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenExistingSubjectUuid))
                .willReturn(Optional.of(givenExistingSubject));
        given(accountRepo.findByUuid(givenExistingSubjectUuid))
                .willReturn(Optional.of(givenAccount(givenExistingSubjectUuid, "abc-existinguser", givenPerson)));
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenExistingSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("message", containsString(
                        "the subject %s already has an account".formatted(givenExistingSubjectUuid))));

        verify(rbacGrantService, never()).grant(any());
    }

    @Test
    void postNewAccountWithExistingSubjectAndNewPersonCreatesAccountAndPerson() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenPerson = givenNaturalPerson("New", "Person");
        val givenExistingSubjectUuid = UUID.randomUUID();
        val givenExistingSubject = RealSubjectEntity.builder()
                .uuid(givenExistingSubjectUuid)
                .name("abc-existinguser")
                .type(SubjectType.USER)
                .build();
        val givenPersonRole = new RbacRoleEntity();
        givenPersonRole.setUuid(UUID.randomUUID());
        val grantMock = mock(RbacGrantService.RbacRoleGranter.class);

        given(contextMock.fetchCurrentSubjectUuid()).willReturn(givenSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(realSubjectRepo.findVisibleSubjectByUuid(givenExistingSubjectUuid))
                .willReturn(Optional.of(givenExistingSubject));
        given(accountRepo.findByUuid(givenExistingSubjectUuid)).willReturn(Optional.empty());
        given(realPersonRepo.save(any())).willReturn(givenPerson);
        given(realPersonRepo.findByUuid(givenPerson.getUuid())).willReturn(Optional.of(givenPerson));
        given(rbacRoleService.rbacRole(givenPerson, RbacRoleType.ADMIN)).willReturn(givenPersonRole);
        given(rbacGrantService.grant(givenPersonRole)).willReturn(grantMock);
        given(em.merge(givenExistingSubject)).willReturn(givenExistingSubject);

        // when
        val response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/accounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person": {
                                        "personType": "NATURAL_PERSON",
                                        "givenName": "New",
                                        "familyName": "Person"
                                    },
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenExistingSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenExistingSubjectUuid.toString()))
                .andExpect(jsonPath("person.uuid").value(givenPerson.getUuid().toString()))
                .andExpect(jsonPath("person.givenName").value("New"))
                .andExpect(jsonPath("person.familyName").value("Person"));

        verify(realPersonRepo).save(any());
        verify(rbacSubjectRepo, never()).create(any());
        verify(grantMock).to(givenExistingSubject);
    }

    // that a non-global-admin cannot create accounts is rejected by context.assumeRoles at database level,
    // verified by HsAccountControllerAcceptanceTest.shouldRejectCreatingAccountIfNotGlobalAdmin

    @Test
    void postNewAccountSucceedsEvenIfActingGlobalAdminHasNoOwnAccount() throws Exception {
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
        // the acting global admin itself has no account:
        given(accountRepo.findByUuid(givenSubjectUuid)).willReturn(Optional.empty());
        given(realSubjectRepo.findVisibleSubjectByUuid(givenNewSubjectUuid)).willReturn(Optional.empty());
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
                        .header("Authorization", bearer("hsh-accountless_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject": {
                                        "uuid": "%s",
                                        "name": "abc-newuser"
                                    },
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPerson.getUuid(), givenNewSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("uuid").value(givenNewSubjectUuid.toString()))
                .andExpect(jsonPath("subject.name").value("abc-newuser"));

        verify(grantMock).to(givenNewSubject);
        verify(em).persist(any(HsAccountEntity.class));
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
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPersonUuid, givenNewSubjectUuid))
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
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenNewSubjectUuid))
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
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "person.uuid": "%s",
                                    "subject.uuid": "%s",
                                    "globalUid": 30001,
                                    "globalGid": 40001
                                }
                                """.formatted(givenPersonUuid, givenNewSubjectUuid))
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
                        .type(SubjectType.USER)
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
