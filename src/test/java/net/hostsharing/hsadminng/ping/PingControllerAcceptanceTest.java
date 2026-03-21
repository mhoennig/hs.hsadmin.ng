package net.hostsharing.hsadminng.ping;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;

@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
@TestPropertySource(properties = {
        "server.port=0",
        "logging.level.root=DEBUG"
})
@Transactional
class PingControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    enum PongTranslationTestCase {
        EN(Locale.ENGLISH, "ponged superuser-alex@hostsharing.net - in English"),
        DE(Locale.GERMAN, "ponged superuser-alex@hostsharing.net - auf Deutsch");

        Locale givenLocale;
        CharSequence expectedPongTranslation;

        PongTranslationTestCase(final Locale givenLocale, final String expectedPongTranslation) {
            this.givenLocale = givenLocale;
            this.expectedPongTranslation = expectedPongTranslation;
        }
    }

    @ParameterizedTest
    @EnumSource(PongTranslationTestCase.class)
    void pongRepliesWithTranslatedPongResponse(final PongTranslationTestCase testCase) {
        final var responseBody = RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                    .header("Accept-Language", testCase.givenLocale)
                    .port(port)
                .when()
                    .get("http://localhost/api/pong")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("text/plain;charset=UTF-8")
                    .extract().body().asString();
        // @formatter:on

        assertThat(responseBody).isEqualTo(testCase.expectedPongTranslation + "\n");
    }

    @Test
    void pongPostRepliesWithTranslatedPongResponse() {
        final var responseBody = RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                    .header("Accept-Language", Locale.GERMAN)
                    .port(port)
                .when()
                    .post("http://localhost/api/pong")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("text/plain;charset=UTF-8")
                    .extract().body().asString();
        // @formatter:on

        assertThat(responseBody).isEqualTo("ponged superuser-alex@hostsharing.net - auf Deutsch\n");
    }

    @Test
    void pingRepliesWithTranslatedPongResponse() {
        final var responseBody = RestAssured // @formatter:off
                .given()
                    .header("Accept-Language", Locale.GERMAN)
                    .port(port)
                .when()
                    .get("http://localhost/api/ping")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("text/plain;charset=UTF-8")
                    .extract().body().asString();
        // @formatter:on

        assertThat(responseBody).isEqualTo("pinged - auf Deutsch\n");
    }
}
