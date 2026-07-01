package net.hostsharing.hsadminng.hs.booking.project;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsBookingProjectController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsBookingProjectControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsBookingProjectRbacRepository bookingProjectRepo;

    @MockitoBean
    HsBookingDebitorRepository debitorRepo;

    @Nested
    class GetListOfBookingProjects {

        @Test
        void returnsAllBookingProjectsWithoutDebitorFilter() throws Exception {
            // given
            when(bookingProjectRepo.findAll()).thenReturn(List.of(givenProject(UUID.randomUUID(), "default project")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/projects")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].caption", is("default project")));
        }

        @Test
        void returnsBookingProjectsByDebitorUuid() throws Exception {
            // given
            val debitorUuid = UUID.randomUUID();
            when(bookingProjectRepo.findAllByDebitorUuid(debitorUuid))
                    .thenReturn(List.of(givenProject(UUID.randomUUID(), "filtered project")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/projects?debitorUuid=" + debitorUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].caption", is("filtered project")));
        }
    }

    @Nested
    class GetSingleBookingProject {

        @Test
        void returnsProjectIfFound() throws Exception {
            // given
            val projectUuid = UUID.randomUUID();
            when(bookingProjectRepo.findByUuid(projectUuid)).thenReturn(Optional.of(givenProject(projectUuid, "some project")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/projects/" + projectUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(projectUuid.toString())))
                    .andExpect(jsonPath("caption", is("some project")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val projectUuid = UUID.randomUUID();
            when(bookingProjectRepo.findByUuid(projectUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/projects/" + projectUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewBookingProject() throws Exception {
        // given
        val debitorUuid = UUID.randomUUID();
        val projectUuid = UUID.randomUUID();
        val debitor = givenDebitor(debitorUuid);
        when(debitorRepo.findByUuid(debitorUuid)).thenReturn(Optional.of(debitor));
        when(bookingProjectRepo.save(any(HsBookingProjectRbacEntity.class))).thenAnswer(invocation -> {
            final HsBookingProjectRbacEntity project = invocation.getArgument(0);
            project.setUuid(projectUuid);
            return project;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/booking/projects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "debitor.uuid": "%s",
                                    "caption": "new project"
                                }
                                """.formatted(debitorUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(projectUuid.toString())))
                .andExpect(jsonPath("caption", is("new project")));
    }

    @Nested
    class DeleteBookingProject {

        @Test
        void respondsNoContentIfDeleted() throws Exception {
            // given
            val projectUuid = UUID.randomUUID();
            when(bookingProjectRepo.deleteByUuid(projectUuid)).thenReturn(1);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/booking/projects/" + projectUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNoContent());
        }

        @Test
        void respondsNotFoundIfMissing() throws Exception {
            // given
            val projectUuid = UUID.randomUUID();
            when(bookingProjectRepo.deleteByUuid(projectUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/booking/projects/" + projectUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void patchesBookingProject() throws Exception {
        // given
        val projectUuid = UUID.randomUUID();
        when(bookingProjectRepo.findByUuid(projectUuid)).thenReturn(Optional.of(givenProject(projectUuid, "old project")));
        when(bookingProjectRepo.save(any(HsBookingProjectRbacEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/booking/projects/" + projectUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "caption": "patched project"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(projectUuid.toString())))
                .andExpect(jsonPath("caption", is("patched project")));
    }

    private HsBookingProjectRbacEntity givenProject(final UUID projectUuid, final String caption) {
        return HsBookingProjectRbacEntity.builder()
                .uuid(projectUuid)
                .debitor(givenDebitor(UUID.randomUUID()))
                .caption(caption)
                .build();
    }

    private HsBookingDebitorEntity givenDebitor(final UUID debitorUuid) {
        return HsBookingDebitorEntity.builder()
                .uuid(debitorUuid)
                .debitorNumber(1234500)
                .defaultPrefix("abc")
                .build();
    }
}
