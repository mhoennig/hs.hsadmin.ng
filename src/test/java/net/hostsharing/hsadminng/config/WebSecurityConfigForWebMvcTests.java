package net.hostsharing.hsadminng.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;


@TestConfiguration
// @EnableMethodSecurity breaks RestTests, endpoints won't be reachable anymore, all return 404
//     that's the reason why this class even exists in the first place
@Profile("test")
public class WebSecurityConfigForWebMvcTests extends BaseWebSecurityConfig {

}
