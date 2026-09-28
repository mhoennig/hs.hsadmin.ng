package net.hostsharing.hsadminng.config;

import com.nimbusds.jwt.SignedJWT;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @MockitoBean
    FakeJwtSelectableUsers selectableUsers;

    @Test
    void tokenReturnsBearerTokenForGivenUsernameAndScope() throws Exception {
        // given
        val givenUsername = "test-user@hostsharing.net";

        // when
        val request = mockMvc.perform(MockMvcRequestBuilders
                        .post("/fake-jwt/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", givenUsername)
                        .param("password", "ignored")
                        .param("scope", "openid profile email")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        val response = request.andExpect(status().isOk())
                .andExpect(jsonPath("access_token", not(startsWith("Bearer "))))
                .andExpect(jsonPath("token_type").value("Bearer"))
                .andExpect(jsonPath("expires_in").value(3600))
                .andExpect(jsonPath("scope").value("openid profile email"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        val responseBody = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        val accessToken = (String) responseBody.get("access_token");
        val jwt = SignedJWT.parse(accessToken);

        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(givenUsername);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("http://test-issuer");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("api");
    }

    @Test
    void discoveryDocumentAndJwksAreServedUnderTheWellKnownPaths() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/fake-jwt/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("issuer").value("http://localhost:8080/fake-jwt"))
                .andExpect(jsonPath("authorization_endpoint").value("http://localhost:8080/fake-jwt/auth"))
                .andExpect(jsonPath("token_endpoint").value("http://localhost:8080/fake-jwt/token"));

        mockMvc.perform(MockMvcRequestBuilders.get("/fake-jwt/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("keys[0].kid").value("test-key"));
    }

    @Test
    void authorizationEndpointServesTheLoginFormAndRedeemsThePostedLogin() throws Exception {
        // the OIDC client redirects the browser to the authorization endpoint,
        // which serves the login form offering the existing users
        when(selectableUsers.findAllActiveUserSubjectNames())
                .thenReturn(List.of("hsh-demo_admin", "xyz-test_user"));
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/fake-jwt/auth")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "test-client")
                        .queryParam("redirect_uri", "http://localhost:3000/callback")
                        .queryParam("state", "some-state"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<select name=\"username\"")))
                .andExpect(content().string(containsString("<option>hsh-demo_admin</option>")))
                .andExpect(content().string(containsString("<option>xyz-test_user</option>")))
                .andExpect(content().string(not(containsString("type=\"password\""))));

        // the submitted login form answers with the redirect carrying the authorization code
        val location = mockMvc.perform(MockMvcRequestBuilders
                        .post("/fake-jwt/auth")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("response_type", "code")
                        .param("client_id", "test-client")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("state", "some-state")
                        .param("username", "form-user"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).startsWith("http://localhost:3000/callback?code=").contains("&state=some-state");

        // the code redeems for tokens including an id_token
        val code = location.substring(location.indexOf("code=") + 5, location.indexOf("&state="));
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/fake-jwt/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", "test-client")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists())
                .andExpect(jsonPath("id_token").exists());
    }

    @Test
    void loginFormFallsBackToFreeTextUsernameWithoutAnyUsersInTheDatabase() throws Exception {
        when(selectableUsers.findAllActiveUserSubjectNames()).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/fake-jwt/auth")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "test-client")
                        .queryParam("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<input type=\"text\" name=\"username\"")))
                .andExpect(content().string(not(containsString("<select"))));
    }
}
