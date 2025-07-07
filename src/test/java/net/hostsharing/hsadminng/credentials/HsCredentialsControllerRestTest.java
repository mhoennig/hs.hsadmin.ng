package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
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
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsCredentialsController.class)
@Import({ StrictMapper.class, JsonObjectMapperConfiguration.class, DisableSecurityConfig.class, MessageTranslator.class })
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
    void patchCredentialsUsed() throws Exception {

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
                        .post("/api/hs/credentials/credentials/%{credentialsUuid}/used"
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
