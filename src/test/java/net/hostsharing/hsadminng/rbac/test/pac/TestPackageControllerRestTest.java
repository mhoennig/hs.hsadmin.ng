package net.hostsharing.hsadminng.rbac.test.pac;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerEntity;
import lombok.val;
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
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestPackageController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class TestPackageControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    TestPackageRepository testPackageRepository;

    @MockitoBean
    EntityManagerWrapper em;

    @Test
    void listPackagesReturnsPackagesFromRepository() throws Exception {
        // given
        val customerUuid = UUID.randomUUID();
        val packageUuid = UUID.randomUUID();
        given(testPackageRepository.findAllByOptionalNameLike("pac")).willReturn(List.of(
                new TestPackageEntity(
                        packageUuid,
                        0,
                        new TestCustomerEntity(customerUuid, 0, "xxx", 10001, "admin@example.org"),
                        "pac00",
                        "some package")));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/test/packages?name=pac")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid", is(packageUuid.toString())))
                .andExpect(jsonPath("$[0].customer.uuid", is(customerUuid.toString())))
                .andExpect(jsonPath("$[0].name", is("pac00")))
                .andExpect(jsonPath("$[0].description", is("some package")));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
    }

    @Test
    void updatePackagePatchesDescription() throws Exception {
        // given
        val packageUuid = UUID.randomUUID();
        val current = new TestPackageEntity(
                packageUuid,
                0,
                new TestCustomerEntity(UUID.randomUUID(), 0, "xxx", 10001, "admin@example.org"),
                "pac00",
                "old description");
        given(testPackageRepository.findByUuid(packageUuid)).willReturn(current);
        given(testPackageRepository.save(current)).willAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/test/packages/{packageUuid}", packageUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "new description"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(packageUuid.toString())))
                .andExpect(jsonPath("description", is("new description")));

        // then
        verify(testPackageRepository).save(argThat(entity ->
                entity.getUuid().equals(packageUuid) &&
                        entity.getDescription().equals("new description")));
    }
}
