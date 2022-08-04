package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
class PackageControllerRestTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    Context contextMock;
    @MockBean
    PackageRepository packageRepositoryMock;

    @Test
    void listPackagesWithoutNameParameter() throws Exception {

        // given
        final var givenPacs = List.of(TestPackage.xxx00, TestPackage.xxx01, TestPackage.xxx02);
        when(packageRepositoryMock.findAllByOptionalNameLike(null)).thenReturn(givenPacs);

        // when
        final var pacs = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/packages")
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "customer#xxx.admin")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].name", is("xxx00")))
            .andExpect(jsonPath("$[1].uuid", is(TestPackage.xxx01.getUuid().toString())))
            .andExpect(jsonPath("$[2].customer.prefix", is("xxx")));

        verify(contextMock).setCurrentUser("mike@hostsharing.net");
        verify(contextMock).assumeRoles("customer#xxx.admin");
    }

    @Test
    void listPackagesWithNameParameter() throws Exception {

        // given
        final var givenPacs = List.of(TestPackage.xxx01);
        when(packageRepositoryMock.findAllByOptionalNameLike("xxx01")).thenReturn(givenPacs);

        // when
        final var pacs = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/packages?name=xxx01")
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "customer#xxx.admin")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("xxx01")));

        verify(contextMock).setCurrentUser("mike@hostsharing.net");
        verify(contextMock).assumeRoles("customer#xxx.admin");
    }
}
