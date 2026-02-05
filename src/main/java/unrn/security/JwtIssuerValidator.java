package unrn.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validador custom de JWT que acepta múltiples issuers válidos.
 * Esto permite que el servicio acepte tokens de Keycloak tanto desde
 * Docker network (keycloak-sso:8080) como desde localhost (localhost:9090)
 * para facilitar testing end-to-end.
 */
public class JwtIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> validIssuers;

    public JwtIssuerValidator(List<String> validIssuers) {
        this.validIssuers = validIssuers;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenIssuer = jwt.getIssuer().toString();

        if (validIssuers.contains(tokenIssuer)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El issuer '" + tokenIssuer + "' no es válido. Issuers aceptados: " + validIssuers,
                null);

        return OAuth2TokenValidatorResult.failure(error);
    }
}
