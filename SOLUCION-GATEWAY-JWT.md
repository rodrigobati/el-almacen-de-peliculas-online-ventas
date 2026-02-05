# Solución para JWT en Gateway

## Problema Actual

### Estado de los Tests

- ✅ **TEST A** (Ventas directo SIN token): 401 OK
- ✅ **TEST B** (Ventas directo CON token): 200 OK
- ❌ **TEST C** (Gateway SIN token): 404 (debería ser 401)
- ❌ **TEST D** (Gateway CON token): 401 (debería ser 200)

### Causa Raíz del Problema

El Gateway está validando el **issuer** del JWT y lo rechaza porque:

1. **Token obtenido desde localhost:**
   - `iss: http://localhost:9090/realms/videoclub`
2. **Gateway esperando issuer de Docker:**
   - `iss: http://keycloak-sso:8080/realms/videoclub`

Esto genera el error:

```
The iss claim is not valid
```

### Por Qué Funciona en Ventas pero NO en Gateway

**Ventas** tiene un `JwtIssuerValidator` custom que acepta **múltiples issuers**:

- `http://keycloak-sso:8080/realms/videoclub` (Docker network)
- `http://localhost:9090/realms/videoclub` (Testing local)

**Gateway** usa validación por defecto de Spring Security que solo acepta UN issuer.

---

## Solución Propuesta

### Opción 1: Configurar Gateway con JWK Set URI (MÁS RÁPIDA)

En lugar de usar `issuer-uri` que obliga a validar el issuer, usar `jwk-set-uri` directamente.

**Archivo:** `apigateway-main/src/main/resources/application-docker.yml`

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Comentar o eliminar issuer-uri
          # issuer-uri: http://keycloak-sso:8080/realms/videoclub

          # Usar JWK Set URI directamente (no valida issuer)
          jwk-set-uri: http://keycloak-sso:8080/realms/videoclub/protocol/openid-connect/certs
```

**Ventajas:**

- ✅ No requiere código custom
- ✅ Solo cambio de configuración
- ✅ Acepta tokens de cualquier issuer (localhost o keycloak-sso)
- ✅ Más rápido en startup (no hace llamada a .well-known)

**Desventajas:**

- ⚠️ No valida el issuer (menos seguro en producción)

---

### Opción 2: Implementar JwtIssuerValidator Custom en Gateway (MÁS SEGURA)

Crear el mismo validador que usamos en Ventas para aceptar múltiples issuers.

**Archivos a crear:**

1. `apigateway-main/src/main/java/com/videoclub/apigateway/security/JwtIssuerValidator.java`
2. Modificar `apigateway-main/src/main/java/com/videoclub/apigateway/config/SecurityConfig.java`

**Código del Validator:**

```java
package com.videoclub.apigateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

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
```

**Modificación del SecurityConfig:**

```java
// Agregar imports
import org.springframework.security.oauth2.jwt.*;
import java.util.List;

// Agregar bean JwtDecoder con validación custom
@Bean
public ReactiveJwtDecoder jwtDecoder() {
    String jwkSetUri = "http://keycloak-sso:8080/realms/videoclub/protocol/openid-connect/certs";
    NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();

    OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
    OAuth2TokenValidator<Jwt> withIssuers = new JwtIssuerValidator(
        List.of(
            "http://keycloak-sso:8080/realms/videoclub",
            "http://localhost:9090/realms/videoclub"
        )
    );

    OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
        withTimestamp,
        withIssuers
    );

    jwtDecoder.setJwtValidator(validator);
    return jwtDecoder;
}
```

**Ventajas:**

- ✅ Más seguro (valida issuer contra lista blanca)
- ✅ Consistente con implementación de Ventas
- ✅ Fácil de extender con más issuers

**Desventajas:**

- ⚠️ Requiere más código
- ⚠️ Necesita rebuild del Gateway

---

## Recomendación

**Para testing local:** Usar **Opción 1** (JWK Set URI)

**Para producción:** Usar **Opción 2** (Validator custom) o configurar Keycloak para emitir tokens con issuer público correcto.

---

## Pasos de Implementación (Opción 1 - Rápida)

1. Editar `apigateway-main/src/main/resources/application-docker.yml`
2. Cambiar `issuer-uri` por `jwk-set-uri`
3. Rebuild Gateway: `docker build -t api-gateway:latest .`
4. Reiniciar Gateway: `docker stop api-gateway && docker rm api-gateway`
5. Iniciar Gateway con nueva imagen
6. Ejecutar matriz de tests

---

## Resultado Esperado

Después de aplicar la solución, los 4 tests deben pasar:

- ✅ TEST A (Ventas NO token): 401
- ✅ TEST B (Ventas CON token): 200
- ✅ TEST C (Gateway NO token): 401
- ✅ TEST D (Gateway CON token): 200
