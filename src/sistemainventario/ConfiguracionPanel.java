package sistemainventario;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modulo de configuracion general para parametros de analisis.
 */
public class ConfiguracionPanel extends JPanel {

    private static final DecimalFormat FORMATO_MONEDA = new DecimalFormat("0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    private final InventarioService service;
    private final JTextField txtCostoPedido = Interfaz.input();
    private final JTextField txtCostoMantenimiento = Interfaz.input();
    private final JTextField txtTiempoEntrega = Interfaz.input();

    public ConfiguracionPanel(InventarioService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 10));
        setBackground(Interfaz.BG_MAIN);
        setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel titulo = new JLabel("<html><h1 style='color:white; margin:0;'>Configuracion</h1>"
                + "<p style='color:#aaaaaa;'>Los valores registrados se cargan aqui para editarlos.</p></html>");
        add(titulo, BorderLayout.NORTH);

        prepararCampo(txtCostoPedido);
        prepararCampo(txtCostoMantenimiento);
        prepararCampo(txtTiempoEntrega);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setBackground(Interfaz.BG_PANEL);
        form.add(crearCampo("Costo por pedido u orden de compra *", txtCostoPedido));
        form.add(crearCampo("Costo de mantenimiento o conservacion *", txtCostoMantenimiento));
        form.add(crearCampo("Tiempo de entrega *", txtTiempoEntrega));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setBackground(Interfaz.BG_PANEL);
        var btnGuardar = Interfaz.botonPrimario("Guardar cambios");
        btnGuardar.addActionListener(e -> guardar());
        acciones.add(btnGuardar);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Interfaz.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Interfaz.BORDER_COLOR),
                new EmptyBorder(14, 14, 14, 14)
        ));
        panel.setPreferredSize(new Dimension(780, 300));

        JLabel subtitulo = new JLabel("Parametros registrados");
        subtitulo.setForeground(java.awt.Color.WHITE);
        subtitulo.setFont(new Font("SansSerif", Font.BOLD, 16));

        panel.add(subtitulo, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);

        JPanel contenedor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contenedor.setBackground(Interfaz.BG_MAIN);
        contenedor.add(panel);
        add(contenedor, BorderLayout.CENTER);
        refrescar();
    }

    public final void refrescar() {
        try {
            service.recargar();
        } catch (InventarioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        ParametrosConfiguracion cfg = service.obtenerConfiguracion();
        if (cfg == null) {
            limpiarCampos();
            return;
        }
        txtCostoPedido.setText(FORMATO_MONEDA.format(cfg.getCostoPedido()));
        txtCostoMantenimiento.setText(FORMATO_MONEDA.format(cfg.getCostoMantenimiento()));
        txtTiempoEntrega.setText(String.valueOf(cfg.getTiempoEntrega()));
    }

    private void prepararCampo(JTextField campo) {
        campo.setPreferredSize(new Dimension(210, 34));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private JPanel crearCampo(String etiqueta, JTextField campo) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Interfaz.BG_PANEL);
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(Interfaz.TEXT_LIGHT);
        lbl.setPreferredSize(new Dimension(390, 34));
        panel.add(lbl);
        panel.add(campo);
        return panel;
    }

    private void guardar() {
        List<String> errores = new ArrayList<>();
        Double costoPedido = leerDecimalPositivo(txtCostoPedido.getText(), "Costo por pedido", errores);
        Double costoMantenimiento = leerDecimalPositivo(txtCostoMantenimiento.getText(), "Costo de mantenimiento", errores);
        Integer tiempoEntrega = leerEnteroPositivo(txtTiempoEntrega.getText(), "Tiempo de entrega", errores);

        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }

        try {
            service.guardarConfiguracion(new ParametrosConfiguracion(costoPedido, costoMantenimiento, tiempoEntrega));
            refrescar();
            JOptionPane.showMessageDialog(this, "Configuracion guardada correctamente.", "Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (InventarioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtCostoPedido.setText("");
        txtCostoMantenimiento.setText("");
        txtTiempoEntrega.setText("");
    }

    private Double leerDecimalPositivo(String valor, String campo, List<String> errores) {
        String limpio = valor == null ? "" : valor.trim().replace(",", ".");
        if (limpio.isEmpty()) {
            errores.add(campo + ": es obligatorio.");
            return null;
        }
        try {
            double numero = Double.parseDouble(limpio);
            if (numero <= 0) {
                errores.add(campo + ": debe ser mayor que 0.");
                return null;
            }
            return numero;
        } catch (NumberFormatException ex) {
            errores.add(campo + ": debe ser numerico.");
            return null;
        }
    }

    private Integer leerEnteroPositivo(String valor, String campo, List<String> errores) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            errores.add(campo + ": es obligatorio.");
            return null;
        }
        if (!limpio.matches("\\d+")) {
            errores.add(campo + ": debe ser un numero entero, sin decimales.");
            return null;
        }
        int numero = Integer.parseInt(limpio);
        if (numero <= 0) {
            errores.add(campo + ": debe ser mayor que 0.");
            return null;
        }
        return numero;
    }

    private void mostrarErrores(List<String> errores) {
        StringBuilder mensaje = new StringBuilder("Revisa estos datos:");
        for (String error : errores) {
            mensaje.append(System.lineSeparator()).append("- ").append(error);
        }
        JOptionPane.showMessageDialog(this, mensaje.toString(), "Validacion", JOptionPane.WARNING_MESSAGE);
    }
}
