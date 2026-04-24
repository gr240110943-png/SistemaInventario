package sistemainventario;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Panel inicial tipo dashboard simple.
 *
 * Muestra contadores rapidos y botones de acceso a los modulos.
 */
public class InicioPanel extends JPanel {

    private final InventarioService service;
    private final NavegadorDashboard navegador;

    private final JLabel lblTotalProductos = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblAgotados = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblStockBajo = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblSobreinventario = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblTotalMovimientos = new JLabel("0", SwingConstants.CENTER);

    /**
     * Construye el panel de inicio.
     */
    public InicioPanel(InventarioService service, NavegadorDashboard navegador) {
        this.service = service;
        this.navegador = navegador;
        construirUI();
        refrescar();
    }

    /**
     * Actualiza los indicadores con datos del servicio.
     */
    public void refrescar() {
        lblTotalProductos.setText(String.valueOf(service.contarProductos()));
        lblAgotados.setText(String.valueOf(service.contarAgotados()));
        lblStockBajo.setText(String.valueOf(service.contarStockBajo()));
        lblSobreinventario.setText(String.valueOf(service.contarSobreinventario()));
        lblTotalMovimientos.setText(String.valueOf(service.contarMovimientos()));
    }

    private void construirUI() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Inicio</h1><p style='color:#aaaaaa;'>Pantalla principal para moverte entre modulos.</p></html>");
        add(titulo, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(2, 3, 14, 14));
        tarjetas.setBackground(Interfaz.BG_MAIN);

        tarjetas.add(crearTarjeta("Productos", lblTotalProductos));
        tarjetas.add(crearTarjeta("Agotados", lblAgotados));
        tarjetas.add(crearTarjeta("Stock bajo", lblStockBajo));
        tarjetas.add(crearTarjeta("Sobreinventario", lblSobreinventario));
        tarjetas.add(crearTarjeta("Movimientos", lblTotalMovimientos));

        JPanel contenedorCentro = new JPanel(new BorderLayout(0, 18));
        contenedorCentro.setBackground(Interfaz.BG_MAIN);
        contenedorCentro.add(tarjetas, BorderLayout.CENTER);

        JPanel accesos = new JPanel(new GridLayout(1, 3, 10, 10));
        accesos.setBackground(Interfaz.BG_MAIN);

        var btnCatalogo = Interfaz.botonPrimario("Productos");
        btnCatalogo.addActionListener(e -> navegador.irAVista(InicioFrame.VISTA_CATALOGO));
        var btnMovimientos = Interfaz.botonPrimario("Movimientos");
        btnMovimientos.addActionListener(e -> navegador.irAVista(InicioFrame.VISTA_MOVIMIENTOS));
      //  var btnStock = Interfaz.botonPrimario("Stock");
        //btnStock.addActionListener(e -> navegador.irAVista(InicioFrame.VISTA_STOCK));

        accesos.add(btnCatalogo);
        accesos.add(btnMovimientos);
       // accesos.add(btnStock);

        contenedorCentro.add(accesos, BorderLayout.SOUTH);
        add(contenedorCentro, BorderLayout.CENTER);
    }

    private JPanel crearTarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 8));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Interfaz.BORDER_COLOR),
                new EmptyBorder(16, 16, 16, 16)
        ));
        tarjeta.setBackground(Interfaz.BG_PANEL);

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setForeground(Interfaz.TEXT_MUTED);
        lblTitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        valor.setForeground(Interfaz.TEXT_LIGHT);
        valor.setFont(new Font("SansSerif", Font.BOLD, 30));

        tarjeta.add(lblTitulo, BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }
}
