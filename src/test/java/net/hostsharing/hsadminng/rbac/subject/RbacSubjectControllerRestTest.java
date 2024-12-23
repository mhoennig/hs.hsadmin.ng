package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacSubjectController.class)
@Import({StandardMapper.class, DisableSecurityConfig.class})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class RbacSubjectControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    RbacSubjectRepository rbacSubjectRepository;

    @MockBean
    EntityManagerWrapper em;


    @Test
    void postNewSubjectUsesGivenUuid() throws Exception {
        // given
        final var givenUuid = UUID.randomUUID();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "uuid": "%s"
                                }
                                """.formatted(givenUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(givenUuid.toString())));

        // then
        verify(rbacSubjectRepository).create(argThat(entity -> entity.getUuid().equals(givenUuid)));
    }

    @Test
    void postNewSubjectGeneratesRandomUuidIfNotGiven() throws Exception {
        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", isUuidValid()));

        // then
        verify(rbacSubjectRepository).create(argThat(entity -> entity.getUuid() != null));
    }
}
