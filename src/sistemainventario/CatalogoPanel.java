package sistemainventario;


import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de catalogo de productos.
 *
 * Aqui se puede:
 * - Ver productos en tabla.
 * - Buscar por clave, nombre o categoria.
 * - Crear, editar y cambiar estado.
 */
public class CatalogoPanel extends JPanel {

    private final InventarioService service;
    private final Runnable onDataChanged;

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JTextField txtBuscar;

    private String ultimoFiltro = "";

    /**
     * Construye el panel y conecta acciones de botones/eventos.
     */
    public CatalogoPanel(InventarioService service, Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 12));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Catalogo</h1><p style='color:#aaaaaa;'>Aqui puedes agregar, editar y ver productos.</p></html>");
        add(titulo, BorderLayout.NORTH);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Interfaz.BG_MAIN);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izquierda.setBackground(Interfaz.BG_MAIN);
        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setForeground(Interfaz.TEXT_LIGHT);
        txtBuscar = Interfaz.input();
        txtBuscar.setPreferredSize(new Dimension(280, 34));
        var btnBuscar = Interfaz.botonPrimario("Buscar");
        btnBuscar.addActionListener(e -> aplicarFiltro(txtBuscar.getText()));
        izquierda.add(lblBuscar);
        izquierda.add(txtBuscar);
        izquierda.add(btnBuscar);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setBackground(Interfaz.BG_MAIN);
        var btnRecargar = Interfaz.botonSecundario("Recargar");
        var btnNuevo = Interfaz.botonPrimario("Nuevo producto");
        var btnEditar = Interfaz.botonSecundario("Editar");
        var btnVer = Interfaz.botonSecundario("Ver");
        var btnEstado = Interfaz.botonSecundario("Cambiar estado");
        btnRecargar.addActionListener(e -> recargar());
        btnNuevo.addActionListener(e -> crearProducto());
        btnEditar.addActionListener(e -> editarProductoSeleccionado());
        btnVer.addActionListener(e -> verProductoSeleccionado());
        btnEstado.addActionListener(e -> cambiarEstadoSeleccionado());
        derecha.add(btnRecargar);
        derecha.add(btnNuevo);
        derecha.add(btnEditar);
        derecha.add(btnVer);
        derecha.add(btnEstado);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);

        modeloTabla = new DefaultTableModel(new Object[]{"CLAVE", "NOMBRE", "CATEGORIA", "PRECIO", "STOCK", "ESTADO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(modeloTabla);
        tabla.setBackground(Interfaz.BG_PANEL);
        tabla.setForeground(Interfaz.TEXT_LIGHT);
        tabla.setRowHeight(36);
        tabla.setGridColor(Interfaz.BORDER_COLOR);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tabla.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tabla.setSelectionBackground(new java.awt.Color(56, 56, 64));
        tabla.setSelectionForeground(java.awt.Color.WHITE);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(center);
        tabla.getColumnModel().getColumn(3).setCellRenderer(center);
        tabla.getColumnModel().getColumn(4).setCellRenderer(center);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new EstadoRenderer());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Interfaz.BG_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Interfaz.BG_MAIN);
        centro.add(barra, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro(txtBuscar.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro(txtBuscar.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro(txtBuscar.getText());
            }
        });

        refrescar();
    }

    /**
     * Recarga la tabla respetando el ultimo filtro aplicado.
     */
    public void refrescar() {
        aplicarFiltro(ultimoFiltro);
    }

    private void recargar() {
        try {
            service.recargar();
            onDataChanged.run();
        } catch (InventarioException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void aplicarFiltro(String filtro) {
        ultimoFiltro = filtro == null ? "" : filtro;
        List<Producto> productos = service.filtrarProductos(ultimoFiltro);
        modeloTabla.setRowCount(0);
        for (Producto p : productos) {
            modeloTabla.addRow(new Object[]{
                p.getClave(),
                p.getNombre(),
                service.obtenerNombreCategoria(p.getCategoriaClave()),
                String.format("$%.2f", p.getPrecio()),
                p.getStockActual(),
                p.getEstado()
            });
        }
    }

    private void crearProducto() {
        Producto nuevo = mostrarDialogoProducto(null);
        if (nuevo == null) {
            return;
        }
        try {
            service.registrarProducto(nuevo);
            onDataChanged.run();
        } catch (InventarioException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void editarProductoSeleccionado() {
        Producto existente = obtenerProductoSeleccionado();
        if (existente == null) {
            mostrarError("Selecciona un producto para editar.");
            return;
        }
        if ("Inactivo".equalsIgnoreCase(existente.getEstado())) {
            mostrarError("No puedes editar un producto inactivo. Activalo primero.");
            return;
        }

        Producto actualizado = mostrarDialogoProducto(existente);
        if (actualizado == null) {
            return;
        }

        try {
            service.actualizarProducto(actualizado);
            onDataChanged.run();
        } catch (InventarioException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void cambiarEstadoSeleccionado() {
        Producto existente = obtenerProductoSeleccionado();
        if (existente == null) {
            mostrarError("Selecciona un producto.");
            return;
        }

        boolean estaActivo = "Activo".equalsIgnoreCase(existente.getEstado());
        String accion = estaActivo ? "desactivar" : "activar";
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas " + accion + " el producto " + existente.getClave() + "?",
                "Confirmar " + accion,
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.cambiarEstadoProducto(existente.getClave());
            onDataChanged.run();
        } catch (InventarioException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void verProductoSeleccionado() {
        Producto p = obtenerProductoSeleccionado();
        if (p == null) {
            mostrarError("Selecciona un producto para ver.");
            return;
        }
        String msg = String.format(
                "Clave: %s%nNombre: %s%nCategoria: %s (%s)%nEstado: %s%nCosto: $%.2f%nPrecio: $%.2f%nStock actual: %d%nStock minimo: %d%nTiempo entrega: %d%nDemanda: %d%nEstado stock: %s",
                p.getClave(), p.getNombre(), service.obtenerNombreCategoria(p.getCategoriaClave()), p.getCategoriaClave(),
                p.getEstado(), p.getCosto(), p.getPrecio(), p.getStockActual(), p.getStockMinimo(), p.getTiempoEntrega(),
                p.getDemanda(), service.calcularEstadoStock(p)
        );
        JOptionPane.showMessageDialog(this, msg, "Detalle del producto", JOptionPane.INFORMATION_MESSAGE);
    }

    private Producto obtenerProductoSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String clave = (String) modeloTabla.getValueAt(row, 0);
        return service.buscarProductoPorClave(clave);
    }

    private Producto mostrarDialogoProducto(Producto base) {
        JTextField txtClave = Interfaz.input();
        JTextField txtNombre = Interfaz.input();
        JComboBox<String> cbCategoria = new JComboBox<>();
        Interfaz.estilizarCombo(cbCategoria);
        JTextField txtCosto = Interfaz.input();
        JTextField txtPrecio = Interfaz.input();
        JTextField txtStockActual = Interfaz.input();
        JTextField txtStockMinimo = Interfaz.input();
        JTextField txtTiempoEntrega = Interfaz.input();
        JTextField txtDemanda = Interfaz.input();

        cbCategoria.addItem("Seleccionar categoria");
        List<String> nombresCategoria = new ArrayList<>(service.getCategorias().values());
        for (String nombreCat : nombresCategoria) {
            cbCategoria.addItem(nombreCat);
        }

        if (base != null) {
            txtClave.setText(base.getClave());
            txtClave.setEditable(false);
            txtNombre.setText(base.getNombre());
            cbCategoria.setSelectedItem(service.obtenerNombreCategoria(base.getCategoriaClave()));
            txtCosto.setText(String.valueOf(base.getCosto()));
            txtPrecio.setText(String.valueOf(base.getPrecio()));
            txtStockActual.setText(String.valueOf(base.getStockActual()));
            txtStockMinimo.setText(String.valueOf(base.getStockMinimo()));
            txtTiempoEntrega.setText(String.valueOf(base.getTiempoEntrega()));
            txtDemanda.setText(String.valueOf(base.getDemanda()));
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 8));
        panel.setBackground(Interfaz.BG_MAIN);
        panel.add(crearCampo("Clave *", txtClave));
        panel.add(crearCampo("Nombre *", txtNombre));
        panel.add(crearCampo("Categoria *", cbCategoria));
        panel.add(crearCampo("Costo *", txtCosto));
        panel.add(crearCampo("Precio *", txtPrecio));
        panel.add(crearCampo("Stock actual *", txtStockActual));
        panel.add(crearCampo("Stock minimo *", txtStockMinimo));
        panel.add(crearCampo("Tiempo entrega *", txtTiempoEntrega));
        panel.add(crearCampo("Demanda *", txtDemanda));

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    base == null ? "Nuevo producto" : "Editar producto",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return null;
            }

            List<String> errores = new ArrayList<>();

            String clave = txtClave.getText().trim();
            String nombre = txtNombre.getText().trim();
            if (clave.isEmpty()) {
                errores.add("Clave: esta vacia.");
            }
            if (nombre.isEmpty()) {
                errores.add("Nombre: esta vacio.");
            }
            if (cbCategoria.getSelectedIndex() == 0) {
                errores.add("Categoria: no seleccionada.");
            }

            String claveCategoria = service.obtenerClaveCategoria((String) cbCategoria.getSelectedItem());
            if (cbCategoria.getSelectedIndex() != 0 && claveCategoria.isBlank()) {
                errores.add("Categoria: valor no valido.");
            }
            if (base == null && !clave.isBlank() && service.buscarProductoPorClave(clave) != null) {
                errores.add("Clave: ya existe.");
            }

            Double costo = leerDecimal(txtCosto.getText().trim(), "Costo", errores);
            Double precio = leerDecimal(txtPrecio.getText().trim(), "Precio", errores);
            Integer stockActual = leerEntero(txtStockActual.getText().trim(), "Stock actual", errores);
            Integer stockMinimo = leerEntero(txtStockMinimo.getText().trim(), "Stock minimo", errores);
            Integer tiempoEntrega = leerEntero(txtTiempoEntrega.getText().trim(), "Tiempo entrega", errores);
            Integer demanda = leerEntero(txtDemanda.getText().trim(), "Demanda", errores);

            if (costo != null && costo <= 0) {
                errores.add("Costo: debe ser mayor que 0.");
            }
            if (precio != null && precio <= 0) {
                errores.add("Precio: debe ser mayor que 0.");
            }
            if (stockActual != null && stockActual < 0) {
                errores.add("Stock actual: no puede ser negativo.");
            }
            if (stockMinimo != null && stockMinimo <= 0) {
                errores.add("Stock minimo: debe ser mayor que 0.");
            }
            if (tiempoEntrega != null && tiempoEntrega <= 0) {
                errores.add("Tiempo entrega: debe ser 1 o mayor.");
            }
            if (demanda != null && demanda <= 0) {
                errores.add("Demanda: debe ser mayor que 0.");
            }

            if (!errores.isEmpty()) {
                mostrarErrores("Revisa estas casillas:", errores);
                continue;
            }

            if (costo >= precio) {
                int advertencia = JOptionPane.showConfirmDialog(
                        this,
                        "Advertencia: el costo es mayor o igual al precio.\n¿Quieres guardar de todos modos?",
                        "Advertencia",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (advertencia != JOptionPane.YES_OPTION) {
                    continue;
                }
            }

            return new Producto(
                    clave,
                    nombre,
                    claveCategoria,
                    base == null ? "Activo" : base.getEstado(),
                    costo,
                    precio,
                    stockActual,
                    stockMinimo,
                    tiempoEntrega,
                    demanda
            );
        }
    }

    private JPanel crearCampo(String etiqueta, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Interfaz.BG_MAIN);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(Interfaz.TEXT_LIGHT);
        p.add(lbl, BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Muestra todos los errores de validacion en una sola ventana emergente.
     */
    private void mostrarErrores(String titulo, List<String> errores) {
        StringBuilder sb = new StringBuilder(titulo).append(System.lineSeparator());
        for (String error : errores) {
            sb.append("- ").append(error).append(System.lineSeparator());
        }
        mostrarError(sb.toString().trim());
    }

    private Double leerDecimal(String texto, String campo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(campo + ": esta vacio.");
            return null;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException ex) {
            errores.add(campo + ": debe ser decimal valido.");
            return null;
        }
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

    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String estado = value == null ? "" : value.toString();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if ("Activo".equalsIgnoreCase(estado)) {
                label.setForeground(new java.awt.Color(74, 227, 161));
            } else {
                label.setForeground(new java.awt.Color(255, 107, 107));
            }
            return label;
        }
    }
}
