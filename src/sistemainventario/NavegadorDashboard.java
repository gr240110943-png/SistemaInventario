package sistemainventario;

/**
 * Contrato simple para cambiar de vista desde paneles hijos.
 */
@FunctionalInterface
public interface NavegadorDashboard {
    void irAVista(String vista);
}


