package unrn.service;

public class ClienteNoAutenticadoException extends RuntimeException {

    static final String ERROR_CLIENTE_NO_AUTENTICADO = "No se pudo determinar el cliente autenticado";

    public ClienteNoAutenticadoException() {
        super(ERROR_CLIENTE_NO_AUTENTICADO);
    }
}