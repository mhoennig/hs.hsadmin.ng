package net.hostsharing.hsadminng.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BaseWebSecurityConfigRestTest.BadCredentialsController.class)
@Import({ MessagesResourceConfig.class,
          MessageTranslator.class,
          JsonObjectMapperConfiguration.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class BaseWebSecurityConfigRestTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void requestToAnArbitraryPathWhichIsNotApiShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/any-but-api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestToPathApiShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isNotFound()); // in this test's config actually no endpoints actually exist
    }

    @RestController
    static class BadCredentialsController {
    }
}
