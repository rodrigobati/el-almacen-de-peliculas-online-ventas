package unrn.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ClienteActualProvider {

    static final String HEADER_CLIENTE_ID = "X-Cliente-Id";
    static final String HEADER_CLIENTE_EMAIL = "X-Cliente-Email";

    private final Environment environment;

    public ClienteActualProvider(Environment environment) {
        this.environment = environment;
    }

    public String obtenerClienteId() {
        String clienteDesdeSecurity = obtenerDesdeSecurityContext();
        if (clienteDesdeSecurity != null) {
            return clienteDesdeSecurity;
        }

        if (permiteFallbackHeaderEnPerfil()) {
            String clienteDesdeHeader = obtenerDesdeHeader();
            if (clienteDesdeHeader != null) {
                return clienteDesdeHeader;
            }
        }

        throw new ClienteNoAutenticadoException();
    }

    public String obtenerClienteEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        if (permiteFallbackHeaderEnPerfil()) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String clienteEmail = request.getHeader(HEADER_CLIENTE_EMAIL);
                if (clienteEmail != null && !clienteEmail.isBlank()) {
                    return clienteEmail;
                }
            }
        }

        return null;
    }

    private boolean permiteFallbackHeaderEnPerfil() {
        for (String perfilActivo : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(perfilActivo) || "test".equalsIgnoreCase(perfilActivo)) {
                return true;
            }
        }
        return false;
    }

    private String obtenerDesdeSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }

        String username = auth.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }

        return username;
    }

    private String obtenerDesdeHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String clienteId = request.getHeader(HEADER_CLIENTE_ID);
        if (clienteId == null || clienteId.isBlank()) {
            return null;
        }

        return clienteId;
    }
}