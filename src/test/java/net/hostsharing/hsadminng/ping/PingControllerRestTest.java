package net.hostsharing.hsadminng.ping;

import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PingController.class)
@Import({ MessagesResourceConfig.class,
          MessageTranslator.class,
          JsonObjectMapperConfiguration.class,
          DisableSecurityConfig.class })
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
class PingControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @RequiredArgsConstructor
    enum I18nTestCases {
        EN("en", "pong anonymousUser - in English"),
        DE("de", "pong anonymousUser - auf Deutsch");

        final String language;
        final String expectedTranslation;
    }

    @ParameterizedTest
    @EnumSource(I18nTestCases.class)
    void pingReturnsPongInEnglish(final I18nTestCases testCase) throws Exception {

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
}
