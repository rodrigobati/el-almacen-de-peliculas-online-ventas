package unrn.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryStockRepository implements StockRepository {

    private static final int DEFAULT_STOCK = 10;

    private final Map<String, Integer> stockPorPelicula = new ConcurrentHashMap<>();

    @Override
    public int stockDisponible(String peliculaId) {
        return stockPorPelicula.getOrDefault(peliculaId, DEFAULT_STOCK);
    }

    @Override
    public void actualizarStock(String peliculaId, int stockDisponible) {
        stockPorPelicula.put(peliculaId, stockDisponible);
    }
}
