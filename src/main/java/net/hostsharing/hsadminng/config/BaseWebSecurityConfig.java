package net.hostsharing.hsadminng.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.RSA_KEY;

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
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final ObjectProvider<JdbcTemplate> jdbcTemplate) throws Exception {
        // @WebMvcTest slices have no DataSource, thus no JdbcTemplate and no API-key authentication;
        // in the real application the JdbcTemplate is always available
        jdbcTemplate.ifAvailable(jdbc -> {
            http.addFilterBefore(new ApiKeyAuthenticationFilter(jdbc), BearerTokenAuthenticationFilter.class);
            // after the Bearer filter: scopes are only enforced if the surviving authentication is
            // the one synthesized from an API-key (requests with both auth headers get rejected before)
            http.addFilterAfter(new ApiKeyScopeEnforcementFilter(), BearerTokenAuthenticationFilter.class);
        });
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

    @Bean("jwtDecoder")
    @Profile("!fake-jwt")
    public JwtDecoder standardJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") final String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") final String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.hmac-secret:}") final String hmacSecret,
            @Value("${spring.security.oauth2.resourceserver.jwt.audiences:}") final String audiences) {
        // the issuer is mandatory, so that every decoder validates the "iss" claim (a token minted
        // under the same signing key but by a different issuer cannot be replayed against us)
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri (HSADMINNG_JWT_ISSUER) must be configured.");
        }
        if (issuerUri.contains("/fake-jwt")) {
            throw new IllegalStateException("You are using a fake-jwt issuer URL (\"" + issuerUri + "\") but the 'fake-jwt' profile is not active. " +
                    "Either activate the 'fake-jwt' profile or set a real JWT issuer URL in HSADMINNG_JWT_ISSUER.");
        }
        if (StringUtils.hasText(hmacSecret)) {
            val decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"))
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
            return withTokenValidators(decoder, issuerUri, audiences);
        }
        if (StringUtils.hasText(jwkSetUri)) {
            if (jwkSetUri.contains("/fake-jwt")) {
                throw new IllegalStateException("You are using a fake-jwt JWK set URI (\"" + jwkSetUri + "\") but the 'fake-jwt' profile is not active. " +
                        "Either activate the 'fake-jwt' profile or set a real JWT JWK set URI in spring.security.oauth2.resourceserver.jwt.jwk-set-uri.");
            }
            return withTokenValidators(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(), issuerUri, audiences);
        }
        return withTokenValidators(JwtDecoders.fromIssuerLocation(issuerUri), issuerUri, audiences);
    }

    // The issuer is always validated (issuer-uri is mandatory). The audience ("aud") claim is only
    // validated if audiences (HSADMINNG_JWT_AUDIENCE) is configured, because the real HS Keycloak
    // access tokens currently carry no "aud" claim at all; once they do, configure the expected value.
    private static NimbusJwtDecoder withTokenValidators(
            final NimbusJwtDecoder decoder, final String issuerUri, final String audiences) {
        final var validators = new ArrayList<OAuth2TokenValidator<Jwt>>();
        validators.add(new JwtTimestampValidator());
        if (StringUtils.hasText(issuerUri)) {
            validators.add(new JwtIssuerValidator(issuerUri));
        }
        if (StringUtils.hasText(audiences)) {
            val expectedAudiences = Arrays.stream(audiences.split(","))
                    .map(String::trim).filter(StringUtils::hasText).toList();
            validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                    aud -> aud != null && expectedAudiences.stream().anyMatch(aud::contains)));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean("jwtDecoder")
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
