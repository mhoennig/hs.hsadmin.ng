package net.hostsharing.hsadminng.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FakeJwtController.class)
@Import({ MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class FakeJwtControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void tokenReturnsBearerTokenForGivenUsernameAndScope() throws Exception {
        // given
        final var givenUsername = "test-user@hostsharing.net";

        // when
        final var request = mockMvc.perform(MockMvcRequestBuilders
                        .post("/fake-jwt/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", givenUsername)
                        .param("password", "ignored")
                        .param("scope", "openid profile email")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        final var response = request.andExpect(status().isOk())
                .andExpect(jsonPath("access_token", not(startsWith("Bearer "))))
                .andExpect(jsonPath("token_type").value("Bearer"))
                .andExpect(jsonPath("expires_in").value(3600))
                .andExpect(jsonPath("scope").value("openid profile email"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final var responseBody = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        final var accessToken = (String) responseBody.get("access_token");
        final var jwt = SignedJWT.parse(accessToken);

        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(givenUsername);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("http://test-issuer");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("api");
    }
}
