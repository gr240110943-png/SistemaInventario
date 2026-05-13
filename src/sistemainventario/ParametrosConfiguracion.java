package sistemainventario;

/**
 * Parametros generales usados por los modulos de analisis.
 */
public class ParametrosConfiguracion {

    private String claveProducto;
    private double costoPedido;
    private double costoMantenimiento;
    private int tiempoEntrega;

    public ParametrosConfiguracion(double costoPedido, double costoMantenimiento, int tiempoEntrega) {
        this("", costoPedido, costoMantenimiento, tiempoEntrega);
    }

    public ParametrosConfiguracion(String claveProducto, double costoPedido, double costoMantenimiento, int tiempoEntrega) {
        this.claveProducto = claveProducto;
        this.costoPedido = costoPedido;
        this.costoMantenimiento = costoMantenimiento;
        this.tiempoEntrega = tiempoEntrega;
    }

    public String getClaveProducto() {
        return claveProducto;
    }

    public void setClaveProducto(String claveProducto) {
        this.claveProducto = claveProducto;
    }

    public double getCostoPedido() {
        return costoPedido;
    }

    public void setCostoPedido(double costoPedido) {
        this.costoPedido = costoPedido;
    }

    public double getCostoMantenimiento() {
        return costoMantenimiento;
    }

    public void setCostoMantenimiento(double costoMantenimiento) {
        this.costoMantenimiento = costoMantenimiento;
    }

    public int getTiempoEntrega() {
        return tiempoEntrega;
    }

    public void setTiempoEntrega(int tiempoEntrega) {
        this.tiempoEntrega = tiempoEntrega;
    }
}
