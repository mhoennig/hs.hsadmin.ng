package net.hostsharing.hsadminng.ping;

import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
}
