package sistemainventario;

/**
 * Linea capturada desde UI para registrar un movimiento.
 */
public class MovimientoLinea {

    private final String claveProducto;
    private final int cantidad;

    public MovimientoLinea(String claveProducto, int cantidad) {
        this.claveProducto = claveProducto;
        this.cantidad = cantidad;
    }

    public String getClaveProducto() {
        return claveProducto;
    }

    public int getCantidad() {
        return cantidad;
    }
}

