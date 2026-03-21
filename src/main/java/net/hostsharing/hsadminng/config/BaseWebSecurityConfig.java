package net.hostsharing.hsadminng.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletResponse;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.RSA_KEY;

@Configuration
@EnableWebSecurity
// securitySchemes should work in OpenAPI yaml, but the Spring templates seem not to support it
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public abstract class BaseWebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                // only list endpoints implemented in libraries here
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/fake-jwt/**"
                                // otherwise use @PreAuthorize annotation at the controller class / endpoint method level
                        ).permitAll()
                        .requestMatchers("/api/**").permitAll() // controlled at method level
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults()))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                // For unknown reason Spring security returns 403 FORBIDDEN for a BadCredentialsException.
                                // But it should return 401 UNAUTHORIZED.
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )
                .build();
    }

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:${server.port}/fake-jwt/.well-known/jwks.json}")
    private String jwkSetUri;

    @Bean
    @Profile("!fake-jwt")
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    @Profile("fake-jwt")
    @SneakyThrows
    public JwtDecoder fakeJwtDecoder() {
        // For fake-jwt profile, use the same RSA key as JwtFakeBearer
        return NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${hsadminng.cors.allowed-origins:*}") final String corsAllowedOrigins) {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(@NonNull final CorsRegistry registry) {
                val allowedOrigins = (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty())
                        ? corsAllowedOrigins.split(",")
                        : new String[]{"*"};
                registry.addMapping("/api/ping")
                        .allowedOrigins("*")
                        .allowedMethods("GET");
                registry.addMapping("/api/**").allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE");
            }
        };
    }
}
