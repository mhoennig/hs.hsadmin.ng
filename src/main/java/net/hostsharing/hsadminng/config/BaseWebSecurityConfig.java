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
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

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
                .addFilterBefore(new AssumedRolesHeaderFilter(), BasicAuthenticationFilter.class)
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

    @Bean
    @Profile("!fake-jwt")
    public JwtDecoder jwtDecoder(
            // TODO.impl [for Story#458]: Maybe move all defaults from the application.yml to here?
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") final String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") final String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.hmac-secret:${HSADMINNG_JWT_HMAC_SECRET:}}") final String hmacSecret) {
        if (StringUtils.hasText(hmacSecret)) {
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"))
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }
        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        if (StringUtils.hasText(issuerUri)) {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }
        throw new IllegalStateException("Either spring.security.oauth2.resourceserver.jwt.hmac-secret (HSADMINNG_JWT_HMAC_SECRET), spring.security.oauth2.resourceserver.jwt.jwk-set-uri (HSADMINNG_JWT_JWKS_URL) or ...issuer-uri (HSADMINNG_JWT_ISSUER) must be configured.");
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
