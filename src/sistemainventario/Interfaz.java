
package sistemainventario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;

/**
 * Utilidades visuales compartidas para mantener estilo consistente en Swing.
 */
public final class Interfaz {

    public static final Color BG_MAIN = new Color(25, 25, 25);
    public static final Color BG_SIDEBAR = new Color(30, 30, 32);
    public static final Color BG_PANEL = new Color(35, 35, 38);
    public static final Color TEXT_LIGHT = new Color(230, 230, 230);
    public static final Color TEXT_MUTED = new Color(150, 150, 150);
    public static final Color ACCENT_BLUE = new Color(60, 130, 246);
    public static final Color BORDER_COLOR = new Color(60, 60, 60);

    private Interfaz() {
    }

    /**
     * Boton principal (accion primaria).
     */
    public static JButton botonPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Boton secundario (accion auxiliar).
     */
    public static JButton botonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(BG_PANEL);
        btn.setForeground(TEXT_LIGHT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(6, 15, 6, 15)
        ));
        return btn;
    }

    /**
     * Campo de texto con estilo base.
     */
    public static JTextField input() {
        JTextField txt = new JTextField();
        txt.setBackground(BG_PANEL);
        txt.setForeground(TEXT_LIGHT);
        txt.setCaretColor(Color.WHITE);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return txt;
    }

    /**
     * Aplica estilo visual base a un combo o componente similar.
     */
    public static void estilizarCombo(JComponent combo) {
        combo.setBackground(BG_PANEL);
        combo.setForeground(TEXT_LIGHT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }
}
