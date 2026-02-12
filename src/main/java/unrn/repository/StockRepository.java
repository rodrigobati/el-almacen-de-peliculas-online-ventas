package unrn.repository;

public interface StockRepository {

    int stockDisponible(String peliculaId);

    void actualizarStock(String peliculaId, int stockDisponible);
}
