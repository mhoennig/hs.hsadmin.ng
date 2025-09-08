package net.hostsharing.hsadminng.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@Configuration
@Profile("!test")
@EnableMethodSecurity // this does not work with @WebMvcTest, see WebSecurityConfigForWebMvcTests
public class WebSecurityConfig extends BaseWebSecurityConfig {
}
