package net.hostsharing.hsadminng.hs.scenarios;

import net.hostsharing.hsadminng.config.ApiKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioTestUnitTest {

    @Test
    void provisioningApiKeyHashConstantMatchesTheClearTextApiKeyConstant() {
        // keep the hard-coded constants (needed as compile-time constants in the annotation) in sync
        assertThat(ApiKey.hash(ScenarioTest.PROVISIONING_API_KEY)).isEqualTo(ScenarioTest.PROVISIONING_API_KEY_SHA256);
    }
}
