package sistemainventario;

/**
 * Modelo de datos de un producto del inventario.
 */
public class Producto {

    private String clave;
    private String nombre;
    private String categoriaClave;
    private String estado;
    private double costo;
    private double precio;
    private int stockActual;
    private int stockMinimo;
    private int tiempoEntrega;
    private int demanda;

    /**
     * Crea un producto con todos sus datos.
     */
    public Producto(String clave, String nombre, String categoriaClave, String estado,
            double costo, double precio, int stockActual, int stockMinimo,
            int tiempoEntrega, int demanda) {
        this.clave = clave;
        this.nombre = nombre;
        this.categoriaClave = categoriaClave;
        this.estado = estado;
        this.costo = costo;
        this.precio = precio;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.tiempoEntrega = tiempoEntrega;
        this.demanda = demanda;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoriaClave() {
        return categoriaClave;
    }

    public void setCategoriaClave(String categoriaClave) {
        this.categoriaClave = categoriaClave;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getCosto() {
        return costo;
    }

    public void setCosto(double costo) {
        this.costo = costo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public int getTiempoEntrega() {
        return tiempoEntrega;
    }

    public void setTiempoEntrega(int tiempoEntrega) {
        this.tiempoEntrega = tiempoEntrega;
    }

    public int getDemanda() {
        return demanda;
    }

    public void setDemanda(int demanda) {
        this.demanda = demanda;
    }
}
