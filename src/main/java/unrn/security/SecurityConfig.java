package unrn.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private String issuerUri;

        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
        private String jwkSetUri;

        @Value("${security.keycloak.client-id:}")
        private String keycloakClientId;

        @Bean
        @Profile("dev|test")
        public SecurityFilterChain securityFilterChainDevTest(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .requestMatchers("/api/**").permitAll()
                                                .anyRequest().permitAll())
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

                return http.build();
        }

        @Bean
        @Profile("!dev & !test")
        public SecurityFilterChain securityFilterChainProdLike(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/internal/projection/rebuild").permitAll()
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.decoder(jwtDecoder())
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter())))
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

                return http.build();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                // Si jwk-set-uri está definido, usarlo directamente (más rápido, no requiere
                // conectividad en startup)
                NimbusJwtDecoder jwtDecoder;
                if (jwkSetUri != null && !jwkSetUri.isEmpty()) {
                        jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
                } else {
                        // Fallback: obtener JWK URI desde issuer (requiere conectividad)
                        jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
                }

                // Validadores: timestamps + issuer múltiple
                OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

                // Validator custom que acepta múltiples issuers
                OAuth2TokenValidator<Jwt> withIssuers = new JwtIssuerValidator(
                                List.of(
                                                "http://keycloak-sso:8080/realms/videoclub", // Docker network
                                                "http://localhost:9090/realms/videoclub" // Testing local
                                ));

                OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp,
                                withIssuers);

                jwtDecoder.setJwtValidator(validator);
                return jwtDecoder;
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter(keycloakClientId));
                return converter;
        }
}
