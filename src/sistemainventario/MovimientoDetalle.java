package sistemainventario;

/**
 * Detalle de un movimiento: producto + cantidad y efecto en stock.
 */
public class MovimientoDetalle {

    private final String idMovimiento;
    private final String claveProducto;
    private final int cantidad; // positiva o negativa segun el tipo del encabezado o el ajuste
    private final int stockAntes;
    private final int stockDespues;

    public MovimientoDetalle(String idMovimiento, String claveProducto, int cantidad, int stockAntes, int stockDespues) {
        this.idMovimiento = idMovimiento;
        this.claveProducto = claveProducto;
        this.cantidad = cantidad;
        this.stockAntes = stockAntes;
        this.stockDespues = stockDespues;
    }

    public String getIdMovimiento() {
        return idMovimiento;
    }

    public String getClaveProducto() {
        return claveProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public int getStockAntes() {
        return stockAntes;
    }

    public int getStockDespues() {
        return stockDespues;
    }
}

