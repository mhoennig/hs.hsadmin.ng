package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProvisioningApiKeyBootstrapUnitTest {

    private static final String CONFIGURED_API_KEY_HASH = ApiKey.hash("hsak_" + "0".repeat(64));

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void aMissingConfiguredApiKeyHashIsIgnored() {
        new ProvisioningApiKeyBootstrap(jdbcTemplate, " ").run(null);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void anInvalidConfiguredApiKeyHashFailsTheApplicationStart() {
        val bootstrapWithInvalidHash = new ProvisioningApiKeyBootstrap(jdbcTemplate, "no-hex-hash");

        assertThatThrownBy(() -> bootstrapWithInvalidHash.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hex-encoded SHA-256 hash");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void provisionsTheApiKeySubjectIfNoApiKeyIsStoredYet() {
        // given
        given(jdbcTemplate.queryForList(anyString(), eq(String.class))).willReturn(List.of());

        // when
        new ProvisioningApiKeyBootstrap(jdbcTemplate, CONFIGURED_API_KEY_HASH).run(null);

        // then the configured hash gets stored 1:1
        verify(jdbcTemplate).execute(contains(CONFIGURED_API_KEY_HASH));
    }

    @Test
    void keepsTheStoredApiKeyIfTheConfiguredHashIsAlreadyStored() {
        // given
        given(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .willReturn(List.of(CONFIGURED_API_KEY_HASH));

        // when
        new ProvisioningApiKeyBootstrap(jdbcTemplate, CONFIGURED_API_KEY_HASH).run(null);

        // then
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void keepsTheStoredApiKeyIfADifferentApiKeyIsStored() {
        // given
        given(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .willReturn(List.of(ApiKey.hash("hsak_" + "e".repeat(64))));

        // when
        new ProvisioningApiKeyBootstrap(jdbcTemplate, CONFIGURED_API_KEY_HASH).run(null);

        // then the stored API-key takes precedence
        verify(jdbcTemplate, never()).execute(anyString());
    }
}
