package net.hostsharing.hsadminng.config;

import io.restassured.RestAssured;
import lombok.val;
import net.hostsharing.hsadminng.HsadminNgApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static net.hostsharing.hsadminng.config.ProvisioningApiKeyBootstrap.HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class,
        properties = "hsadminng.security.provisioning-api-key-sha256="
                + ProvisioningApiKeyBootstrapAcceptanceTest.CONFIGURED_PROVISIONING_API_KEY_SHA256)
@ActiveProfiles("fake-jwt")
class ProvisioningApiKeyBootstrapAcceptanceTest {

    // only the client knows the clear-text API-key ...
    static final String CONFIGURED_PROVISIONING_API_KEY =
            "hsak_0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    // ... the server environment just gets its SHA-256 hash, see the consistency test below
    static final String CONFIGURED_PROVISIONING_API_KEY_SHA256 =
            "6634030b6bfaaa68215048ec4822798e269e57096735d4986249e6eb412f26b8";

    @LocalServerPort
    private Integer port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ProvisioningApiKeyBootstrap bootstrap;

    @Test
    void configuredHashMatchesTheClearTextApiKey() {
        // keep the hard-coded constants (needed as compile-time constants in the annotation) in sync
        assertThat(ApiKey.hash(CONFIGURED_PROVISIONING_API_KEY)).isEqualTo(CONFIGURED_PROVISIONING_API_KEY_SHA256);
    }

    @Test
    void aFurtherApplicationStartKeepsTheStoredApiKey() {
        // when the bootstrap runs again, like on a further application start
        bootstrap.run(null);

        // then there is still exactly the one API-key with the configured hash
        assertThat(storedKeyHashes()).containsExactly(CONFIGURED_PROVISIONING_API_KEY_SHA256);
    }

    @Test
    void aDifferentConfiguredApiKeyHashDoesNotReplaceTheStoredApiKey() {
        // when the bootstrap runs with a different configured API-key hash
        val differentApiKey = "hsak_" + "f".repeat(64);
        new ProvisioningApiKeyBootstrap(jdbcTemplate, ApiKey.hash(differentApiKey)).run(null);

        // then the stored API-key takes precedence ...
        assertThat(storedKeyHashes()).containsExactly(CONFIGURED_PROVISIONING_API_KEY_SHA256);

        // ... the different API-key does not authenticate ...
        // @formatter:off
        RestAssured
                .given()
                    .header("Hostsharing-Api-Key", differentApiKey)
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/accounts/current")
                .then().assertThat()
                    .statusCode(401);

        // ... but the stored API-key still does
        RestAssured
                .given()
                    .header("Hostsharing-Api-Key", CONFIGURED_PROVISIONING_API_KEY)
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/accounts/current")
                .then().assertThat()
                    .statusCode(200);
        // @formatter:on
    }

    @Test
    void aMissingConfiguredApiKeyHashIsIgnored() {
        new ProvisioningApiKeyBootstrap(jdbcTemplate, " ").run(null);

        assertThat(storedKeyHashes()).containsExactly(CONFIGURED_PROVISIONING_API_KEY_SHA256);
    }

    @Test
    void anInvalidConfiguredApiKeyHashFailsTheApplicationStart() {
        val bootstrapWithInvalidHash = new ProvisioningApiKeyBootstrap(jdbcTemplate, "no-hex-hash");

        assertThatThrownBy(() -> bootstrapWithInvalidHash.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hex-encoded SHA-256 hash");
    }

    private List<String> storedKeyHashes() {
        return jdbcTemplate.queryForList("""
                select ak.keyHash
                    from rbac.api_key ak
                    join rbac.subject s on s.uuid = ak.uuid
                    where s.name = '%s'
                """.formatted(HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME), String.class);
    }
}
