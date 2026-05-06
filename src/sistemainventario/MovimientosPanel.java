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
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Construye la vista de movimientos y conecta eventos.
     */
    public MovimientosPanel(InventarioService service, Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 14));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel cabecera = new JPanel(new BorderLayout(20, 0));
        cabecera.setBackground(Interfaz.BG_MAIN);
        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Movimientos</h1><p style='color:#aaaaaa;'>Aqui registras entradas, salidas y ajustes.</p></html>");
        lblNumeroMovimientoCabecera.setForeground(Interfaz.ACCENT_BLUE);
        lblNumeroMovimientoCabecera.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblNumeroMovimientoCabecera.setHorizontalAlignment(SwingConstants.RIGHT);
        cabecera.add(titulo, BorderLayout.CENTER);
        cabecera.add(lblNumeroMovimientoCabecera, BorderLayout.EAST);
        add(cabecera, BorderLayout.NORTH);

        JPanel registro = construirPanelRegistro();
        JPanel historial = construirPanelHistorial();

        JPanel centro = new JPanel(new BorderLayout(0, 14));
        centro.setBackground(Interfaz.BG_MAIN);
        centro.add(registro, BorderLayout.NORTH);
        centro.add(historial, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        cbFiltroProducto.addActionListener(e -> cargarHistorial());

        refrescar();
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
        var btnLimpiar = Interfaz.botonSecundario("Limpiar");
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
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Interfaz.BG_MAIN);

        // Tabla stock actual
        JPanel stockPanel = new JPanel(new BorderLayout(0, 8));
        stockPanel.setBackground(Interfaz.BG_MAIN);
        JLabel lblStock = new JLabel("Stock actual de productos");
        lblStock.setForeground(Color.WHITE);
        lblStock.setFont(new Font("SansSerif", Font.BOLD, 13));

        modeloStock = new DefaultTableModel(new Object[]{"CLAVE", "PRODUCTO", "STOCK", "MINIMO", "ESTADO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaStock = new JTable(modeloStock);
        tablaStock.setBackground(Interfaz.BG_PANEL);
        tablaStock.setForeground(Interfaz.TEXT_LIGHT);
        tablaStock.setRowHeight(32);
        tablaStock.setGridColor(Interfaz.BORDER_COLOR);
        tablaStock.setShowVerticalLines(false);
        tablaStock.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tablaStock.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tablaStock.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tablaStock.getColumnModel().getColumn(0).setCellRenderer(center);
        tablaStock.getColumnModel().getColumn(2).setCellRenderer(center);
        tablaStock.getColumnModel().getColumn(3).setCellRenderer(center);
        tablaStock.getColumnModel().getColumn(4).setCellRenderer(new EstadoStockRenderer());

        JScrollPane scrollStock = new JScrollPane(tablaStock);
        scrollStock.getViewport().setBackground(Interfaz.BG_PANEL);
        scrollStock.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        stockPanel.add(lblStock, BorderLayout.NORTH);
        stockPanel.add(scrollStock, BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.setBackground(Interfaz.BG_MAIN);
        JLabel lblFiltro = new JLabel("Historial por producto:");
        lblFiltro.setForeground(Interfaz.TEXT_LIGHT);
        Interfaz.estilizarCombo(cbFiltroProducto);
        cbFiltroProducto.setPreferredSize(new Dimension(280, 32));
        var btnVerTodos = Interfaz.botonSecundario("Ver todos");
        btnVerTodos.addActionListener(e -> {
            if (cbFiltroProducto.getItemCount() > 0) {
                cbFiltroProducto.setSelectedIndex(0);
            }
            cargarHistorial();
        });
        barra.add(lblFiltro);
        barra.add(cbFiltroProducto);
        barra.add(btnVerTodos);

        modeloHistorial = new DefaultTableModel(
                new Object[]{"NO.", "ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "STOCK ANTES", "STOCK DESPUES", "MOTIVO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modeloHistorial);
        tabla.setBackground(Interfaz.BG_PANEL);
        tabla.setForeground(Interfaz.TEXT_LIGHT);
        tabla.setRowHeight(34);
        tabla.setGridColor(Interfaz.BORDER_COLOR);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tabla.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));

        DefaultTableCellRenderer centerHist = new DefaultTableCellRenderer();
        centerHist.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(centerHist);
        tabla.getColumnModel().getColumn(1).setCellRenderer(centerHist);
        tabla.getColumnModel().getColumn(2).setCellRenderer(centerHist);
        tabla.getColumnModel().getColumn(4).setCellRenderer(centerHist);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new CantidadRenderer());
        tabla.getColumnModel().getColumn(6).setCellRenderer(centerHist);
        tabla.getColumnModel().getColumn(7).setCellRenderer(centerHist);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Interfaz.BG_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        lblResumen.setForeground(Interfaz.TEXT_MUTED);

        JPanel historialPanel = new JPanel(new BorderLayout(0, 8));
        historialPanel.setBackground(Interfaz.BG_MAIN);
        historialPanel.add(barra, BorderLayout.NORTH);
        historialPanel.add(scroll, BorderLayout.CENTER);
        historialPanel.add(lblResumen, BorderLayout.SOUTH);

        panel.add(stockPanel, BorderLayout.NORTH);
        panel.add(historialPanel, BorderLayout.CENTER);
        return panel;
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

    private boolean seleccionarProductoPorCodigo() {
        String codigo = txtCodigoProducto.getText() == null ? "" : txtCodigoProducto.getText().trim();
        if (codigo.isEmpty()) {
            return false;
        }
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
        String motivo = txtMotivo.getText().trim();
        if (motivo.isBlank()) {
            errores.add("Motivo: esta vacio.");
        }

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

            // Advertencia sobreinventario (entrada)
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

            MovimientoEncabezado movimiento = service.registrarMovimiento(tipo, fecha, motivo, lineas);
            limpiarFormulario();
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
