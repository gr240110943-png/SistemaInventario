
package sistemainventario;



import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada del sistema.
 *
 * Su unica responsabilidad es levantar dependencias principales y abrir la
 * ventana de inicio.
 */
public class SistemaInventario {

    /**
     * Arranque de la aplicacion.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                CsvInventarioRepository repository = new CsvInventarioRepository("productos.csv", "movimientos.csv");
                InventarioService service = new InventarioService(repository);
                InicioFrame ventana = new InicioFrame(service);
                ventana.setVisible(true);
            } catch (InventarioException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
}
