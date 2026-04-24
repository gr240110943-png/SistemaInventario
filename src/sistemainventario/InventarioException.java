package sistemainventario;

/**
 * Excepcion de negocio para errores controlados del modulo de inventario.
 */
public class InventarioException extends Exception {

    public InventarioException(String message) {
        super(message);
    }
}
