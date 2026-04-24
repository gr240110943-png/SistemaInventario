package sistemainventario;

import java.time.LocalDateTime;

/**
 * Modelo que representa un movimiento de inventario.
 *
 * La cantidad puede ser positiva o negativa segun el tipo.
 */
public class MovimientoInventario {

    private final String id;
    private final String claveProducto;
    private final int cantidad;
    private final LocalDateTime fecha;
    private final String tipo;
    private final String motivo;
    private final int stockAntes;
    private final int stockDespues;

    /**
     * Crea un registro inmutable de movimiento.
     */
    public MovimientoInventario(String id, String claveProducto, int cantidad, LocalDateTime fecha,
            String tipo, String motivo, int stockAntes, int stockDespues) {
        this.id = id;
        this.claveProducto = claveProducto;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.tipo = tipo;
        this.motivo = motivo;
        this.stockAntes = stockAntes;
        this.stockDespues = stockDespues;
    }

    public String getId() {
        return id;
    }

    public String getClaveProducto() {
        return claveProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public int getStockAntes() {
        return stockAntes;
    }

    public int getStockDespues() {
        return stockDespues;
    }
}
