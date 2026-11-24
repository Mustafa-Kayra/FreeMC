package dev.freemc.ui;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;

/** Colours, fonts and the small factory methods every panel shares. */
public final class Theme {

    public static final Color BG = new Color(0x0E0E15);
    public static final Color SURFACE = new Color(0x16161F);
    public static final Color SURFACE_ALT = new Color(0x1D1D28);
    public static final Color FIELD = new Color(0x101018);
    public static final Color BORDER = new Color(0x2A2A38);
    public static final Color TEXT = new Color(0xEDEDF4);
    public static final Color MUTED = new Color(0x8C8CA1);
    public static final Color ACCENT = new Color(0x4ADE80);
    public static final Color ACCENT_INK = new Color(0x04120A);
    public static final Color DANGER = new Color(0xF87171);

    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    public static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    public static final Font INPUT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private Theme() {
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("Panel.background", BG);
            UIManager.put("OptionPane.background", SURFACE);
            UIManager.put("OptionPane.messageForeground", TEXT);
        } catch (Exception ignored) {
            // the platform look and feel is acceptable
        }
    }

    public static RoundPanel card(LayoutManager layout, int padding) {
        RoundPanel panel = new RoundPanel(SURFACE, BORDER, 14);
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(padding, padding + 2, padding, padding + 2));
        return panel;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    public static void styleInput(JTextField field) {
        field.setBackground(FIELD);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(INPUT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(7, 9, 7, 9)));
    }

    public static <T> JComboBox<T> combo() {
        JComboBox<T> combo = new JComboBox<>();
        styleCombo(combo);
        return combo;
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(FIELD);
        combo.setForeground(TEXT);
        combo.setFont(INPUT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setBackground(FIELD);
        spinner.setForeground(TEXT);
        spinner.setFont(INPUT);
        spinner.setBorder(BorderFactory.createLineBorder(BORDER));
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            styleInput(editor.getTextField());
            editor.getTextField().setBorder(new EmptyBorder(5, 8, 5, 8));
            editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    public static void styleProgress(JProgressBar bar) {
        bar.setBackground(SURFACE_ALT);
        bar.setForeground(ACCENT);
        bar.setBorderPainted(false);
        bar.setStringPainted(true);
        bar.setString("");
        bar.setFont(SMALL);
    }

    public static void styleLog(JTextArea area) {
        area.setBackground(FIELD);
        area.setForeground(new Color(0xC5C5D4));
        area.setCaretColor(ACCENT);
        area.setFont(MONO);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    public static void transparent(JComponent component) {
        component.setOpaque(false);
    }
}
