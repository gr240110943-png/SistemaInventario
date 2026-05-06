package sistemainventario;

import java.time.LocalDateTime;

/**
 * Encabezado de un movimiento de inventario.
 *
 * Un movimiento puede tener multiples productos (detalles).
 */
public class MovimientoEncabezado {

    private final String id;
    private final int numeroMovimiento;
    private final LocalDateTime fecha;
    private final String tipo;
    private final String motivo;

    public MovimientoEncabezado(String id, int numeroMovimiento, LocalDateTime fecha, String tipo, String motivo) {
        this.id = id;
        this.numeroMovimiento = numeroMovimiento;
        this.fecha = fecha;
        this.tipo = tipo;
        this.motivo = motivo;
    }

    public String getId() {
        return id;
    }

    public int getNumeroMovimiento() {
        return numeroMovimiento;
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
}

