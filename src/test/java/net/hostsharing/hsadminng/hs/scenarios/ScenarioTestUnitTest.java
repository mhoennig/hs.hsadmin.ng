package net.hostsharing.hsadminng.hs.scenarios;

import net.hostsharing.hsadminng.config.ApiKey;
import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

class ScenarioTestUnitTest {

    @Test
    void provisioningApiKeyHashConstantMatchesTheClearTextApiKeyConstant() {
        // keep the hard-coded constants (needed as compile-time constants in the annotation) in sync
        assertThat(ApiKey.hash(ScenarioTest.PROVISIONING_API_KEY)).isEqualTo(ScenarioTest.PROVISIONING_API_KEY_SHA256);
    }

    @Test
    void keptValueIsKnownAndResolvable() {
        // when
        ScenarioTest.putKeptValue("ApiKey: kept.value.test", "hsak_kept.value.test");

        // then
        assertThat(ScenarioTest.knowVariables()).containsEntry("ApiKey: kept.value.test", "hsak_kept.value.test");
        assertThat(ScenarioTest.resolve("%{ApiKey: kept.value.test}", DROP_COMMENTS)).isEqualTo("hsak_kept.value.test");
    }
}
