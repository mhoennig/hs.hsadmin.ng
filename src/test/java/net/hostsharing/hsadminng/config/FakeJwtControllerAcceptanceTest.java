package net.hostsharing.hsadminng.config;

import com.nimbusds.jwt.SignedJWT;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
class FakeJwtControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Test
    // Guards the real servlet form-parameter parsing, which `FakeJwtControllerRestTest` cannot cover:
    // MockMvc sets parameters directly on the mock request, but on a real server the container parses
    // them from the request body stream, which must not be consumed by any filter beforehand.
    void tokenEndpointHonorsFormEncodedUsername() throws Exception {

        // when a token is requested with form-encoded parameters, like from a real OAuth client
        // @formatter:off
        final String accessToken = RestAssured
                .given()
                    .contentType(ContentType.URLENC)
                    .formParam("username", "xyz-form_user")
                    .formParam("password", "ignored")
                    .port(port)
                .when()
                    .post("http://localhost/fake-jwt/token")
                .then().assertThat()
                    .statusCode(200)
                    .body("token_type", is("Bearer"))
                    .extract().path("access_token");
        // @formatter:on

        // then the issued token carries the given username as subject
        assertThat(SignedJWT.parse(accessToken).getJWTClaimsSet().getSubject())
                .isEqualTo("xyz-form_user");
    }
}
