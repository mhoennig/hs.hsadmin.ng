package net.hostsharing.hsadminng.ping;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                HsadminNgApplication.class,
                DisableSecurityConfig.class,
                JpaAttempt.class
        }
)
@ActiveProfiles("test")
@Transactional
@Tag("generalIntegrationTest")
class PingControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    enum PingTranslationTestCase {
        EN(Locale.ENGLISH, "pong superuser-alex@hostsharing.net - in English"),
        DE(Locale.GERMAN, "pong superuser-alex@hostsharing.net - auf Deutsch"),
        FR(Locale.FRENCH, "pong superuser-alex@hostsharing.net - in English [fr translation missing]");

        Locale givenLocale;
        CharSequence expectedPongTranslation;

        PingTranslationTestCase(final Locale givenLocale, final String expectedPongTranslation) {
            this.givenLocale = givenLocale;
            this.expectedPongTranslation = expectedPongTranslation;
        }
    }

    @ParameterizedTest
    @EnumSource(PingTranslationTestCase.class)
    void pingRepliesWithTranslatedPongResponse(final PingTranslationTestCase testCase) {
        final var responseBody = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .header("Accept-Language", testCase.givenLocale)
                    .port(port)
                .when()
                    .get("http://localhost/api/ping")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("text/plain;charset=UTF-8")
                    .extract().body().asString();
                // @formatter:on

        assertThat(responseBody).isEqualTo(testCase.expectedPongTranslation + "\n");
    }
}
