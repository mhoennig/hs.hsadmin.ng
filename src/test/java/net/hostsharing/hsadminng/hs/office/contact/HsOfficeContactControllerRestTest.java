package net.hostsharing.hsadminng.hs.office.contact;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeContactController.class)
@Import({ StrictMapper.class,
          HsOfficeContactFromResourceConverter.class,
          JsonObjectMapperConfiguration.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeContactControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsOfficeContactRbacRepository contactRepo;

    @Nested
    class GetListOfContacts {

        @Test
        void returnsContactsByCaption() throws Exception {
            // given
            when(contactRepo.findContactByOptionalCaptionLike("some"))
                    .thenReturn(List.of(givenContact("some contact")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/contacts?caption=some")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].caption", is("some contact")))
                    .andExpect(jsonPath("$[0].emailAddresses.main", is("some@example.org")));
        }

        @Test
        void returnsContactsByEmailAddress() throws Exception {
            // given
            when(contactRepo.findContactByEmailAddress("some@example.org"))
                    .thenReturn(List.of(givenContact("email contact")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/contacts?emailAddress=some@example.org")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].caption", is("email contact")));
        }
    }

    @Nested
    class GetSingleContact {

        @Test
        void returnsContactIfFound() throws Exception {
            // given
            val contactUuid = UUID.randomUUID();
            when(contactRepo.findByUuid(contactUuid)).thenReturn(Optional.of(givenContact(contactUuid, "some contact")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/contacts/" + contactUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(contactUuid.toString())))
                    .andExpect(jsonPath("caption", is("some contact")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val contactUuid = UUID.randomUUID();
            when(contactRepo.findByUuid(contactUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/contacts/" + contactUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewContact() throws Exception {
        // given
        val contactUuid = UUID.randomUUID();
        when(contactRepo.save(any(HsOfficeContactRbacEntity.class)))
                .thenReturn(givenContact(contactUuid, "new contact"));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/contacts")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "caption": "new contact",
                                    "emailAddresses": { "main": "new@example.org" }
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(contactUuid.toString())))
                .andExpect(jsonPath("caption", is("new contact")));
    }

    @Test
    void deletesContact() throws Exception {
        // given
        val contactUuid = UUID.randomUUID();
        when(contactRepo.deleteByUuid(contactUuid)).thenReturn(1);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/hs/office/contacts/" + contactUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());
    }

    @Test
    void patchesContact() throws Exception {
        // given
        val contactUuid = UUID.randomUUID();
        val current = givenContact(contactUuid, "old contact");
        when(contactRepo.findByUuid(contactUuid)).thenReturn(Optional.of(current));
        when(contactRepo.save(any(HsOfficeContactRbacEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/contacts/" + contactUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "caption": "patched contact"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("caption", is("patched contact")));
    }

    private HsOfficeContactRbacEntity givenContact(final String caption) {
        return givenContact(UUID.randomUUID(), caption);
    }

    private HsOfficeContactRbacEntity givenContact(final UUID uuid, final String caption) {
        return HsOfficeContactRbacEntity.builder()
                .uuid(uuid)
                .caption(caption)
                .emailAddresses(Map.of("main", "some@example.org"))
                .phoneNumbers(Map.of("phone_office", "+49 123 456789"))
                .build();
    }
}
