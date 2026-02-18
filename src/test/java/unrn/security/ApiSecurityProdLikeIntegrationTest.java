package unrn.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ventasdb-prod-like;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9090/realms/videoclub/protocol/openid-connect/certs"
})
class ApiSecurityProdLikeIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Test
    @DisplayName("ApiSinAutenticacion enPerfilProdLike retorna401")
    void apiSinAutenticacion_enPerfilProdLike_retorna401() throws Exception {
        // Setup: Preparar el escenario
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Ejercitación: Ejecutar la acción a probar
        mockMvc.perform(get("/api/carrito"))
                // Verificación: Verificar el resultado esperado
                .andExpect(status().isUnauthorized());
    }
}
