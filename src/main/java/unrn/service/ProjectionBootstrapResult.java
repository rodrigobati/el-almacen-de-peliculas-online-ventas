package unrn.service;

public class ProjectionBootstrapResult {

    static final String ERROR_FETCHED_INVALIDO = "La cantidad fetched debe ser mayor o igual a cero";
    static final String ERROR_INSERTED_INVALIDO = "La cantidad inserted debe ser mayor o igual a cero";
    static final String ERROR_UPDATED_INVALIDO = "La cantidad updated debe ser mayor o igual a cero";
    static final String ERROR_DEACTIVATED_INVALIDO = "La cantidad deactivated debe ser mayor o igual a cero";
    static final String ERROR_DURATION_INVALIDO = "La duración debe ser mayor o igual a cero";

    private final int fetched;
    private final int inserted;
    private final int updated;
    private final int deactivated;
    private final long durationMs;

    public ProjectionBootstrapResult(int fetched, int inserted, int updated, int deactivated, long durationMs) {
        assertFetchedValido(fetched);
        assertInsertedValido(inserted);
        assertUpdatedValido(updated);
        assertDeactivatedValido(deactivated);
        assertDurationValida(durationMs);

        this.fetched = fetched;
        this.inserted = inserted;
        this.updated = updated;
        this.deactivated = deactivated;
        this.durationMs = durationMs;
    }

    private void assertFetchedValido(int fetched) {
        if (fetched < 0) {
            throw new RuntimeException(ERROR_FETCHED_INVALIDO);
        }
    }

    private void assertInsertedValido(int inserted) {
        if (inserted < 0) {
            throw new RuntimeException(ERROR_INSERTED_INVALIDO);
        }
    }

    private void assertUpdatedValido(int updated) {
        if (updated < 0) {
            throw new RuntimeException(ERROR_UPDATED_INVALIDO);
        }
    }

    private void assertDeactivatedValido(int deactivated) {
        if (deactivated < 0) {
            throw new RuntimeException(ERROR_DEACTIVATED_INVALIDO);
        }
    }

    private void assertDurationValida(long durationMs) {
        if (durationMs < 0) {
            throw new RuntimeException(ERROR_DURATION_INVALIDO);
        }
    }

    public int fetched() {
        return fetched;
    }

    public int inserted() {
        return inserted;
    }

    public int updated() {
        return updated;
    }

    public int deactivated() {
        return deactivated;
    }

    public long durationMs() {
        return durationMs;
    }
}
