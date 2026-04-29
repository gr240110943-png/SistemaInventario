package sistemainventario;


import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
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

    private static final Dimension DIMENSION_CAMPO_FORM = new Dimension(420, 42);
    private static final Dimension DIMENSION_CAMPO_TEXTO_LARGO = new Dimension(420, 86);
    private static final Dimension DIMENSION_DIALOGO_FORM = new Dimension(1060, 720);
    private static final Dimension DIMENSION_DIALOGO_VER = new Dimension(1200, 780);

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

        modeloTabla = new DefaultTableModel(new Object[]{"CLAVE", "NOMBRE", "CATEGORIA", "COSTO", "PRECIO", "STOCK", "ESTADO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 3, 4 -> Double.class;
                    case 5 -> Integer.class;
                    default -> String.class;
                };
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
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(0).setCellRenderer(center);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new MonedaRenderer());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new MonedaRenderer());
        tabla.getColumnModel().getColumn(5).setCellRenderer(right);
        tabla.getColumnModel().getColumn(6).setCellRenderer(new EstadoRenderer());
        tabla.setRowSorter(new TableRowSorter<>(modeloTabla));

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
                p.getCosto(),
                p.getPrecio(),
                p.getStockActual(),
                p.getEstado()
            });
        }
    }

    private void crearProducto() {
        Producto borrador = null;
        while (true) {
            Producto nuevo = mostrarDialogoProducto(null, borrador);
            if (nuevo == null) {
                return;
            }
            try {
                service.registrarProducto(nuevo);
                onDataChanged.run();
                return;
            } catch (InventarioException ex) {
                mostrarError(ex.getMessage());
                borrador = nuevo;
            }
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

        Producto borrador = null;
        while (true) {
            Producto actualizado = mostrarDialogoProducto(existente, borrador);
            if (actualizado == null) {
                return;
            }
            try {
                service.actualizarProducto(actualizado);
                onDataChanged.run();
                return;
            } catch (InventarioException ex) {
                mostrarError(ex.getMessage());
                borrador = actualizado;
            }
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

        JTextField txtClave = Interfaz.input();
        JTextField txtNombre = Interfaz.input();
        JTextField txtCategoria = Interfaz.input();
        JTextField txtEstado = Interfaz.input();
        JTextField txtCosto = Interfaz.input();
        JTextField txtPrecio = Interfaz.input();
        JTextField txtStockActual = Interfaz.input();
        JTextField txtStockMinimo = Interfaz.input();
        JTextField txtTiempoEntrega = Interfaz.input();
        JTextField txtDemanda = Interfaz.input();
        JTextField txtEstadoStock = Interfaz.input();

        txtCosto.setHorizontalAlignment(JTextField.RIGHT);
        txtPrecio.setHorizontalAlignment(JTextField.RIGHT);
        txtStockActual.setHorizontalAlignment(JTextField.RIGHT);
        txtStockMinimo.setHorizontalAlignment(JTextField.RIGHT);
        txtTiempoEntrega.setHorizontalAlignment(JTextField.RIGHT);
        txtDemanda.setHorizontalAlignment(JTextField.RIGHT);

        txtClave.setText(p.getClave());
        txtNombre.setText(p.getNombre());
        txtCategoria.setText(service.obtenerNombreCategoria(p.getCategoriaClave()));
        txtEstado.setText(p.getEstado());
        txtCosto.setText(String.format("$%.2f", p.getCosto()));
        txtPrecio.setText(String.format("$%.2f", p.getPrecio()));
        txtStockActual.setText(String.valueOf(p.getStockActual()));
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        txtTiempoEntrega.setText(String.valueOf(p.getTiempoEntrega()));
        txtDemanda.setText(String.valueOf(p.getDemanda()));
        txtEstadoStock.setText(service.calcularEstadoStock(p));

        bloquearCampo(txtClave);
        bloquearCampo(txtNombre);
        bloquearCampo(txtCategoria);
        bloquearCampo(txtEstado);
        bloquearCampo(txtCosto);
        bloquearCampo(txtPrecio);
        bloquearCampo(txtStockActual);
        bloquearCampo(txtStockMinimo);
        bloquearCampo(txtTiempoEntrega);
        bloquearCampo(txtDemanda);
        bloquearCampo(txtEstadoStock);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 8));
        panel.setBackground(Interfaz.BG_MAIN);
        panel.add(crearCampo("Clave", txtClave));
        panel.add(crearCampo("Nombre", txtNombre));
        panel.add(crearCampo("Categoria", txtCategoria));
        panel.add(crearCampo("Estado", txtEstado));
        panel.add(crearCampo("Costo", txtCosto));
        panel.add(crearCampo("Precio", txtPrecio));
        panel.add(crearCampo("Stock actual", txtStockActual));
        panel.add(crearCampo("Stock minimo", txtStockMinimo));
        panel.add(crearCampo("Tiempo entrega", txtTiempoEntrega));
        panel.add(crearCampo("Demanda", txtDemanda));
        panel.add(crearCampo("Estado stock", txtEstadoStock));

        mostrarDialogoSoloLectura(panel, "Ver producto");
    }

    private Producto obtenerProductoSeleccionado() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            return null;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        String clave = (String) modeloTabla.getValueAt(filaModelo, 0);
        return service.buscarProductoPorClave(clave);
    }

    private Producto mostrarDialogoProducto(Producto base, Producto valoresIniciales) {
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
        txtCosto.setHorizontalAlignment(JTextField.RIGHT);
        txtPrecio.setHorizontalAlignment(JTextField.RIGHT);
        txtStockActual.setHorizontalAlignment(JTextField.RIGHT);
        txtStockMinimo.setHorizontalAlignment(JTextField.RIGHT);
        txtTiempoEntrega.setHorizontalAlignment(JTextField.RIGHT);
        txtDemanda.setHorizontalAlignment(JTextField.RIGHT);

        instalarEnterComoSiguiente(txtClave);
        instalarEnterComoSiguiente(txtNombre);
        instalarEnterComoSiguiente(cbCategoria);
        instalarEnterComoSiguiente(txtCosto);
        instalarEnterComoSiguiente(txtPrecio);
        instalarEnterComoSiguiente(txtStockActual);
        instalarEnterComoSiguiente(txtStockMinimo);
        instalarEnterComoSiguiente(txtTiempoEntrega);
        instalarEnterComoSiguiente(txtDemanda);

        instalarFiltroDecimal(txtCosto, 2);
        instalarFiltroDecimal(txtPrecio, 2);

        cbCategoria.addItem("Seleccionar categoria");
        List<String> nombresCategoria = new ArrayList<>(service.getCategorias().values());
        for (String nombreCat : nombresCategoria) {
            cbCategoria.addItem(nombreCat);
        }

        if (base != null) {
            // Editar: la clave siempre se bloquea
            txtClave.setText(base.getClave());
            txtClave.setEditable(false);
            Producto origen = valoresIniciales != null ? valoresIniciales : base;
            txtNombre.setText(origen.getNombre());
            cbCategoria.setSelectedItem(service.obtenerNombreCategoria(origen.getCategoriaClave()));
            txtCosto.setText(formatearDecimal2(origen.getCosto()));
            txtPrecio.setText(formatearDecimal2(origen.getPrecio()));
            txtStockActual.setText(String.valueOf(origen.getStockActual()));
            txtStockMinimo.setText(String.valueOf(origen.getStockMinimo()));
            txtTiempoEntrega.setText(String.valueOf(origen.getTiempoEntrega()));
            txtDemanda.setText(String.valueOf(origen.getDemanda()));
        } else if (valoresIniciales != null) {
            // Nuevo: se respeta el borrador, manteniendo la clave editable
            txtClave.setText(valoresIniciales.getClave());
            txtNombre.setText(valoresIniciales.getNombre());
            cbCategoria.setSelectedItem(service.obtenerNombreCategoria(valoresIniciales.getCategoriaClave()));
            txtCosto.setText(formatearDecimal2(valoresIniciales.getCosto()));
            txtPrecio.setText(formatearDecimal2(valoresIniciales.getPrecio()));
            txtStockActual.setText(String.valueOf(valoresIniciales.getStockActual()));
            txtStockMinimo.setText(String.valueOf(valoresIniciales.getStockMinimo()));
            txtTiempoEntrega.setText(String.valueOf(valoresIniciales.getTiempoEntrega()));
            txtDemanda.setText(String.valueOf(valoresIniciales.getDemanda()));
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
            int option = mostrarDialogoFormulario(panel, base == null ? "Nuevo producto" : "Editar producto", primerComponenteFormulario(txtClave, txtNombre, cbCategoria));

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
                SwingUtilities.invokeLater(() -> enfocarPrimerError(txtClave, txtNombre, cbCategoria, txtCosto, txtPrecio, txtStockActual, txtStockMinimo, txtTiempoEntrega, txtDemanda));
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
                    redondear2(costo),
                    redondear2(precio),
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

    private void bloquearCampo(JTextField campo) {
        campo.setEditable(false);
        campo.setFocusable(false);
        campo.setCaretColor(Interfaz.TEXT_LIGHT);
    }

    private int mostrarDialogoFormulario(JPanel contenido, String titulo, JComponent focoInicial) {
        JOptionPane optionPane = new JOptionPane(
                contenido,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );
        if (focoInicial != null) {
            optionPane.setInitialSelectionValue(focoInicial);
        }
        return mostrarDialogoConEstilo(optionPane, titulo, focoInicial);
    }

    private void mostrarDialogoSoloLectura(JPanel contenido, String titulo) {
        JOptionPane optionPane = new JOptionPane(
                contenido,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{"Cerrar"},
                "Cerrar"
        );
        mostrarDialogoConEstilo(optionPane, titulo, null);
    }

    private int mostrarDialogoConEstilo(JOptionPane optionPane, String titulo, JComponent focoInicial) {
        optionPane.setBackground(Interfaz.BG_MAIN);
        optionPane.setOpaque(true);
        optionPane.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));

        JDialog dialog = optionPane.createDialog(this, titulo);
        dialog.getContentPane().setBackground(Interfaz.BG_MAIN);
        aplicarTemaDialogo(dialog.getContentPane());
        dialog.setResizable(false);
        dialog.setAutoRequestFocus(true);
        // Evita que el botón OK tome el foco por defecto
        dialog.getRootPane().setDefaultButton(null);
        if (focoInicial != null) {
            Runnable aplicarFoco = () -> SwingUtilities.invokeLater(() -> {
                focoInicial.requestFocusInWindow();
                if (focoInicial instanceof JTextField t) {
                    t.selectAll();
                }
            });

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    // Varios intentos cortos: en JOptionPane el foco suele “rebotar” al botón OK
                    Timer timer = new Timer(40, null);
                    timer.addActionListener(ev -> {
                        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                        if (owner == focoInicial) {
                            timer.stop();
                            return;
                        }
                        aplicarFoco.run();
                    });
                    timer.setRepeats(true);
                    timer.start();
                }
            });

            dialog.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    aplicarFoco.run();
                }

                @Override
                public void windowLostFocus(WindowEvent e) {
                    // no-op
                }
            });
        }
        dialog.setVisible(true);

        Object valor = optionPane.getValue();
        if (valor == null) {
            return JOptionPane.CLOSED_OPTION;
        }
        if (valor instanceof Integer) {
            return (Integer) valor;
        }
        return JOptionPane.OK_OPTION;
    }

    private void aplicarTemaDialogo(Component componente) {
        if (componente instanceof JPanel) {
            componente.setBackground(Interfaz.BG_MAIN);
        }
        if (componente instanceof JLabel etiqueta) {
            etiqueta.setForeground(Interfaz.TEXT_LIGHT);
        }
        if (componente instanceof JButton boton) {
            boton.setBackground(Interfaz.BG_PANEL);
            boton.setForeground(Interfaz.TEXT_LIGHT);
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                aplicarTemaDialogo(hijo);
            }
        }
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
            return Double.parseDouble(normalizarDecimal(texto));
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

    private static void instalarEnterComoSiguiente(JComponent componente) {
        componente.getInputMap(JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "transferFocus"
        );
        componente.getActionMap().put("transferFocus", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                componente.transferFocus();
            }
        });
    }

    private static void instalarFiltroDecimal(JTextField campo, int decimales) {
        if (campo.getDocument() instanceof AbstractDocument doc) {
            doc.setDocumentFilter(new DecimalDocumentFilter(decimales));
        }
    }

    private static String normalizarDecimal(String texto) {
        String t = texto.trim()
                .replace("$", "")
                .replace(" ", "")
                .replace(",", ".");
        return t;
    }

    private static double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static String formatearDecimal2(double valor) {
        return String.format("%.2f", valor);
    }

    private static JComponent primerComponenteFormulario(JTextField txtClave, JTextField txtNombre, JComboBox<String> cbCategoria) {
        if (txtClave.isEditable()) {
            return txtClave;
        }
        return txtNombre != null ? txtNombre : cbCategoria;
    }

    private static void enfocarPrimerError(
            JTextField txtClave,
            JTextField txtNombre,
            JComboBox<String> cbCategoria,
            JTextField txtCosto,
            JTextField txtPrecio,
            JTextField txtStockActual,
            JTextField txtStockMinimo,
            JTextField txtTiempoEntrega,
            JTextField txtDemanda
    ) {
        // Orden de chequeo: de arriba hacia abajo / izquierda a derecha
        if (txtClave.isEditable() && txtClave.getText().trim().isEmpty()) {
            txtClave.requestFocusInWindow();
            txtClave.selectAll();
            return;
        }
        if (txtNombre.getText().trim().isEmpty()) {
            txtNombre.requestFocusInWindow();
            txtNombre.selectAll();
            return;
        }
        if (cbCategoria.getSelectedIndex() == 0) {
            cbCategoria.requestFocusInWindow();
            return;
        }
        if (txtCosto.getText().trim().isEmpty()) {
            txtCosto.requestFocusInWindow();
            txtCosto.selectAll();
            return;
        }
        if (txtPrecio.getText().trim().isEmpty()) {
            txtPrecio.requestFocusInWindow();
            txtPrecio.selectAll();
            return;
        }
        if (txtStockActual.getText().trim().isEmpty()) {
            txtStockActual.requestFocusInWindow();
            txtStockActual.selectAll();
            return;
        }
        if (txtStockMinimo.getText().trim().isEmpty()) {
            txtStockMinimo.requestFocusInWindow();
            txtStockMinimo.selectAll();
            return;
        }
        if (txtTiempoEntrega.getText().trim().isEmpty()) {
            txtTiempoEntrega.requestFocusInWindow();
            txtTiempoEntrega.selectAll();
            return;
        }
        if (txtDemanda.getText().trim().isEmpty()) {
            txtDemanda.requestFocusInWindow();
            txtDemanda.selectAll();
        }
    }

    private static class DecimalDocumentFilter extends DocumentFilter {
        private final int decimales;

        private DecimalDocumentFilter(int decimales) {
            this.decimales = Math.max(0, decimales);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) {
                return;
            }
            String nuevo = construirNuevoTexto(fb, offset, 0, string);
            if (esValido(nuevo)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String nuevo = construirNuevoTexto(fb, offset, length, text == null ? "" : text);
            if (esValido(nuevo)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String nuevo = construirNuevoTexto(fb, offset, length, "");
            if (esValido(nuevo)) {
                super.remove(fb, offset, length);
            }
        }

        private String construirNuevoTexto(FilterBypass fb, int offset, int length, String agregado) throws BadLocationException {
            String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
            StringBuilder sb = new StringBuilder(actual);
            sb.replace(offset, offset + length, agregado);
            return sb.toString();
        }

        private boolean esValido(String texto) {
            String t = texto.trim();
            if (t.isEmpty()) {
                return true;
            }
            // Permite solo numeros y separador decimal ('.' o ',')
            int puntos = 0;
            int comas = 0;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (Character.isDigit(c)) {
                    continue;
                }
                if (c == '.') {
                    puntos++;
                    continue;
                }
                if (c == ',') {
                    comas++;
                    continue;
                }
                return false;
            }
            if (puntos + comas > 1) {
                return false;
            }
            int idx = Math.max(t.indexOf('.'), t.indexOf(','));
            if (idx >= 0) {
                int decCount = t.length() - idx - 1;
                return decCount <= decimales;
            }
            return true;
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

    private static class MonedaRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel etiqueta = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            etiqueta.setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Number numero) {
                etiqueta.setText(String.format("$%.2f", numero.doubleValue()));
            }
            return etiqueta;
        }
    }
}
