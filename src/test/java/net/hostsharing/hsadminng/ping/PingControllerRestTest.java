package net.hostsharing.hsadminng.ping;

import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.generated.api.v1.model.ApiVersionGetResponse200Resource;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.OffsetDateTime;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PingController.class)
@Import({ MessagesResourceConfig.class,
          MessageTranslator.class,
          JsonObjectMapperConfiguration.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class PingControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    VersionProvider versionProviderMock;

    @RequiredArgsConstructor
    enum I18nTestCases {
        EN("en", "pinged - in English"),
        DE("de", "pinged - auf Deutsch");

        final String language;
        final String expectedTranslation;
    }

    @ParameterizedTest
    @EnumSource(I18nTestCases.class)
    void pingReturnsPingedInRequestedLanguage(final I18nTestCases testCase) throws Exception {

        // when
        final var request = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/ping")
                        .header("Accept-Language", testCase.language)
                        .accept(MediaType.TEXT_PLAIN))
                .andDo(print());

        // then
        request
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith(testCase.expectedTranslation)));
    }

    @Test
    void pongReturnsPongedWithSubject() throws Exception {

        // when
        final var request = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/pong")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Accept-Language", "de")
                        .accept(MediaType.TEXT_PLAIN))
                .andDo(print());

        // then
        request
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hsh-alex_superuser")));
    }

    @Test
    void pongPostReturnsPongedWithSubject() throws Exception {

        // when
        final var request = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/pong")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Accept-Language", "de")
                        .accept(MediaType.TEXT_PLAIN))
                .andDo(print());

        // then
        request
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hsh-alex_superuser")));
    }

    @Test
    void versionReturnsVersionInfo() throws Exception {
        // given
        final var versionResponse = new ApiVersionGetResponse200Resource();
        versionResponse.setVersion("0.0.1-SNAPSHOT");
        versionResponse.setGroup("net.hostsharing");
        versionResponse.setArtifact("hsadmin-ng");
        versionResponse.setName("hsadmin-ng");
        versionResponse.setBuildTime(OffsetDateTime.parse("2026-07-10T17:34:23.309Z"));
        versionResponse.setBuildHost("test-build-host");
        versionResponse.setGitBranch("master");
        versionResponse.setGitCommit("0123456789abcdef0123456789abcdef01234567");
        versionResponse.setGitDirty(false);
        when(versionProviderMock.getVersion()).thenReturn(ResponseEntity.ok(versionResponse));

        // when
        final var request = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/version")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        // then
        request
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
                .andExpect(jsonPath("$.group").value("net.hostsharing"))
                .andExpect(jsonPath("$.artifact").value("hsadmin-ng"))
                .andExpect(jsonPath("$.name").value("hsadmin-ng"))
                .andExpect(jsonPath("$.buildTime").value("2026-07-10T17:34:23.309Z"))
                .andExpect(jsonPath("$.buildHost").value("test-build-host"))
                .andExpect(jsonPath("$.gitBranch").value("master"))
                .andExpect(jsonPath("$.gitCommit").value("0123456789abcdef0123456789abcdef01234567"))
                .andExpect(jsonPath("$.gitDirty").value(false));
    }
}
