package unrn.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unrn.api.CompraController;
import unrn.dto.CarritoCompraResponse;
import unrn.service.ConfirmarCompraService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompraController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9090/realms/videoclub/protocol/openid-connect/certs"
})
class ApiSecurityProdLikeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmarCompraService confirmarCompraService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("ApiSinAutenticacion enPerfilProdLike retorna401")
    void apiSinAutenticacion_enPerfilProdLike_retorna401() throws Exception {
        // Setup: Preparar el escenario

        // Ejercitación: Ejecutar la acción a probar
        mockMvc.perform(get("/api/carrito"))
                // Verificación: Verificar el resultado esperado
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ApiConClaimsJwt enPerfilProdLike retorna200")
    void apiConClaimsJwt_enPerfilProdLike_retorna200() throws Exception {
        // Setup: Preparar el escenario
        when(jwtDecoder.decode(anyString())).thenReturn(jwtConRealmRoleClient());
        when(confirmarCompraService.verCarrito()).thenReturn(new CarritoCompraResponse(List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO));

        // Ejercitación: Ejecutar la acción a probar
        mockMvc.perform(get("/api/carrito")
                .header("Authorization", "Bearer token-client"))
                // Verificación: Verificar el resultado esperado
                .andExpect(status().isOk());
    }

    private Jwt jwtConRealmRoleClient() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "cliente-1")
                .claim("realm_access", Map.of("roles", List.of("client")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
