package net.hostsharing.hsadminng.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**").permitAll() // TODO.impl: implement authentication
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }

}
