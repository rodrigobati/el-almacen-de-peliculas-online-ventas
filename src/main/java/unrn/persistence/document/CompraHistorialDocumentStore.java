package unrn.persistence.document;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import unrn.dto.CompraDetalleResponse;
import unrn.dto.CompraResumenResponse;
import unrn.persistence.CompraEntity;

@Component
public class CompraHistorialDocumentStore {

    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final boolean enabled;

    public CompraHistorialDocumentStore(
            ObjectProvider<MongoTemplate> mongoTemplateProvider,
            @Value("${ventas.historial.mongodb.enabled:true}") boolean enabled) {
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.enabled = enabled;
    }

    public boolean activo() {
        return enabled && mongoTemplateProvider.getIfAvailable() != null;
    }

    public void guardar(CompraEntity compra) {
        if (!activo()) {
            return;
        }
        mongoTemplateProvider.getObject().save(CompraDocument.desde(compra));
    }

    public List<CompraResumenResponse> historialCompras(String clienteId) {
        Query query = new Query(Criteria.where("clienteId").is(clienteId))
                .with(Sort.by(Sort.Direction.DESC, "fechaHora"));

        return mongoTemplateProvider.getObject()
                .find(query, CompraDocument.class)
                .stream()
                .map(CompraDocument::toResumenResponse)
                .toList();
    }

    public Optional<CompraDetalleResponse> detalleCompra(Long compraId, String clienteId) {
        Query query = new Query(Criteria.where("compraId").is(compraId).and("clienteId").is(clienteId));

        return Optional.ofNullable(mongoTemplateProvider.getObject().findOne(query, CompraDocument.class))
                .map(CompraDocument::toDetalleResponse);
    }
}