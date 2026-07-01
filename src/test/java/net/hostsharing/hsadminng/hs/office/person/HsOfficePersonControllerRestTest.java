package net.hostsharing.hsadminng.hs.office.person;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficePersonController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficePersonControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsOfficePersonRbacRepository personRepo;

    @Nested
    class GetListOfPersons {

        @Test
        void returnsPersonsByNameAndType() throws Exception {
            // given
            when(personRepo.findPersonByOptionalNameLike("miller"))
                    .thenReturn(List.of(
                            givenPerson("Miller GmbH", LEGAL_PERSON),
                            givenPerson("Miller", "Mila", NATURAL_PERSON)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/persons?name=miller&type=LEGAL_PERSON")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].personType", is("LEGAL_PERSON")))
                    .andExpect(jsonPath("$[0].tradeName", is("Miller GmbH")));
        }

        @Test
        void returnsPersonsRepresentedByPerson() throws Exception {
            // given
            val representativeUuid = UUID.randomUUID();
            when(personRepo.findPersonsRepresentedByPersonWithUuid(representativeUuid))
                    .thenReturn(List.of(givenPerson("Represented GmbH", LEGAL_PERSON)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/persons?representedByPersonUuid=" + representativeUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].tradeName", is("Represented GmbH")));
        }
    }

    @Nested
    class GetSinglePerson {

        @Test
        void returnsPersonIfFound() throws Exception {
            // given
            val personUuid = UUID.randomUUID();
            when(personRepo.findByUuid(personUuid)).thenReturn(Optional.of(givenPerson(personUuid, "Miller GmbH")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/persons/" + personUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(personUuid.toString())))
                    .andExpect(jsonPath("tradeName", is("Miller GmbH")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val personUuid = UUID.randomUUID();
            when(personRepo.findByUuid(personUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/persons/" + personUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewPerson() throws Exception {
        // given
        val personUuid = UUID.randomUUID();
        when(personRepo.save(any(HsOfficePersonRbacEntity.class)))
                .thenReturn(givenPerson(personUuid, "New GmbH"));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/persons")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personType": "LEGAL_PERSON",
                                    "tradeName": "New GmbH"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(personUuid.toString())))
                .andExpect(jsonPath("tradeName", is("New GmbH")));
    }

    @Test
    void deletesPerson() throws Exception {
        // given
        val personUuid = UUID.randomUUID();
        when(personRepo.deleteByUuid(personUuid)).thenReturn(1);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/hs/office/persons/" + personUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());
    }

    @Test
    void patchesPerson() throws Exception {
        // given
        val personUuid = UUID.randomUUID();
        val current = givenPerson(personUuid, "Old GmbH");
        when(personRepo.findByUuid(personUuid)).thenReturn(Optional.of(current));
        when(personRepo.save(any(HsOfficePersonRbacEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/persons/" + personUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tradeName": "Patched GmbH"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("tradeName", is("Patched GmbH")));
    }

    private HsOfficePersonRbacEntity givenPerson(final String tradeName, final HsOfficePersonType personType) {
        return HsOfficePersonRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(personType)
                .tradeName(tradeName)
                .build();
    }

    private HsOfficePersonRbacEntity givenPerson(final String familyName, final String givenName, final HsOfficePersonType personType) {
        return HsOfficePersonRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(personType)
                .familyName(familyName)
                .givenName(givenName)
                .build();
    }

    private HsOfficePersonRbacEntity givenPerson(final UUID uuid, final String tradeName) {
        return HsOfficePersonRbacEntity.builder()
                .uuid(uuid)
                .personType(LEGAL_PERSON)
                .tradeName(tradeName)
                .build();
    }
}
