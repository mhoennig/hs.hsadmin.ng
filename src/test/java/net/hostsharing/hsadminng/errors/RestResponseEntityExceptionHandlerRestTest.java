package net.hostsharing.hsadminng.errors;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RestResponseEntityExceptionHandlerRestTest.TestController.class)
@Import({ JsonObjectMapperConfiguration.class,
          MessageTranslator.class,
          RestResponseEntityExceptionHandler.class,
          RestResponseEntityExceptionHandlerRestTest.TestConfig.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class RestResponseEntityExceptionHandlerRestTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test/exception")
        public ResponseEntity<String> testEndpoint() {
            throw new AccessDeniedException("Access is denied");
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void handleAccessDeniedExceptionReturnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/test/exception"))
                .andExpect(status().isUnauthorized());
    }
}
