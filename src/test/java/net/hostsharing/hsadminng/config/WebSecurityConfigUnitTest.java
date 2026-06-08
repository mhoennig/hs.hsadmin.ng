package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigUnitTest {

    @Test
    void isProductionSecurityConfig() {
        // when
        val config = new WebSecurityConfig();

        // then
        assertThat(config).isInstanceOf(BaseWebSecurityConfig.class);
        assertThat(WebSecurityConfig.class.getAnnotation(Profile.class).value()).containsExactly("!test");
        assertThat(WebSecurityConfig.class.getAnnotation(EnableMethodSecurity.class)).isNotNull();
    }
}
