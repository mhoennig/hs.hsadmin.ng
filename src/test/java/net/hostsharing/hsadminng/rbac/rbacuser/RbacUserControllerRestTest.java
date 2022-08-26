package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isValidUuid;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacUserController.class)
class RbacUserControllerRestTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    Context contextMock;
    @MockBean
    RbacUserRepository rbacUserRepository;

    @Test
    void createUserUsesGivenUuid() throws Exception {
        // given
        final var givenUuid = UUID.randomUUID();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac-users")
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
        verify(rbacUserRepository).create(argThat(entity -> entity.getUuid().equals(givenUuid)));
    }

    @Test
    void createUserGeneratesRandomUuidIfNotGiven() throws Exception {
        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", isValidUuid()));

        // then
        verify(rbacUserRepository).create(argThat(entity -> entity.getUuid() != null));
    }
}
