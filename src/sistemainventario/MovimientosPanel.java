package sistemainventario;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;

/**
 * Panel de movimientos de inventario.
 *
 * Permite registrar entradas/salidas/ajustes y ver historial por producto.
 */
public class MovimientosPanel extends JPanel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InventarioService service;
    private final Runnable onDataChanged;

    private final JComboBox<String> cbTipoMovimiento = new JComboBox<>(new String[]{"Entrada", "Salida", "Ajuste"});
    private final JTextField txtFecha = Interfaz.input();
    private final JTextField txtMotivo = Interfaz.input();

    // Captura de detalle (lineas del movimiento)
    private final JTextField txtCodigoProducto = Interfaz.input();
    private final JComboBox<String> cbProductoLinea = new JComboBox<>();
    private final JTextField txtCantidadLinea = Interfaz.input();
    private DefaultTableModel modeloDetalle;
    private JTable tablaDetalle;

    // Tabla stock actual
    private DefaultTableModel modeloStock;
    private JTable tablaStock;

    private final JComboBox<String> cbFiltroProducto = new JComboBox<>();
    private DefaultTableModel modeloHistorial;
    private final JLabel lblResumen = new JLabel("Movimientos: 0");
    private final JLabel lblNumeroMovimientoCabecera = new JLabel();

    private boolean seleccionarProductoPorCodigo() {
        String codigo = txtCodigoProducto.getText() == null ? "" : txtCodigoProducto.getText().trim();
        if (codigo.isEmpty()) {
            return false;
        }
        
        actualizarCombos(); // <--- AGREGA ESTO PARA RESTAURAR LA LISTA COMPLETA
        
        for (int i = 0; i < cbProductoLinea.getItemCount(); i++) {
            String item = cbProductoLinea.getItemAt(i);
            if (extraerClave(item).equalsIgnoreCase(codigo)) {
                cbProductoLinea.setSelectedIndex(i);
                return true;
            }
        }
        mostrarError("No existe el producto con codigo: " + codigo);
        txtCodigoProducto.requestFocusInWindow();
        txtCodigoProducto.selectAll();
        return false;
    }
    
    /**
     * Construye la vista de movimientos y conecta eventos.
     */
    public MovimientosPanel(InventarioService service, Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 14));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(20, 28, 20, 28));
         JPanel centro = new JPanel(new BorderLayout(0, 14));
         
         JPanel registro = construirPanelRegistro();
        JPanel historial = construirPanelHistorial();

         
        centro.setBackground(Interfaz.BG_MAIN);
        centro.add(registro, BorderLayout.NORTH);
        
        // 1. Creamos el scroll general y metemos el panel 'centro' dentro
        JScrollPane scrollGeneral = new JScrollPane(centro);
        scrollGeneral.setBorder(null);
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(16); // Hace que la rueda del mouse baje fluido
        scrollGeneral.getViewport().setBackground(Interfaz.BG_MAIN); // Mantiene tu color de fondo oscuro
        
        // 2. Agregamos el scrollGeneral a la vista principal (reemplazando el add(centro...))
        add(scrollGeneral, BorderLayout.CENTER);


        JPanel cabecera = new JPanel(new BorderLayout(20, 0));
        cabecera.setBackground(Interfaz.BG_MAIN);
        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Movimientos</h1><p style='color:#aaaaaa;'>Aqui registras entradas, salidas y ajustes.</p></html>");
        lblNumeroMovimientoCabecera.setForeground(Interfaz.ACCENT_BLUE);
        lblNumeroMovimientoCabecera.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblNumeroMovimientoCabecera.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel accesosRapidos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accesosRapidos.setBackground(Interfaz.BG_MAIN);
        var btnAbrirStock = Interfaz.botonSecundario("Abrir Stock Actual");
        var btnAbrirHistorial = Interfaz.botonSecundario("Abrir Historial");
        btnAbrirStock.addActionListener(e -> abrirVentanaStockActual());
        btnAbrirHistorial.addActionListener(e -> abrirVentanaHistorial());
        accesosRapidos.add(lblNumeroMovimientoCabecera);
        accesosRapidos.add(btnAbrirStock);
        accesosRapidos.add(btnAbrirHistorial);
        cabecera.add(titulo, BorderLayout.CENTER);
        cabecera.add(accesosRapidos, BorderLayout.EAST);
        add(cabecera, BorderLayout.NORTH);
        
        cbFiltroProducto.addActionListener(e -> cargarHistorial());

        refrescar();
    }

    private JPanel crearBarraBusquedaTabla(String etiqueta, JTable tabla, DefaultTableModel modelo) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Interfaz.BG_MAIN);
        
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(java.awt.Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        JTextField txtBuscar = new JTextField();
        txtBuscar.setPreferredSize(new Dimension(250, 32));
        txtBuscar.setBackground(Interfaz.BG_PANEL);
        txtBuscar.setForeground(java.awt.Color.WHITE);
        txtBuscar.setCaretColor(java.awt.Color.WHITE);
        txtBuscar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Interfaz.BORDER_COLOR),
                new javax.swing.border.EmptyBorder(0, 5, 0, 5)
        ));

        // Magia de filtrado: TableRowSorter
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        // Evento que detecta cada vez que escribes o borras una letra
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            
            private void filtrar() {
                String texto = txtBuscar.getText();
                if (texto.trim().isEmpty()) {
                    sorter.setRowFilter(null); // Muestra todo si está vacío
                } else {
                    // "(?i)" hace que ignore mayúsculas y minúsculas al buscar
                    sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
                }
            }
        });

        panel.add(lbl);
        panel.add(txtBuscar);
        return panel;
    }
    
    /**
     * Recarga combo de productos, info de stock y tabla de historial.
     */
   public void refrescar() {
        actualizarCombos();
        cargarStockActual();
        cargarHistorial();
        actualizarNumeroMovimientoEncabezado();
        txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
        txtCantidadLinea.setText("1");
        txtCodigoProducto.requestFocusInWindow();
    }
   
    private void actualizarNumeroMovimientoEncabezado() {
        int n = service.obtenerSiguienteNumeroMovimiento();
        lblNumeroMovimientoCabecera.setText("No. movimiento (siguiente): " + n);
    }

    private JPanel construirPanelRegistro() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Interfaz.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Interfaz.BORDER_COLOR),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JLabel titulo = new JLabel("Registrar movimiento");
        titulo.setForeground(java.awt.Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));

       Interfaz.estilizarCombo(cbTipoMovimiento);
        Interfaz.estilizarCombo(cbProductoLinea);
        configurarBuscadorProducto(); 
        
        txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
        txtCantidadLinea.setText("1");
        txtCodigoProducto.setToolTipText("Escanea o escribe el codigo (clave) y presiona Enter");

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 10));
        form.setBackground(Interfaz.BG_PANEL);
        form.add(crearCampo("Tipo *", cbTipoMovimiento));
        form.add(crearCampo("Fecha *", txtFecha));
        form.add(crearCampo("Motivo *", txtMotivo));

        // Seccion de detalle (friendly para escaner)
        JPanel detalleTop = new JPanel(new GridLayout(0, 4, 12, 10));
        detalleTop.setBackground(Interfaz.BG_PANEL);
        detalleTop.add(crearCampo("Codigo (scan) *", txtCodigoProducto));
        detalleTop.add(crearCampo("Producto *", cbProductoLinea));
        detalleTop.add(crearCampo("Cantidad *", txtCantidadLinea));
        var btnAgregar = Interfaz.botonSecundario("Agregar");
        btnAgregar.addActionListener(e -> agregarLinea());
        JPanel contAgregar = new JPanel(new BorderLayout());
        contAgregar.setBackground(Interfaz.BG_PANEL);
        contAgregar.add(new JLabel(""), BorderLayout.NORTH);
        contAgregar.add(btnAgregar, BorderLayout.CENTER);
        detalleTop.add(contAgregar);

        modeloDetalle = new DefaultTableModel(new Object[]{"CLAVE", "PRODUCTO", "CANTIDAD"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaDetalle = new JTable(modeloDetalle);
        tablaDetalle.setBackground(Interfaz.BG_PANEL);
        tablaDetalle.setForeground(Interfaz.TEXT_LIGHT);
        tablaDetalle.setRowHeight(32);
        tablaDetalle.setGridColor(Interfaz.BORDER_COLOR);
        tablaDetalle.setShowVerticalLines(false);
        tablaDetalle.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tablaDetalle.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tablaDetalle.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tablaDetalle.getColumnModel().getColumn(0).setCellRenderer(center);
        tablaDetalle.getColumnModel().getColumn(2).setCellRenderer(center);

        // Atajo: Delete para quitar linea(s)
        tablaDetalle.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                "quitarLinea"
        );
        tablaDetalle.getActionMap().put("quitarLinea", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitarLineaSeleccionada();
            }
        });

        // Flujo con Enter (escaner/teclado):
        // - Enter en codigo: seleccionar producto por clave y pasar a cantidad
        // - Enter en cantidad: agregar linea
        txtCodigoProducto.addActionListener(e -> {
            if (seleccionarProductoPorCodigo()) {
                txtCantidadLinea.requestFocusInWindow();
                txtCantidadLinea.selectAll();
            }
        });
        txtCantidadLinea.addActionListener(e -> agregarLinea());

        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.getViewport().setBackground(Interfaz.BG_PANEL);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        JPanel barraDetalle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        barraDetalle.setBackground(Interfaz.BG_PANEL);
        var btnQuitar = Interfaz.botonSecundario("Quitar seleccionado");
        btnQuitar.addActionListener(e -> quitarLineaSeleccionada());
        barraDetalle.add(btnQuitar);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setBackground(Interfaz.BG_PANEL);
        var btnLimpiar = Interfaz.botonSecundario("Limpiar registro");
        var btnRegistrar = Interfaz.botonPrimario("Registrar");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnRegistrar.addActionListener(e -> registrarMovimiento());
        botones.add(btnLimpiar);
        botones.add(btnRegistrar);

        panel.add(titulo, BorderLayout.NORTH);
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(Interfaz.BG_PANEL);
        cuerpo.add(form, BorderLayout.NORTH);
        cuerpo.add(detalleTop, BorderLayout.CENTER);
        cuerpo.add(scrollDetalle, BorderLayout.SOUTH);
        panel.add(cuerpo, BorderLayout.CENTER);
        panel.add(barraDetalle, BorderLayout.SOUTH);

        JPanel contenedor = new JPanel(new BorderLayout(0, 10));
        contenedor.setBackground(Interfaz.BG_MAIN);
        contenedor.add(panel, BorderLayout.CENTER);
        contenedor.add(botones, BorderLayout.SOUTH);
        return contenedor;
    }

  private JPanel construirPanelHistorial() {
    // Panel principal con scroll interno (si el contenedor padre no tiene scroll, se puede ajustar aquí)
    JPanel panel = new JPanel(new BorderLayout(0, 20)); 
    panel.setBackground(Interfaz.BG_MAIN);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JPanel stockPanel = new JPanel(new BorderLayout(0, 8));
    stockPanel.setBackground(Interfaz.BG_MAIN);

    // Configuración de Modelo y Tabla de Stock
    modeloStock = new DefaultTableModel(new Object[]{"CLAVE", "PRODUCTO", "STOCK", "MINIMO", "ESTADO"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    tablaStock = new JTable(modeloStock);
    
    // Estilos de la tabla Stock
    tablaStock.setBackground(Interfaz.BG_PANEL);
    tablaStock.setForeground(Interfaz.TEXT_LIGHT);
    tablaStock.setRowHeight(32);
    tablaStock.setGridColor(Interfaz.BORDER_COLOR);
    tablaStock.setShowVerticalLines(false);
    tablaStock.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
    tablaStock.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
    tablaStock.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));

    // Renderers para Stock
    DefaultTableCellRenderer center = new DefaultTableCellRenderer();
    center.setHorizontalAlignment(SwingConstants.CENTER);
    tablaStock.getColumnModel().getColumn(0).setCellRenderer(center);
    tablaStock.getColumnModel().getColumn(2).setCellRenderer(center);
    tablaStock.getColumnModel().getColumn(3).setCellRenderer(center);
    tablaStock.getColumnModel().getColumn(4).setCellRenderer(new EstadoStockRenderer());

    // Evento: Al hacer clic en el stock, se filtra el historial automáticamente
    tablaStock.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting() && tablaStock.getSelectedRow() >= 0) {
            int fila = tablaStock.getSelectedRow();
            // IMPORTANTE: Usar convertRowIndexToModel por si la tabla está filtrada
            int modelRow = tablaStock.convertRowIndexToModel(fila);
            String clave = (String) modeloStock.getValueAt(modelRow, 0);
            String nombre = (String) modeloStock.getValueAt(modelRow, 1);
            cbFiltroProducto.setSelectedItem(clave + " - " + nombre);
        }
    });

    JScrollPane scrollStock = new JScrollPane(tablaStock);
    scrollStock.getViewport().setBackground(Interfaz.BG_PANEL);
    scrollStock.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));
    scrollStock.setPreferredSize(new Dimension(0, 250)); // Altura fija para no desplazar todo

    JLabel lblStock = new JLabel("Stock actual");
    lblStock.setForeground(Color.WHITE);
    lblStock.setFont(new Font("SansSerif", Font.BOLD, 13));

    stockPanel.add(lblStock, BorderLayout.NORTH);
    stockPanel.add(scrollStock, BorderLayout.CENTER);


    JPanel historialPanel = new JPanel(new BorderLayout(0, 8));
    historialPanel.setBackground(Interfaz.BG_MAIN);

    // Cabecera del historial: filtro por producto
    JPanel cabeceraHistorial = new JPanel(new GridLayout(1, 1, 0, 8));
    cabeceraHistorial.setBackground(Interfaz.BG_MAIN);

    // Fila 1: Filtro por Producto (ComboBox)
    JPanel filaCombo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    filaCombo.setBackground(Interfaz.BG_MAIN);
    JLabel lblFiltro = new JLabel("Historial de:");
    lblFiltro.setForeground(Interfaz.TEXT_LIGHT);
    Interfaz.estilizarCombo(cbFiltroProducto);
    cbFiltroProducto.setPreferredSize(new Dimension(280, 32));
    
    var btnVerTodos = Interfaz.botonSecundario("Ver todos");
    btnVerTodos.addActionListener(e -> {
        if (cbFiltroProducto.getItemCount() > 0) cbFiltroProducto.setSelectedIndex(0);
        cargarHistorial();
    });
    
    filaCombo.add(lblFiltro);
    filaCombo.add(cbFiltroProducto);
    filaCombo.add(btnVerTodos);

    // Configuración de Modelo y Tabla de Historial
    modeloHistorial = new DefaultTableModel(
            new Object[]{"NO.", "ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "STOCK ANT.", "STOCK DESP.", "MOTIVO"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    JTable tablaHist = new JTable(modeloHistorial);
    
    // Estilos de la tabla Historial
    tablaHist.setBackground(Interfaz.BG_PANEL);
    tablaHist.setForeground(Interfaz.TEXT_LIGHT);
    tablaHist.setRowHeight(34);
    tablaHist.setGridColor(Interfaz.BORDER_COLOR);
    tablaHist.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
    tablaHist.getTableHeader().setForeground(Interfaz.TEXT_MUTED);

    // Renderers para Historial
    DefaultTableCellRenderer centerHist = new DefaultTableCellRenderer();
    centerHist.setHorizontalAlignment(SwingConstants.CENTER);
    tablaHist.getColumnModel().getColumn(0).setCellRenderer(centerHist);
    tablaHist.getColumnModel().getColumn(1).setCellRenderer(centerHist);
    tablaHist.getColumnModel().getColumn(2).setCellRenderer(centerHist);
    tablaHist.getColumnModel().getColumn(4).setCellRenderer(centerHist);
    tablaHist.getColumnModel().getColumn(5).setCellRenderer(new CantidadRenderer());
    tablaHist.getColumnModel().getColumn(6).setCellRenderer(centerHist);
    tablaHist.getColumnModel().getColumn(7).setCellRenderer(centerHist);

    cabeceraHistorial.add(filaCombo);

    JScrollPane scrollHist = new JScrollPane(tablaHist);
    scrollHist.getViewport().setBackground(Interfaz.BG_PANEL);
    scrollHist.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));
    scrollHist.setPreferredSize(new Dimension(0, 300)); // Altura para el historial

    lblResumen.setForeground(Interfaz.TEXT_MUTED);

    historialPanel.add(cabeceraHistorial, BorderLayout.NORTH);
    historialPanel.add(scrollHist, BorderLayout.CENTER);
    historialPanel.add(lblResumen, BorderLayout.SOUTH);

    
    panel.add(stockPanel, BorderLayout.NORTH);
    panel.add(historialPanel, BorderLayout.CENTER);

    return panel;
}

    private void abrirVentanaStockActual() {
        cargarStockActual();
        JTable tabla = crearTablaStock(modeloStock);
        mostrarVentanaTabla("Stock actual de productos", tabla, null, new Dimension(850, 500));
    }

    private void abrirVentanaHistorial() {
        cargarHistorial();
        JTable tabla = crearTablaHistorial(modeloHistorial);
        JLabel resumen = new JLabel(lblResumen.getText());
        resumen.setForeground(Interfaz.TEXT_MUTED);
        mostrarVentanaTabla("Historial de movimientos", tabla, resumen, new Dimension(1100, 580));
    }

    private JTable crearTablaStock(DefaultTableModel modelo) {
        JTable tabla = crearTablaBase(modelo, 32);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(center);
        tabla.getColumnModel().getColumn(2).setCellRenderer(center);
        tabla.getColumnModel().getColumn(3).setCellRenderer(center);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new EstadoStockRenderer());
        return tabla;
    }

    private JTable crearTablaHistorial(DefaultTableModel modelo) {
        JTable tabla = crearTablaBase(modelo, 34);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(center);
        tabla.getColumnModel().getColumn(1).setCellRenderer(center);
        tabla.getColumnModel().getColumn(2).setCellRenderer(center);
        tabla.getColumnModel().getColumn(4).setCellRenderer(center);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new CantidadRenderer());
        tabla.getColumnModel().getColumn(6).setCellRenderer(center);
        tabla.getColumnModel().getColumn(7).setCellRenderer(center);
        return tabla;
    }

    private JTable crearTablaBase(DefaultTableModel modelo, int altoFila) {
        JTable tabla = new JTable(modelo);
        tabla.setBackground(Interfaz.BG_PANEL);
        tabla.setForeground(Interfaz.TEXT_LIGHT);
        tabla.setRowHeight(altoFila);
        tabla.setGridColor(Interfaz.BORDER_COLOR);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tabla.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        return tabla;
    }

    private void mostrarVentanaTabla(String titulo, JTable tabla, JLabel pie, Dimension tamano) {
        JDialog ventana = new JDialog(SwingUtilities.getWindowAncestor(this), titulo, Dialog.ModalityType.MODELESS);
        ventana.getContentPane().setBackground(Interfaz.BG_MAIN);
        ventana.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Interfaz.BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel cabeceraTabla = new JPanel(new BorderLayout(0, 10));
        cabeceraTabla.setBackground(Interfaz.BG_MAIN);
        cabeceraTabla.add(lblTitulo, BorderLayout.NORTH);
        cabeceraTabla.add(crearBarraBusquedaTabla("Buscar:", tabla, (DefaultTableModel) tabla.getModel()), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Interfaz.BG_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        panel.add(cabeceraTabla, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        if (pie != null) {
            panel.add(pie, BorderLayout.SOUTH);
        }

        ventana.add(panel, BorderLayout.CENTER);
        ventana.setSize(tamano);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }

    private void actualizarCombos() {
        String seleccionadoLinea = (String) cbProductoLinea.getSelectedItem();
        String seleccionadoFiltro = (String) cbFiltroProducto.getSelectedItem();

        cbProductoLinea.removeAllItems();
        cbFiltroProducto.removeAllItems();
        cbFiltroProducto.addItem("Todos los productos");

        List<Producto> productos = service.obtenerProductosOrdenados();
        for (Producto p : productos) {
            String item = p.getClave() + " - " + p.getNombre();
            cbProductoLinea.addItem(item);
            cbFiltroProducto.addItem(item);
        }

        if (seleccionadoLinea != null) {
            cbProductoLinea.setSelectedItem(seleccionadoLinea);
        }
        if (cbProductoLinea.getSelectedIndex() < 0 && cbProductoLinea.getItemCount() > 0) {
            cbProductoLinea.setSelectedIndex(0);
        }

        if (seleccionadoFiltro != null) {
            cbFiltroProducto.setSelectedItem(seleccionadoFiltro);
        }
        if (cbFiltroProducto.getSelectedIndex() < 0 && cbFiltroProducto.getItemCount() > 0) {
            cbFiltroProducto.setSelectedIndex(0);
        }
    }

    private void cargarStockActual() {
        modeloStock.setRowCount(0);
        List<Producto> productos = service.obtenerProductosOrdenados();
        for (Producto p : productos) {
            modeloStock.addRow(new Object[]{
                p.getClave(),
                p.getNombre(),
                p.getStockActual(),
                p.getStockMinimo(),
                service.calcularEstadoStock(p)
            });
        }
    }

    private void ejecutarLimpieza() {
    // Aquí va solo la acción de borrar, sin JOptionPanes
    txtMotivo.setText("");
    txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
    if (cbTipoMovimiento.getItemCount() > 0) {
        cbTipoMovimiento.setSelectedIndex(0);
    }
    txtCantidadLinea.setText("1");
    txtCodigoProducto.setText("");
    modeloDetalle.setRowCount(0);
    txtCodigoProducto.requestFocusInWindow();
}
    
    private void agregarLinea() {
        List<String> errores = new ArrayList<>();
        // Si viene de escaner, intenta seleccionar por codigo antes de tomar el combo
        String codigo = txtCodigoProducto.getText() == null ? "" : txtCodigoProducto.getText().trim();
        if (!codigo.isEmpty()) {
            seleccionarProductoPorCodigo();
        }
        String item = (String) cbProductoLinea.getSelectedItem();
        if (item == null || item.isBlank()) {
            errores.add("Producto: no seleccionado.");
        }
        String clave = extraerClave(item);
        Integer cantidad = leerEntero(txtCantidadLinea.getText().trim(), "Cantidad", errores);
        if (!errores.isEmpty()) {
            mostrarErrores("Revisa estas casillas:", errores);
            return;
        }

        // Si ya existe, acumula (misma clave)
        for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
            String claveRow = String.valueOf(modeloDetalle.getValueAt(i, 0));
            if (claveRow.equalsIgnoreCase(clave)) {
                int actual = Integer.parseInt(String.valueOf(modeloDetalle.getValueAt(i, 2)));
                modeloDetalle.setValueAt(actual + cantidad, i, 2);
                txtCantidadLinea.setText("");
                return;
            }
        }

        modeloDetalle.addRow(new Object[]{clave, item, cantidad});
        // Listo para el siguiente escaneo
        txtCantidadLinea.setText("1");
        txtCodigoProducto.setText("");
        txtCodigoProducto.requestFocusInWindow();
    }

    private void quitarLineaSeleccionada() {
        int row = tablaDetalle.getSelectedRow();
        if (row < 0) {
            mostrarError("Selecciona una linea para quitar.");
            return;
        }
        modeloDetalle.removeRow(row);
    }

    private void registrarMovimiento() {
       List<String> errores = new ArrayList<>();

        String tipo = (String) cbTipoMovimiento.getSelectedItem();
        if (tipo == null || tipo.isBlank()) {
            errores.add("Tipo: no seleccionado.");
        }

        LocalDateTime fecha = leerFecha(txtFecha.getText().trim(), "Fecha", errores);
        
        // --- CAMBIO AQUÍ: Validación condicional del Motivo ---
        String motivo = txtMotivo.getText().trim();
        if (tipo != null && tipo.equalsIgnoreCase("AJUSTE") && motivo.isBlank()) {
            errores.add("Motivo: es obligatorio para movimientos de tipo AJUSTE.");
        }
        
        // Si no es ajuste y está vacío, le ponemos un texto por defecto para que no sea null
        if (motivo.isBlank()) {
            motivo = "Sin motivo";
        }
        // ------------------------------------------------------

        if (modeloDetalle.getRowCount() == 0) {
            errores.add("Detalle: agrega al menos un producto.");
        }

        if (!errores.isEmpty()) {
            mostrarErrores("Revisa estas casillas:", errores);
            return;
        }

        try {
            List<MovimientoLinea> lineas = new ArrayList<>();
            for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
                String clave = String.valueOf(modeloDetalle.getValueAt(i, 0));
                int cant = Integer.parseInt(String.valueOf(modeloDetalle.getValueAt(i, 2)));
                lineas.add(new MovimientoLinea(clave, cant));
            }

            List<String> advertencias = service.previsualizarAdvertenciasSobreinventario(tipo, lineas);
            if (!advertencias.isEmpty()) {
                StringBuilder sb = new StringBuilder("Advertencia de sobreinventario:")
                        .append(System.lineSeparator());
                for (String a : advertencias) {
                    sb.append("- ").append(a).append(System.lineSeparator());
                }
                int resp = JOptionPane.showConfirmDialog(
                        this,
                        sb.append(System.lineSeparator()).append("¿Deseas continuar?").toString().trim(),
                        "Advertencia",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (resp != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Usamos la variable 'motivo' que ya procesamos arriba
            MovimientoEncabezado movimiento = service.registrarMovimiento(tipo, fecha, motivo, lineas);
            ejecutarLimpieza();
            onDataChanged.run();
            cargarStockActual();
            cargarHistorial();
            actualizarNumeroMovimientoEncabezado();
            JOptionPane.showMessageDialog(this,
                    "Movimiento guardado.\nNo. movimiento: " + movimiento.getNumeroMovimiento() + "\nId: " + movimiento.getId(),
                    "Listo",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (InventarioException ex) {
            mostrarError(ex.getMessage());
        }
    }
private void configurarBuscadorProducto() {
        cbProductoLinea.setEditable(true);
        javax.swing.JTextField txtEditor = (javax.swing.JTextField) cbProductoLinea.getEditor().getEditorComponent();
        
        // Estilizar el editor de texto para que coincida con el tema oscuro
        txtEditor.setBackground(Interfaz.BG_PANEL);
        txtEditor.setForeground(Color.WHITE);
        txtEditor.setCaretColor(Color.WHITE);
        txtEditor.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));

        txtEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int keyCode = e.getKeyCode();
                // Ignorar teclas de navegación (flechas, enter, escape)
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || 
                    keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE ||
                    keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
                    return;
                }

                String textoBusqueda = txtEditor.getText().toLowerCase();
                
                // Obtener productos y filtrar coincidencias (por nombre o clave)
                List<Producto> productos = service.obtenerProductosOrdenados();
                List<String> filtrados = new ArrayList<>();
                for (Producto p : productos) {
                    String item = p.getClave() + " - " + p.getNombre();
                    if (item.toLowerCase().contains(textoBusqueda)) {
                        filtrados.add(item);
                    }
                }

                // Actualizar el modelo del JComboBox
                cbProductoLinea.setModel(new DefaultComboBoxModel<>(filtrados.toArray(new String[0])));
                cbProductoLinea.setSelectedItem(null); // Evitar que autoseleccione y borre lo escrito
                txtEditor.setText(textoBusqueda); // Restaurar el texto del usuario

                // Ocultar y mostrar el popup para que se actualice visualmente
                cbProductoLinea.hidePopup();
                if (!filtrados.isEmpty()) {
                    cbProductoLinea.showPopup();
                }
            }
        });
    }
    
    private void cargarHistorial() {
        modeloHistorial.setRowCount(0);
        String item = (String) cbFiltroProducto.getSelectedItem();
        String claveFiltro = "";
        if (item != null && !"Todos los productos".equalsIgnoreCase(item)) {
            claveFiltro = extraerClave(item);
        }

        List<MovimientoInventario> lista = service.obtenerMovimientos(claveFiltro);
        for (MovimientoInventario m : lista) {
            int numero = service.obtenerNumeroMovimiento(m.getId());
            Producto p = service.buscarProductoPorClave(m.getClaveProducto());
            String nombreProducto = p == null ? m.getClaveProducto() : p.getClave() + " - " + p.getNombre();
            String cantidadTexto = m.getCantidad() > 0 ? "+" + m.getCantidad() : String.valueOf(m.getCantidad());
            modeloHistorial.addRow(new Object[]{
                numero == 0 ? "" : numero,
                m.getId(),
                m.getFecha().format(FORMATO_FECHA),
                nombreProducto,
                m.getTipo(),
                cantidadTexto,
                m.getStockAntes(),
                m.getStockDespues(),
                m.getMotivo()
            });
        }

        if (claveFiltro.isBlank()) {
            lblResumen.setText("Movimientos totales: " + lista.size());
        } else {
            lblResumen.setText("Movimientos para " + claveFiltro + ": " + lista.size());
        }
    }

    private String extraerClave(String item) {
        if (item == null) {
            return "";
        }
        int index = item.indexOf(" - ");
        return index < 0 ? item.trim() : item.substring(0, index).trim();
    }


    private void limpiarFormulario() {
      int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "Se limpiarán los productos agregados sin ningún cambio en el inventario. ¿Deseas continuar?",
            "Confirmar limpieza",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
    );

    if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
        ejecutarLimpieza(); // Llamamos al método que borra
    }
    }

    private JPanel crearCampo(String etiqueta, java.awt.Component campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Interfaz.BG_PANEL);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(Interfaz.TEXT_LIGHT);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return panel;
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrores(String titulo, List<String> errores) {
        StringBuilder sb = new StringBuilder(titulo).append(System.lineSeparator());
        for (String error : errores) {
            sb.append("- ").append(error).append(System.lineSeparator());
        }
        mostrarError(sb.toString().trim());
    }

    private Integer leerEntero(String texto, String campo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(campo + ": esta vacio.");
            return null;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            errores.add(campo + ": debe ser entero valido.");
            return null;
        }
    }

    private LocalDateTime leerFecha(String texto, String campo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(campo + ": esta vacia.");
            return null;
        }
        try {
            return LocalDateTime.parse(texto, FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            errores.add(campo + ": formato invalido (usa yyyy-MM-dd HH:mm).");
            return null;
        }
    }

    private static class CantidadRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String cantidad = value == null ? "0" : value.toString();
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (cantidad.startsWith("+")) {
                lbl.setForeground(new java.awt.Color(74, 227, 161));
            } else if (cantidad.startsWith("-")) {
                lbl.setForeground(new java.awt.Color(255, 107, 107));
            } else {
                lbl.setForeground(Interfaz.TEXT_LIGHT);
            }
            return lbl;
        }
    }

    private static class EstadoStockRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String estado = value == null ? "" : value.toString();
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if ("Agotado".equalsIgnoreCase(estado)) {
                lbl.setForeground(new Color(255, 107, 107));
            } else if ("Stock bajo".equalsIgnoreCase(estado)) {
                lbl.setForeground(new Color(255, 206, 86));
            } else if ("Sobreinventario".equalsIgnoreCase(estado)) {
                lbl.setForeground(new Color(96, 165, 250));
            } else {
                lbl.setForeground(Interfaz.TEXT_LIGHT);
            }
            return lbl;
        }
    }
}
