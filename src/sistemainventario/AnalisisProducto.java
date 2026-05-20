package sistemainventario;

/**
 * Resultado del analisis de inventario por producto.
 *
 * Guarda los datos que necesita la tabla de analisis:
 * - Grupo ABC
 * - Consumo del producto
 * - Punto de reorden
 * - EQQ
 * - Indicadores de stock
 */
public class AnalisisProducto {

    private final Producto producto;
    private final String grupoAbc;
    private final double consumoProducto;
    private final double porcentajeConsumo;
    private final double porcentajeAcumulado;
    private final double puntoReorden;
    private final double eqq;
    private final boolean stockBajo;
    private final boolean sobreinventario;
    private final boolean bajoPuntoReorden;

    public AnalisisProducto(Producto producto, String grupoAbc, double consumoProducto,
            double porcentajeConsumo, double porcentajeAcumulado, double puntoReorden,
            double eqq, boolean stockBajo, boolean sobreinventario, boolean bajoPuntoReorden) {
        this.producto = producto;
        this.grupoAbc = grupoAbc;
        this.consumoProducto = consumoProducto;
        this.porcentajeConsumo = porcentajeConsumo;
        this.porcentajeAcumulado = porcentajeAcumulado;
        this.puntoReorden = puntoReorden;
        this.eqq = eqq;
        this.stockBajo = stockBajo;
        this.sobreinventario = sobreinventario;
        this.bajoPuntoReorden = bajoPuntoReorden;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getGrupoAbc() {
        return grupoAbc;
    }

    public double getConsumoProducto() {
        return consumoProducto;
    }

    public double getPorcentajeConsumo() {
        return porcentajeConsumo;
    }

    public double getPorcentajeAcumulado() {
        return porcentajeAcumulado;
    }

    public double getPuntoReorden() {
        return puntoReorden;
    }

    public double getEqq() {
        return eqq;
    }

    public boolean isStockBajo() {
        return stockBajo;
    }

    public boolean isSobreinventario() {
        return sobreinventario;
    }

    public boolean isBajoPuntoReorden() {
        return bajoPuntoReorden;
    }

    /**
     * Texto resumido para mostrar rapido los indicadores en tabla.
     */
    public String getIndicadoresTexto() {
        StringBuilder sb = new StringBuilder();
        if (stockBajo) {
            sb.append("Stock bajo");
        }
        if (sobreinventario) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Sobreinventario");
        }
        if (bajoPuntoReorden) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Bajo reorden");
        }
        if (sb.length() == 0) {
            return "Normal";
        }
        return sb.toString();
    }
}
