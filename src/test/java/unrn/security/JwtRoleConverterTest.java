package unrn.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtRoleConverterTest {

    @Test
    @DisplayName("JwtRoleConverter usa clientId configurado")
    void JwtRoleConverter_usaClientIdConfigurado() {
        // Setup: Preparar el escenario
        JwtRoleConverter converter = new JwtRoleConverter("web");
        Jwt jwt = jwtWithClaims(List.of(), null,
                Map.of(
                        "web", Map.of("roles", List.of("admin")),
                        "mobile", Map.of("roles", List.of("client"))));

        // Ejercitación: Ejecutar la acción a probar
        Set<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Verificación: Verificar el resultado esperado
        assertEquals(Set.of("ROLE_ADMIN"), authorities,
                "Debe preferir security.keycloak.client-id cuando está presente");
    }

    @Test
    @DisplayName("JwtRoleConverter usa fallback azp")
    void JwtRoleConverter_usaFallbackAzp() {
        // Setup: Preparar el escenario
        JwtRoleConverter converter = new JwtRoleConverter();
        Jwt jwt = jwtWithClaims(List.of(), "mobile",
                Map.of(
                        "web", Map.of("roles", List.of("admin")),
                        "mobile", Map.of("roles", List.of("client"))));

        // Ejercitación: Ejecutar la acción a probar
        Set<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Verificación: Verificar el resultado esperado
        assertEquals(Set.of("ROLE_CLIENT"), authorities,
                "Debe usar azp cuando no hay client-id configurado");
    }

    @Test
    @DisplayName("JwtRoleConverter usa fallback merge y sin duplicados")
    void JwtRoleConverter_usaFallbackMergeYSinDuplicados() {
        // Setup: Preparar el escenario
        JwtRoleConverter converter = new JwtRoleConverter();
        Jwt jwt = jwtWithClaims(List.of("admin"), null,
                Map.of(
                        "web", Map.of("roles", List.of("admin")),
                        "mobile", Map.of("roles", List.of("client", "ROLE_ADMIN"))));

        // Ejercitación: Ejecutar la acción a probar
        Set<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Verificación: Verificar el resultado esperado
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_CLIENT"), authorities,
                "Debe unificar realm/resource y eliminar duplicados con prefijo canónico");
    }

    private Jwt jwtWithClaims(List<String> realmRoles, String azp, Map<String, Object> resourceAccess) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", realmRoles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));

        if (azp != null) {
            builder.claim("azp", azp);
        }
        if (resourceAccess != null) {
            builder.claim("resource_access", resourceAccess);
        }

        return builder.build();
    }
}
