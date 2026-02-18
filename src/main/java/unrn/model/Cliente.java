package unrn.model;

public class Cliente {

    static final String ERROR_CLIENTE_ID_NULO = "El id del cliente no puede ser nulo";
    static final String ERROR_CLIENTE_ID_VACIO = "El id del cliente no puede estar vacío";

    private final String clienteId;

    public Cliente(String clienteId) {
        assertClienteIdNoNulo(clienteId);
        assertClienteIdNoVacio(clienteId);
        this.clienteId = clienteId;
    }

    private void assertClienteIdNoNulo(String clienteId) {
        if (clienteId == null) {
            throw new RuntimeException(ERROR_CLIENTE_ID_NULO);
        }
    }

    private void assertClienteIdNoVacio(String clienteId) {
        if (clienteId.trim().isEmpty()) {
            throw new RuntimeException(ERROR_CLIENTE_ID_VACIO);
        }
    }

    public boolean esElMismoQue(Cliente otro) {
        if (otro == null) {
            return false;
        }
        return this.clienteId.equals(otro.clienteId);
    }

    public String clienteId() {
        return clienteId;
    }
}