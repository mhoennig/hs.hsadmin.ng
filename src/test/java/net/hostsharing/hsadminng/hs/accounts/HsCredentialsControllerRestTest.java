package net.hostsharing.hsadminng.hs.accounts;

import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import org.hamcrest.CustomMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsCredentialsController.class)
@Import({
        StrictMapper.class,
        JsonObjectMapperConfiguration.class,
        DisableSecurityConfig.class,
        // HOWTO: test i18n translations
        MessagesResourceConfig.class,
        MessageTranslator.class })
@ActiveProfiles("test")
class HsCredentialsControllerRestTest {

    private static final UUID PERSON_UUID = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    RbacSubjectRepository subjectRepo;

    @MockitoBean
    HsOfficePersonRealRepository realPersonRepo;

    @MockitoBean
    HsOfficePersonRbacRepository rbacPersonRepo;

    @MockitoBean
    HsCredentialsContextRbacRepository loginContextRbacRepo;

    @MockitoBean
    HsCredentialsRepository credentialsRepo;

    @MockitoBean
    CredentialContextResourceToEntityMapper contextMapper;

    @Test
    void shouldFetchCurrentLoginUser() throws Exception {
        // given
        final UUID currentSubjectUuid = UUID.randomUUID();
        given(contextMock.fetchCurrentSubjectUuid()).willReturn(currentSubjectUuid);
        given(contextMock.isGlobalAdmin()).willReturn(true);
        given(subjectRepo.findByUuid(currentSubjectUuid)).willReturn(
                RbacSubjectEntity.builder().uuid(currentSubjectUuid).name("test-user").build()
        );
        given(credentialsRepo.findByUuid(currentSubjectUuid)).willReturn(
                Optional.of(HsCredentialsEntity.builder()
                        .uuid(currentSubjectUuid)
                        .person(HsOfficePersonRbacEntity.builder()
                                .uuid(PERSON_UUID)
                                .personType(NATURAL_PERSON)
                                .familyName("Miller")
                                .givenName("Steph")
                                .build())
                        .subject(RbacSubjectEntity.builder().name("steph-miller").build())
                        .build())
        );

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/current")
                        .header("Authorization", "Bearer test")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.uuid").value(currentSubjectUuid.toString()))
                .andExpect(jsonPath("$.subject.name").value("test-user"))
                .andExpect(jsonPath("$.person.uuid").value(PERSON_UUID.toString()))
                .andExpect(jsonPath("$.person.familyName").value("Miller"))
                .andExpect(jsonPath("$.person.givenName").value("Steph"))
                .andExpect(jsonPath("$.globalAdmin").value(true));
    }

    @Test
    void shouldFilterInvalidContextsRegardingNonNaturalPerson() throws Exception {
        // given
        final var givenCredentialsUuid = UUID.randomUUID();
        final var contextForNP = HsCredentialsContextRealEntity.builder()
                .uuid(UUID.randomUUID())
                .type("HSADMIN")
                .qualifier("prod")
                .onlyForNaturalPersons(true)
                .build();
        final var contextForAll = HsCredentialsContextRealEntity.builder()
                .uuid(UUID.randomUUID())
                .type("SSH")
                .qualifier("prod")
                .onlyForNaturalPersons(false)
                .build();
        final var credentialsEntity = HsCredentialsEntity.builder()
                .uuid(givenCredentialsUuid)
                .person(HsOfficePersonRbacEntity.builder()
                        .uuid(PERSON_UUID)
                        .personType(LEGAL_PERSON)
                        .build())
                .subject(RbacSubjectEntity.builder().name("some-nickname").build())
                .loginContexts(Set.of(contextForNP, contextForAll))
                .build();
        when(credentialsRepo.findByUuid(givenCredentialsUuid))
                .thenReturn(Optional.of(credentialsEntity));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/accounts/credentials/" + givenCredentialsUuid)
                        .header("Authorization", "Bearer test")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].type").value("SSH"))
                .andExpect(jsonPath("$.contexts[0].qualifier").value("prod"))
                .andExpect(jsonPath("$.contexts[0].onlyForNaturalPersons").value(false));
    }

    @Test
    void shouldRejectCreatingCredentialsForUnrepresentedPerson() throws Exception {
        // given
        final var personUuid = UUID.randomUUID();

        final AtomicReference<RbacSubjectEntity> createdSubject = new AtomicReference<>();
        given(subjectRepo.create(any())).willAnswer(invocation -> {
            final var passedEntity = (RbacSubjectEntity) invocation.getArgument(0);
            passedEntity.setUuid(UUID.randomUUID());
            createdSubject.set(passedEntity); // Capture the instance
            return passedEntity;
        });
        given(contextMock.fetchCurrentSubject()).willAnswer(invocation -> createdSubject.get().getName());
        given(subjectRepo.findByUuid(any())).willAnswer(invocation -> createdSubject.get());
        given(rbacPersonRepo.findByUuid(personUuid)).willReturn(Optional.of(
                HsOfficePersonRbacEntity.builder().uuid(personUuid).personType(NATURAL_PERSON).build()
        ));
        given(rbacPersonRepo.findPersonsrepresentedByPersonWithUuid(personUuid)).willReturn(List.of(
                // some persons, but not the one from the login-user itself
                HsOfficePersonRbacEntity.builder().uuid(UUID.randomUUID()).personType(NATURAL_PERSON).build(),
                HsOfficePersonRbacEntity.builder().uuid(UUID.randomUUID()).personType(LEGAL_PERSON).build()
        ));

        final var givenCredentialsUuid = UUID.randomUUID();

        final var contextForNP = HsCredentialsContextRealEntity.builder()
                .uuid(UUID.randomUUID())
                .type("HSADMIN")
                .qualifier("prod")
                .onlyForNaturalPersons(true)
                .build();
        final var contextForAll = HsCredentialsContextRealEntity.builder()
                .uuid(UUID.randomUUID())
                .type("SSH")
                .qualifier("prod")
                .onlyForNaturalPersons(false)
                .build();
        final var credentialsEntity = HsCredentialsEntity.builder()
                .uuid(givenCredentialsUuid)
                .person(HsOfficePersonRbacEntity.builder()
                        .uuid(PERSON_UUID)
                        .personType(LEGAL_PERSON)
                        .build())
                .subject(RbacSubjectEntity.builder().name("some-nickname").build())
                .loginContexts(Set.of(contextForNP, contextForAll))
                .build();
        when(credentialsRepo.findByUuid(givenCredentialsUuid))
                .thenReturn(Optional.of(credentialsEntity));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/credentials")
                        .header("Authorization", "Bearer test")
                        // HOWTO: test i18n translations
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                            "person.uuid": "${personUuid}",
                                            "nickname": "${nickname}",
                                            "active": true,
                                            "globalUid": 30001,
                                            "globalGid": 40001,
                                            "contexts": [
                                                {
                                                    "uuid" : "11111111-1111-1111-1111-111111111111",
                                                    "type" : "HSADMIN",
                                                    "qualifier" : "prod",
                                                    "onlyForNaturalPersons" : true,
                                                    "publicAccess" : true
                                                }
                                            ]
                                        }
                                        """
                                        .replace("${personUuid}", personUuid.toString())
                                        .replace("${nickname}", "new-user")
                        )
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", containsString(
                        "Zugriff verweigert: personUuid \"${personUuid}\" wird von der eingeloggten Person nicht repräsentiert"
                                .replace("${personUuid}", personUuid.toString()))));
    }

    @Test
    void markCredentialsAsUsed() throws Exception {

        // given
        final var givenCredentialsUuid = UUID.randomUUID();
        when(credentialsRepo.findByUuid(givenCredentialsUuid)).thenReturn(Optional.of(
                HsCredentialsEntity.builder()
                        .uuid(givenCredentialsUuid)
                        .person(HsOfficePersonRbacEntity.builder().uuid(PERSON_UUID).build())
                        .subject(RbacSubjectEntity.builder().name("some-nickname").build())
                        .lastUsed(null)
                        .onboardingToken("fake-onboarding-token")
                        .build()
        ));
        when(credentialsRepo.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/accounts/credentials/%{credentialsUuid}/used"
                                .replace("%{credentialsUuid}", givenCredentialsUuid.toString()))
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$", lenientlyEquals("""
                                {
                                    "uuid": "%{credentialsUuid}",
                                    "onboardingToken": null
                                }
                                """.replace("%{credentialsUuid}", givenCredentialsUuid.toString())
                        )))
                .andExpect(jsonPath("$.lastUsed").value(new CustomMatcher<String>("lastUsed should have recent timestamp") {

                    @Override
                    public boolean matches(final Object o) {
                        if (o == null) {
                            return false;
                        }
                        final var lastUsed = ZonedDateTime.parse(o.toString(), DateTimeFormatter.ISO_DATE_TIME)
                                .toLocalDateTime();
                        return lastUsed.isAfter(LocalDateTime.now().minusMinutes(1)) &&
                                lastUsed.isBefore(LocalDateTime.now());
                    }
                }));
    }
}
