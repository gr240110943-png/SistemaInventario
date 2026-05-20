package sistemainventario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
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
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Modulo de analisis de inventario.
 *
 * Incluye:
 * - Consumo total (solo salidas)
 * - Tabla de analisis ABC + EQQ + punto de reorden
 * - Filtro por clave/nombre/categoria
 * - Detalle de consumo por producto (solo salidas)
 * - Indicadores de stock
 */
public class AnalisisInventarioPanel extends JPanel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InventarioService service;
    private final JTextField txtFiltro = Interfaz.input();
    private final JLabel lblConsumoTotal = new JLabel("$0.00", SwingConstants.CENTER);
    private final JLabel lblStockBajo = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblSobreinventario = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblBajoReorden = new JLabel("0", SwingConstants.CENTER);
    private final JLabel lblAvisoSinConsumo = new JLabel(" ", SwingConstants.LEFT);
    private final JLabel lblResumenDetalle = new JLabel("Selecciona un producto para ver sus salidas.");

    private final DefaultTableModel modeloAnalisis;
    private final JTable tablaAnalisis;
    private final DefaultTableModel modeloDetalle;
    private final JTable tablaDetalle;

    private boolean avisoSinConsumoMostrado = false;

    public AnalisisInventarioPanel(InventarioService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 12));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Analisis de inventario</h1>"
                + "<p style='color:#aaaaaa;'>Consumo, clasificacion ABC, EQQ, punto de reorden e indicadores.</p></html>");
        add(titulo, BorderLayout.NORTH);

        modeloAnalisis = new DefaultTableModel(
                new Object[]{"GRUPO", "CODIGO BARRAS", "NOMBRE", "CATEGORIA", "PRECIO", "STOCK ACTUAL", "PUNTO REORDEN", "EQQ", "INDICADORES"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 4, 6, 7 -> Double.class;
                    case 5 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        tablaAnalisis = new JTable(modeloAnalisis);
        configurarTablaAnalisis();

        modeloDetalle = new DefaultTableModel(
                new Object[]{"NO.", "ID", "FECHA", "CANTIDAD SALIDA", "COSTO ACTUAL", "CONSUMO", "MOTIVO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 3 -> Integer.class;
                    case 4, 5 -> Double.class;
                    default -> String.class;
                };
            }
        };

        tablaDetalle = new JTable(modeloDetalle);
        configurarTablaDetalle();

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Interfaz.BG_MAIN);
        contenido.add(construirCabeceraConIndicadores(), BorderLayout.NORTH);
        contenido.add(construirPanelAnalisisYDetalle(), BorderLayout.CENTER);
        add(contenido, BorderLayout.CENTER);

        txtFiltro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });

        tablaAnalisis.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarDetalleConsumoSeleccionado();
            }
        });

        refrescar();
    }

    /**
     * Refresca parametros, tabla principal y detalle.
     */
    public final void refrescar() {
        aplicarFiltro();
    }

    private JPanel construirCabeceraConIndicadores() {
        JPanel principal = new JPanel(new BorderLayout(0, 10));
        principal.setBackground(Interfaz.BG_MAIN);

        JPanel barraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barraFiltro.setBackground(Interfaz.BG_MAIN);
        JLabel lblFiltro = new JLabel("Filtrar:");
        lblFiltro.setForeground(Interfaz.TEXT_LIGHT);
        txtFiltro.setPreferredSize(new Dimension(330, 34));
        txtFiltro.addActionListener(e -> aplicarFiltro());
        var btnLimpiar = Interfaz.botonSecundario("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtFiltro.setText("");
            aplicarFiltro();
        });
        JButton btnGrafica = Interfaz.botonPrimario("Grafica ABC");
        btnGrafica.addActionListener(e -> mostrarGraficaAbc());
        barraFiltro.add(lblFiltro);
        barraFiltro.add(txtFiltro);
        barraFiltro.add(btnLimpiar);
        barraFiltro.add(btnGrafica);

        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 10, 10));
        tarjetas.setBackground(Interfaz.BG_MAIN);
        tarjetas.add(crearTarjeta("Consumo total", lblConsumoTotal));
        tarjetas.add(crearTarjeta("Stock bajo", lblStockBajo));
        tarjetas.add(crearTarjeta("Sobreinventario", lblSobreinventario));
        tarjetas.add(crearTarjeta("Bajo punto reorden", lblBajoReorden));

        lblAvisoSinConsumo.setForeground(new Color(255, 206, 86));
        lblAvisoSinConsumo.setBorder(new EmptyBorder(2, 4, 0, 0));

        principal.add(barraFiltro, BorderLayout.NORTH);
        principal.add(tarjetas, BorderLayout.CENTER);
        principal.add(lblAvisoSinConsumo, BorderLayout.SOUTH);
        return principal;
    }

    private JPanel construirPanelAnalisisYDetalle() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 12));
        panel.setBackground(Interfaz.BG_MAIN);

        JPanel panelAnalisis = new JPanel(new BorderLayout(0, 8));
        panelAnalisis.setBackground(Interfaz.BG_MAIN);
        JLabel lblAnalisis = new JLabel("Tabla de analisis");
        lblAnalisis.setForeground(Color.WHITE);
        lblAnalisis.setFont(new Font("SansSerif", Font.BOLD, 14));
        JScrollPane scrollAnalisis = new JScrollPane(tablaAnalisis);
        scrollAnalisis.getViewport().setBackground(Interfaz.BG_PANEL);
        scrollAnalisis.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));
        panelAnalisis.add(lblAnalisis, BorderLayout.NORTH);
        panelAnalisis.add(scrollAnalisis, BorderLayout.CENTER);

        JPanel panelDetalle = new JPanel(new BorderLayout(0, 8));
        panelDetalle.setBackground(Interfaz.BG_MAIN);
        JLabel lblDetalle = new JLabel("Detalle de consumo (solo salidas)");
        lblDetalle.setForeground(Color.WHITE);
        lblDetalle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.getViewport().setBackground(Interfaz.BG_PANEL);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(Interfaz.BORDER_COLOR));
        lblResumenDetalle.setForeground(Interfaz.TEXT_MUTED);
        panelDetalle.add(lblDetalle, BorderLayout.NORTH);
        panelDetalle.add(scrollDetalle, BorderLayout.CENTER);
        panelDetalle.add(lblResumenDetalle, BorderLayout.SOUTH);

        panel.add(panelAnalisis);
        panel.add(panelDetalle);
        return panel;
    }

    private JPanel crearTarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setBackground(Interfaz.BG_PANEL);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Interfaz.BORDER_COLOR),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setForeground(Interfaz.TEXT_MUTED);
        lblTitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        valor.setForeground(Color.WHITE);
        valor.setFont(new Font("SansSerif", Font.BOLD, 22));

        tarjeta.add(lblTitulo, BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    private void configurarTablaAnalisis() {
        tablaAnalisis.setBackground(Interfaz.BG_PANEL);
        tablaAnalisis.setForeground(Interfaz.TEXT_LIGHT);
        tablaAnalisis.setRowHeight(34);
        tablaAnalisis.setGridColor(Interfaz.BORDER_COLOR);
        tablaAnalisis.setShowVerticalLines(false);
        tablaAnalisis.getTableHeader().setBackground(Interfaz.BG_SIDEBAR);
        tablaAnalisis.getTableHeader().setForeground(Interfaz.TEXT_MUTED);
        tablaAnalisis.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaAnalisis.setSelectionBackground(new Color(56, 56, 64));
        tablaAnalisis.setSelectionForeground(Color.WHITE);
        tablaAnalisis.setRowSorter(new TableRowSorter<>(modeloAnalisis));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        tablaAnalisis.getColumnModel().getColumn(0).setCellRenderer(new GrupoRenderer());
        tablaAnalisis.getColumnModel().getColumn(4).setCellRenderer(new MonedaRenderer());
        tablaAnalisis.getColumnModel().getColumn(5).setCellRenderer(right);
        tablaAnalisis.getColumnModel().getColumn(6).setCellRenderer(new DecimalRenderer());
        tablaAnalisis.getColumnModel().getColumn(7).setCellRenderer(new DecimalRenderer());
        tablaAnalisis.getColumnModel().getColumn(8).setCellRenderer(new IndicadorRenderer());
        tablaAnalisis.getColumnModel().getColumn(1).setCellRenderer(center);
    }

    private void configurarTablaDetalle() {
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
        tablaDetalle.getColumnModel().getColumn(3).setCellRenderer(center);
        tablaDetalle.getColumnModel().getColumn(4).setCellRenderer(new MonedaRenderer());
        tablaDetalle.getColumnModel().getColumn(5).setCellRenderer(new MonedaRenderer());
    }

    private void aplicarFiltro() {
        String claveSeleccionada = obtenerClaveSeleccionada();
        List<AnalisisProducto> lista = service.obtenerAnalisisInventario(txtFiltro.getText());

        modeloAnalisis.setRowCount(0);
        for (AnalisisProducto item : lista) {
            Producto producto = item.getProducto();
            modeloAnalisis.addRow(new Object[]{
                item.getGrupoAbc(),
                producto.getClave(),
                producto.getNombre(),
                service.obtenerNombreCategoria(producto.getCategoriaClave()),
                producto.getPrecio(),
                producto.getStockActual(),
                item.getPuntoReorden(),
                item.getEqq(),
                item.getIndicadoresTexto()
            });
        }

        actualizarIndicadoresGlobales();
        restaurarSeleccion(claveSeleccionada);
    }

    private void actualizarIndicadoresGlobales() {
        List<AnalisisProducto> total = service.obtenerAnalisisInventario("");
        int stockBajo = 0;
        int sobreinventario = 0;
        int bajoReorden = 0;
        for (AnalisisProducto item : total) {
            if (item.isStockBajo()) {
                stockBajo++;
            }
            if (item.isSobreinventario()) {
                sobreinventario++;
            }
            if (item.isBajoPuntoReorden()) {
                bajoReorden++;
            }
        }

        double consumoTotal = service.obtenerConsumoTotalInventario();
        lblConsumoTotal.setText(String.format("$%.2f", consumoTotal));
        lblStockBajo.setText(String.valueOf(stockBajo));
        lblSobreinventario.setText(String.valueOf(sobreinventario));
        lblBajoReorden.setText(String.valueOf(bajoReorden));

        if (consumoTotal <= 0) {
            lblAvisoSinConsumo.setText("No hay consumos (salidas) registrados. Aun no se puede clasificar ABC con valores reales.");
            if (!avisoSinConsumoMostrado) {
                JOptionPane.showMessageDialog(
                        this,
                        "No hay consumos (salidas) registrados.\n"
                        + "Para calcular ABC y consumo total necesitas registrar salidas en movimientos.",
                        "Sin consumos",
                        JOptionPane.INFORMATION_MESSAGE
                );
                avisoSinConsumoMostrado = true;
            }
        } else {
            lblAvisoSinConsumo.setText(" ");
            avisoSinConsumoMostrado = false;
        }
    }

    private void restaurarSeleccion(String claveSeleccionada) {
        if (modeloAnalisis.getRowCount() == 0) {
            tablaAnalisis.clearSelection();
            modeloDetalle.setRowCount(0);
            lblResumenDetalle.setText("No hay productos para mostrar con el filtro actual.");
            return;
        }

        if (claveSeleccionada != null && !claveSeleccionada.isBlank()) {
            for (int filaModelo = 0; filaModelo < modeloAnalisis.getRowCount(); filaModelo++) {
                String clave = String.valueOf(modeloAnalisis.getValueAt(filaModelo, 1));
                if (clave.equalsIgnoreCase(claveSeleccionada)) {
                    int filaVista = tablaAnalisis.convertRowIndexToView(filaModelo);
                    if (filaVista >= 0) {
                        tablaAnalisis.setRowSelectionInterval(filaVista, filaVista);
                        return;
                    }
                }
            }
        }

        tablaAnalisis.setRowSelectionInterval(0, 0);
    }

    private String obtenerClaveSeleccionada() {
        int filaVista = tablaAnalisis.getSelectedRow();
        if (filaVista < 0) {
            return "";
        }
        int filaModelo = tablaAnalisis.convertRowIndexToModel(filaVista);
        return String.valueOf(modeloAnalisis.getValueAt(filaModelo, 1));
    }

    private void cargarDetalleConsumoSeleccionado() {
        int filaVista = tablaAnalisis.getSelectedRow();
        if (filaVista < 0) {
            modeloDetalle.setRowCount(0);
            lblResumenDetalle.setText("Selecciona un producto para ver sus salidas.");
            return;
        }

        int filaModelo = tablaAnalisis.convertRowIndexToModel(filaVista);
        String clave = String.valueOf(modeloAnalisis.getValueAt(filaModelo, 1));
        Producto producto = service.buscarProductoPorClave(clave);
        if (producto == null) {
            modeloDetalle.setRowCount(0);
            lblResumenDetalle.setText("El producto seleccionado ya no existe.");
            return;
        }

        List<MovimientoInventario> salidas = service.obtenerSalidasPorProducto(clave);
        modeloDetalle.setRowCount(0);
        double consumoProducto = 0.0;

        for (MovimientoInventario salida : salidas) {
            int cantidadSalida = Math.abs(salida.getCantidad());
            double consumoLinea = cantidadSalida * producto.getCosto();
            consumoProducto += consumoLinea;
            modeloDetalle.addRow(new Object[]{
                service.obtenerNumeroMovimiento(salida.getId()),
                salida.getId(),
                salida.getFecha().format(FORMATO_FECHA),
                cantidadSalida,
                producto.getCosto(),
                consumoLinea,
                salida.getMotivo()
            });
        }

        lblResumenDetalle.setText("Salidas de " + producto.getClave() + " - " + producto.getNombre()
                + ": " + salidas.size() + " movimiento(s), consumo $" + String.format("%.2f", consumoProducto));
    }

    private void mostrarGraficaAbc() {
        List<AnalisisProducto> datos = service.obtenerAnalisisInventario(txtFiltro.getText());
        int grupoA = 0;
        int grupoB = 0;
        int grupoC = 0;
        double consumoA = 0.0;
        double consumoB = 0.0;
        double consumoC = 0.0;

        for (AnalisisProducto item : datos) {
            String grupo = item.getGrupoAbc();
            if ("A".equalsIgnoreCase(grupo)) {
                grupoA++;
                consumoA += item.getConsumoProducto();
            } else if ("B".equalsIgnoreCase(grupo)) {
                grupoB++;
                consumoB += item.getConsumoProducto();
            } else if ("C".equalsIgnoreCase(grupo)) {
                grupoC++;
                consumoC += item.getConsumoProducto();
            }
        }

        if (grupoA + grupoB + grupoC == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No hay consumos clasificados en A, B o C para graficar.",
                    "Grafica ABC",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(grupoA, "Productos", "A");
        dataset.addValue(grupoB, "Productos", "B");
        dataset.addValue(grupoC, "Productos", "C");
        dataset.addValue(consumoA, "Consumo", "A");
        dataset.addValue(consumoB, "Consumo", "B");
        dataset.addValue(consumoC, "Consumo", "C");

        JFreeChart chart = ChartFactory.createBarChart(
                "Clasificacion ABC por grupo",
                "Grupo",
                "Cantidad / Consumo",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        configurarEstiloGrafica(chart);

        JFrame ventana = new JFrame("Grafica ABC");
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.setContentPane(new ChartPanel(chart));
        ventana.setSize(780, 520);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }

    private void configurarEstiloGrafica(JFreeChart chart) {
        chart.setBackgroundPaint(Interfaz.BG_MAIN);
        chart.getTitle().setPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Interfaz.BG_PANEL);
        plot.setDomainGridlinePaint(Interfaz.BORDER_COLOR);
        plot.setRangeGridlinePaint(Interfaz.BORDER_COLOR);
        plot.getDomainAxis().setLabelPaint(Interfaz.TEXT_LIGHT);
        plot.getDomainAxis().setTickLabelPaint(Interfaz.TEXT_LIGHT);
        plot.getRangeAxis().setLabelPaint(Interfaz.TEXT_LIGHT);
        plot.getRangeAxis().setTickLabelPaint(Interfaz.TEXT_LIGHT);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(74, 227, 161));
        renderer.setSeriesPaint(1, new Color(96, 165, 250));
    }

    private static class MonedaRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Number numero) {
                lbl.setText(String.format("$%.2f", numero.doubleValue()));
            }
            return lbl;
        }
    }

    private static class DecimalRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Number numero) {
                lbl.setText(String.format("%.2f", numero.doubleValue()));
            }
            return lbl;
        }
    }

    private static class GrupoRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String grupo = value == null ? "" : value.toString();
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                if ("A".equalsIgnoreCase(grupo)) {
                    lbl.setForeground(new Color(74, 227, 161));
                } else if ("B".equalsIgnoreCase(grupo)) {
                    lbl.setForeground(new Color(255, 206, 86));
                } else if ("C".equalsIgnoreCase(grupo)) {
                    lbl.setForeground(new Color(255, 107, 107));
                } else {
                    lbl.setForeground(Interfaz.TEXT_MUTED);
                }
            }
            return lbl;
        }
    }

    private static class IndicadorRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String texto = value == null ? "" : value.toString();
            if (!isSelected) {
                if (texto.contains("Stock bajo") || texto.contains("Bajo reorden")) {
                    lbl.setForeground(new Color(255, 206, 86));
                } else if (texto.contains("Sobreinventario")) {
                    lbl.setForeground(new Color(96, 165, 250));
                } else {
                    lbl.setForeground(new Color(74, 227, 161));
                }
            }
            return lbl;
        }
    }
}
