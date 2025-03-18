package net.hostsharing.hsadminng.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
// TODO.impl: securitySchemes should work in OpenAPI yaml, but the Spring templates seem not to support it
@SecurityScheme(type = SecuritySchemeType.HTTP, name = "casTicket", scheme = "bearer", bearerFormat = "CAS ticket", description = "CAS ticket", in = SecuritySchemeIn.HEADER)
public class WebSecurityConfig {

    @Lazy
    @Autowired
    private CasAuthenticationFilter authenticationFilter;

    @Bean
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/api/hs/hosting/asset-types/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(authenticationFilter, AuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                // For unknown reasons Spring security returns 403 FORBIDDEN for a BadCredentialsException.
                                // But it should return 401 UNAUTHORIZED.
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )
                .build();
    }

    @Bean
    @Profile("realCasAuthenticator")
    public CasAuthenticator realCasServiceTicketValidator() {
        return new RealCasAuthenticator();
    }

    @Bean
    @Profile("fakeCasAuthenticator")
    public CasAuthenticator fakeCasServiceTicketValidator() {
        return new FakeCasAuthenticator();
    }

    @Bean
    public CasAuthenticationFilter authenticationFilter(final CasAuthenticator authenticator) {
        return new CasAuthenticationFilter(authenticator);
    }
}
