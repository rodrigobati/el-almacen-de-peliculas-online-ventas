package unrn.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unrn.dto.ApiErrorResponse;
import unrn.dto.ProjectionBootstrapResponse;
import unrn.service.ProjectionBootstrapService;

@RestController
@RequestMapping("/internal/projection")
public class ProjectionBootstrapController {

    static final String ERROR_TOKEN_INVALIDO = "Token interno inválido";
    static final String ERROR_TOKEN_NO_CONFIGURADO = "Token interno no configurado";

    private final ProjectionBootstrapService projectionBootstrapService;
    private final String internalToken;

    public ProjectionBootstrapController(ProjectionBootstrapService projectionBootstrapService,
            @Value("${ventas.bootstrap.internal-token:}") String internalToken) {
        this.projectionBootstrapService = projectionBootstrapService;
        this.internalToken = internalToken;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<?> rebuildProjection(
            @RequestHeader(value = "X-Internal-Token", required = false) String requestToken) {
        if (internalToken == null || internalToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiErrorResponse("PROJECTION_BOOTSTRAP_FORBIDDEN", ERROR_TOKEN_NO_CONFIGURADO));
        }

        if (requestToken == null || requestToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("PROJECTION_BOOTSTRAP_UNAUTHORIZED", ERROR_TOKEN_INVALIDO));
        }

        if (!internalToken.equals(requestToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiErrorResponse("PROJECTION_BOOTSTRAP_FORBIDDEN", ERROR_TOKEN_INVALIDO));
        }

        var result = projectionBootstrapService.rebuildProjection();
        return ResponseEntity.ok(new ProjectionBootstrapResponse(
                result.fetched(),
                result.inserted(),
                result.updated(),
                result.deactivated(),
                result.durationMs()));
    }
}
