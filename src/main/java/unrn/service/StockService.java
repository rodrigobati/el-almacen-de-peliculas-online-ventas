package unrn.service;

import org.springframework.stereotype.Service;
import unrn.repository.StockRepository;

@Service
public class StockService {

    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_PELICULA_ID_VACIO = "El id de la película no puede estar vacío";
    static final String ERROR_STOCK_INVALIDO = "El stock disponible debe ser mayor o igual a cero";

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public int stockDisponible(String peliculaId) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);
        return stockRepository.stockDisponible(peliculaId);
    }

    public void actualizarStock(String peliculaId, int stockDisponible) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);
        assertStockValido(stockDisponible);
        stockRepository.actualizarStock(peliculaId, stockDisponible);
    }

    private void assertPeliculaIdNoNulo(String peliculaId) {
        if (peliculaId == null) {
            throw new RuntimeException(ERROR_PELICULA_ID_NULO);
        }
    }

    private void assertPeliculaIdNoVacio(String peliculaId) {
        if (peliculaId.trim().isEmpty()) {
            throw new RuntimeException(ERROR_PELICULA_ID_VACIO);
        }
    }

    private void assertStockValido(int stockDisponible) {
        if (stockDisponible < 0) {
            throw new RuntimeException(ERROR_STOCK_INVALIDO);
        }
    }
}
