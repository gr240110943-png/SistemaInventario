package sistemainventario;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Ventana principal del sistema.
 *
 * Contiene el menu lateral y un CardLayout para navegar entre paneles
 * (Inicio, Catalogo, Movimientos y Stock).
 */
public class InicioFrame extends javax.swing.JFrame {

    public static final String VISTA_INICIO = "INICIO";
    public static final String VISTA_CATALOGO = "CATALOGO";
    public static final String VISTA_MOVIMIENTOS = "MOVIMIENTOS";
   // public static final String VISTA_STOCK = "STOCK";

    private final InventarioService service;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);

    private final InicioPanel panelInicio;
    private final CatalogoPanel panelCatalogo;
    private final MovimientosPanel panelMovimientos;
 //   private final StockPanel panelStock;

    private JButton btnInicio;
    private JButton btnCatalogo;
    private JButton btnMovimientos;
    private JButton btnStock;

    private String vistaActiva = VISTA_INICIO;

    /**
     * Construye la ventana principal y registra los paneles del sistema.
     */
    public InicioFrame(InventarioService service) {
        this.service = service;
        setTitle("Inventario");
        setSize(1360, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Interfaz.BG_MAIN);
        setLayout(new BorderLayout());

        crearMenuLateral();

        panelInicio = new InicioPanel(service, this::mostrarVista);
        panelCatalogo = new CatalogoPanel(service, this::actualizarTodo);
        panelMovimientos = new MovimientosPanel(service, this::actualizarTodo);
      //  panelStock = new StockPanel(service);

        panelContenido.setBackground(Interfaz.BG_MAIN);
        panelContenido.add(panelInicio, VISTA_INICIO);
        panelContenido.add(panelCatalogo, VISTA_CATALOGO);
        panelContenido.add(panelMovimientos, VISTA_MOVIMIENTOS);
       // panelContenido.add(panelStock, VISTA_STOCK);
        add(panelContenido, BorderLayout.CENTER);

        actualizarTodo();
        mostrarVista(VISTA_INICIO);
    }

    /**
     * Refresca todos los paneles para mostrar datos actualizados.
     */
    public final void actualizarTodo() {
        panelInicio.refrescar();
        panelCatalogo.refrescar();
        panelMovimientos.refrescar();
      //  panelStock.refrescar();
    }

    /**
     * Cambia la vista actual en el CardLayout.
     */
    public final void mostrarVista(String vista) {
        vistaActiva = vista;
        cardLayout.show(panelContenido, vista);
        actualizarEstadoBotones();
    }

    private void crearMenuLateral() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Interfaz.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Interfaz.BORDER_COLOR));

        JLabel lblTitulo = new JLabel("<html><b style='color:#3a7bd5; font-size:14px;'>Sistema de Inventario</b><br><span style='color:#999999; font-size:10px;'>Menu principal</span></html>");
        lblTitulo.setBorder(new EmptyBorder(20, 16, 20, 16));
        sidebar.add(lblTitulo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));

        btnInicio = crearBotonMenu("Inicio");
        btnInicio.addActionListener(e -> mostrarVista(VISTA_INICIO));
        btnCatalogo = crearBotonMenu("Catalogo Productos");
        btnCatalogo.addActionListener(e -> mostrarVista(VISTA_CATALOGO));
        btnMovimientos = crearBotonMenu("Movimientos");
        btnMovimientos.addActionListener(e -> mostrarVista(VISTA_MOVIMIENTOS));
        btnStock = crearBotonMenu("Stock");
     //   btnStock.addActionListener(e -> mostrarVista(VISTA_STOCK));

        sidebar.add(btnInicio);
        sidebar.add(btnCatalogo);
        sidebar.add(btnMovimientos);
       // sidebar.add(btnStock);
        sidebar.add(Box.createVerticalGlue());

        add(sidebar, BorderLayout.WEST);
    }

    private JButton crearBotonMenu(String texto) {
        JButton btn = new JButton(texto);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 44));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(12, 20, 12, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(Interfaz.TEXT_LIGHT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return btn;
    }

    private void actualizarEstadoBotones() {
        marcar(btnInicio, VISTA_INICIO.equals(vistaActiva));
        marcar(btnCatalogo, VISTA_CATALOGO.equals(vistaActiva));
        marcar(btnMovimientos, VISTA_MOVIMIENTOS.equals(vistaActiva));
      //  marcar(btnStock, VISTA_STOCK.equals(vistaActiva));
    }

    private void marcar(JButton boton, boolean activo) {
        if (activo) {
            boton.setForeground(Interfaz.ACCENT_BLUE);
            boton.setFont(new Font("SansSerif", Font.BOLD, 13));
            boton.setOpaque(true);
            boton.setBackground(new java.awt.Color(40, 50, 70));
        } else {
            boton.setForeground(Interfaz.TEXT_LIGHT);
            boton.setFont(new Font("SansSerif", Font.PLAIN, 13));
            boton.setOpaque(false);
            boton.setBackground(Interfaz.BG_SIDEBAR);
        }
    }
}
