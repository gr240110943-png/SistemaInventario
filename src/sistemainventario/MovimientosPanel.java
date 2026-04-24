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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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

    private final JComboBox<String> cbProductoMovimiento = new JComboBox<>();
    private final JComboBox<String> cbTipoMovimiento = new JComboBox<>(new String[]{"Entrada", "Salida", "Ajuste"});
    private final JTextField txtCantidad = Interfaz.input();
    private final JTextField txtFecha = Interfaz.input();
    private final JTextField txtMotivo = Interfaz.input();
    private final JLabel lblStockActual = new JLabel("Stock actual: -");

    private final JComboBox<String> cbFiltroProducto = new JComboBox<>();
    private DefaultTableModel modeloHistorial;
    private final JLabel lblResumen = new JLabel("Movimientos: 0");

    /**
     * Construye la vista de movimientos y conecta eventos.
     */
    public MovimientosPanel(InventarioService service, Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 14));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Movimientos</h1><p style='color:#aaaaaa;'>Aqui registras entradas, salidas y ajustes.</p></html>");
        add(titulo, BorderLayout.NORTH);

        JPanel registro = construirPanelRegistro();
        JPanel historial = construirPanelHistorial();

        JPanel centro = new JPanel(new BorderLayout(0, 14));
        centro.setBackground(Interfaz.BG_MAIN);
        centro.add(registro, BorderLayout.NORTH);
        centro.add(historial, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        cbProductoMovimiento.addActionListener(e -> actualizarInfoProducto());
        cbFiltroProducto.addActionListener(e -> cargarHistorial());

        refrescar();
    }

    /**
     * Recarga combo de productos, info de stock y tabla de historial.
     */
    public void refrescar() {
        actualizarCombos();
        actualizarInfoProducto();
        cargarHistorial();
        txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
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

        Interfaz.estilizarCombo(cbProductoMovimiento);
        Interfaz.estilizarCombo(cbTipoMovimiento);
        txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
        lblStockActual.setForeground(Interfaz.TEXT_MUTED);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 10));
        form.setBackground(Interfaz.BG_PANEL);
        form.add(crearCampo("Producto *", cbProductoMovimiento));
        form.add(crearCampo("Tipo *", cbTipoMovimiento));
        form.add(crearCampo("Cantidad *", txtCantidad));
        form.add(crearCampo("Fecha (yyyy-MM-dd HH:mm) *", txtFecha));
        form.add(crearCampo("Motivo *", txtMotivo));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setBackground(Interfaz.BG_PANEL);
        var btnLimpiar = Interfaz.botonSecundario("Limpiar");
        var btnRegistrar = Interfaz.botonPrimario("Registrar");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnRegistrar.addActionListener(e -> registrarMovimiento());
        botones.add(btnLimpiar);
        botones.add(btnRegistrar);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(lblStockActual, BorderLayout.SOUTH);

        JPanel contenedor = new JPanel(new BorderLayout(0, 10));
        contenedor.setBackground(Interfaz.BG_MAIN);
        contenedor.add(panel, BorderLayout.CENTER);
        contenedor.add(botones, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel construirPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Interfaz.BG_MAIN);

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
                new Object[]{"ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "STOCK ANTES", "STOCK DESPUES", "MOTIVO"}, 0) {
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

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(center);
        tabla.getColumnModel().getColumn(1).setCellRenderer(center);
        tabla.getColumnModel().getColumn(3).setCellRenderer(center);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new CantidadRenderer());
        tabla.getColumnModel().getColumn(5).setCellRenderer(center);
        tabla.getColumnModel().getColumn(6).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Interfaz.BG_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        lblResumen.setForeground(Interfaz.TEXT_MUTED);

        panel.add(barra, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(lblResumen, BorderLayout.SOUTH);
        return panel;
    }

    private void actualizarCombos() {
        String seleccionadoMovimiento = (String) cbProductoMovimiento.getSelectedItem();
        String seleccionadoFiltro = (String) cbFiltroProducto.getSelectedItem();

        cbProductoMovimiento.removeAllItems();
        cbFiltroProducto.removeAllItems();
        cbFiltroProducto.addItem("Todos los productos");

        List<Producto> productos = service.obtenerProductosOrdenados();
        for (Producto p : productos) {
            String item = p.getClave() + " - " + p.getNombre();
            cbProductoMovimiento.addItem(item);
            cbFiltroProducto.addItem(item);
        }

        if (seleccionadoMovimiento != null) {
            cbProductoMovimiento.setSelectedItem(seleccionadoMovimiento);
        }
        if (cbProductoMovimiento.getSelectedIndex() < 0 && cbProductoMovimiento.getItemCount() > 0) {
            cbProductoMovimiento.setSelectedIndex(0);
        }

        if (seleccionadoFiltro != null) {
            cbFiltroProducto.setSelectedItem(seleccionadoFiltro);
        }
        if (cbFiltroProducto.getSelectedIndex() < 0 && cbFiltroProducto.getItemCount() > 0) {
            cbFiltroProducto.setSelectedIndex(0);
        }
    }

    private void actualizarInfoProducto() {
        String item = (String) cbProductoMovimiento.getSelectedItem();
        if (item == null) {
            lblStockActual.setText("Stock actual: -");
            return;
        }
        Producto p = service.buscarProductoPorClave(extraerClave(item));
        if (p == null) {
            lblStockActual.setText("Stock actual: -");
            return;
        }
        lblStockActual.setText("Stock actual: " + p.getStockActual()
                + " | Minimo: " + p.getStockMinimo()
                + " | Estado: " + service.calcularEstadoStock(p));
    }

    private void registrarMovimiento() {
        List<String> errores = new ArrayList<>();
        String item = (String) cbProductoMovimiento.getSelectedItem();
        String clave = "";
        if (item == null || item.isBlank()) {
            errores.add("Producto: no seleccionado.");
        } else {
            clave = extraerClave(item);
        }

        String tipo = (String) cbTipoMovimiento.getSelectedItem();
        if (tipo == null || tipo.isBlank()) {
            errores.add("Tipo: no seleccionado.");
        }

        Integer cantidad = leerEntero(txtCantidad.getText().trim(), "Cantidad", errores);
        if (cantidad != null && tipo != null) {
            if (("Entrada".equalsIgnoreCase(tipo) || "Salida".equalsIgnoreCase(tipo)) && cantidad <= 0) {
                errores.add("Cantidad: para " + tipo.toLowerCase() + " debe ser mayor que 0.");
            }
            if ("Ajuste".equalsIgnoreCase(tipo) && cantidad == 0) {
                errores.add("Cantidad: para ajuste no puede ser 0.");
            }
        }

        LocalDateTime fecha = leerFecha(txtFecha.getText().trim(), "Fecha", errores);
        String motivo = txtMotivo.getText().trim();
        if (motivo.isBlank()) {
            errores.add("Motivo: esta vacio.");
        }

        if (!errores.isEmpty()) {
            mostrarErrores("Revisa estas casillas:", errores);
            return;
        }

        try {
            service.registrarMovimiento(clave, tipo, cantidad, fecha, motivo);
            limpiarFormulario();
            onDataChanged.run();
            seleccionarFiltroProducto(clave);
            JOptionPane.showMessageDialog(this, "Movimiento guardado.", "Listo", JOptionPane.INFORMATION_MESSAGE);
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
            Producto p = service.buscarProductoPorClave(m.getClaveProducto());
            String nombreProducto = p == null ? m.getClaveProducto() : p.getClave() + " - " + p.getNombre();
            String cantidadTexto = m.getCantidad() > 0 ? "+" + m.getCantidad() : String.valueOf(m.getCantidad());
            modeloHistorial.addRow(new Object[]{
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

    private void seleccionarFiltroProducto(String clave) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < cbFiltroProducto.getItemCount(); i++) {
            items.add(cbFiltroProducto.getItemAt(i));
        }
        for (String item : items) {
            if (extraerClave(item).equalsIgnoreCase(clave)) {
                cbFiltroProducto.setSelectedItem(item);
                break;
            }
        }
        cargarHistorial();
    }

    private String extraerClave(String item) {
        if (item == null) {
            return "";
        }
        int index = item.indexOf(" - ");
        return index < 0 ? item.trim() : item.substring(0, index).trim();
    }

    private void limpiarFormulario() {
        txtCantidad.setText("");
        txtMotivo.setText("");
        txtFecha.setText(LocalDateTime.now().format(FORMATO_FECHA));
        if (cbTipoMovimiento.getItemCount() > 0) {
            cbTipoMovimiento.setSelectedIndex(0);
        }
        actualizarInfoProducto();
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
}
