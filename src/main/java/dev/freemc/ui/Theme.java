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
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Palette, typography and widget factories shared by every page. */
public final class Theme {

    public static Color BG;
    public static Color SURFACE_LOW;
    public static Color SURFACE;
    public static Color SURFACE_HIGH;
    public static Color SURFACE_TOP;
    public static Color OUTLINE;
    public static Color OUTLINE_SOFT;
    public static Color TEXT;
    public static Color TEXT_MUTED;
    public static Color ACCENT;
    public static Color ACCENT_INK;
    public static Color MINT;
    public static Color MINT_INK;
    public static Color DANGER;
    public static Color DANGER_BG;

    private static final Map<String, Font> BASES = new HashMap<>();
    private static boolean dark = true;

    private Theme() {
    }

    public static void init(boolean darkTheme) {
        dark = darkTheme;
        if (dark) {
            BG = new Color(0x131412);
            SURFACE_LOW = new Color(0x1b1c1a);
            SURFACE = new Color(0x1f201e);
            SURFACE_HIGH = new Color(0x292a28);
            SURFACE_TOP = new Color(0x343533);
            OUTLINE = new Color(0x8d9479);
            OUTLINE_SOFT = new Color(0x434933);
            TEXT = new Color(0xe4e2df);
            TEXT_MUTED = new Color(0xc3caad);
            ACCENT = new Color(0xbaf500);
            ACCENT_INK = new Color(0x263500);
            MINT = new Color(0x00f4b9);
            MINT_INK = new Color(0x006b4f);
            DANGER = new Color(0xffb4ab);
            DANGER_BG = new Color(0x5c1410);
        } else {
            BG = new Color(0xf4f4ee);
            SURFACE_LOW = new Color(0xffffff);
            SURFACE = new Color(0xeeeeE6);
            SURFACE_HIGH = new Color(0xe2e3d8);
            SURFACE_TOP = new Color(0xd2d4c3);
            OUTLINE = new Color(0x6b7060);
            OUTLINE_SOFT = new Color(0xc8cab8);
            TEXT = new Color(0x171814);
            TEXT_MUTED = new Color(0x4a5044);
            ACCENT = new Color(0x9ed600);
            ACCENT_INK = new Color(0x1d2800);
            MINT = new Color(0x008f6b);
            MINT_INK = new Color(0xe8fff5);
            DANGER = new Color(0x9c1c14);
            DANGER_BG = new Color(0xffdad6);
        }
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // the platform look and feel works too
        }
    }

    public static boolean isDark() {
        return dark;
    }

    public static Font display(float size) {
        return display(Font.BOLD, size);
    }

    public static Font display(int style, float size) {
        Font base = load("ArchivoNarrow-Bold.ttf", "ArchivoNarrow-Medium.ttf");
        return (base != null ? base : new Font(Font.SANS_SERIF, style, 12)).deriveFont(style, size);
    }

    public static Font displayTracked(float size) {
        Font font = display(Font.BOLD, size);
        Map<java.awt.font.TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, 0.09);
        return font.deriveFont(attributes);
    }

    public static Font body(float size) {
        return body(Font.PLAIN, size);
    }

    public static Font body(int style, float size) {
        Font base = load("Geist-Regular.ttf", "Geist-SemiBold.ttf");
        return (base != null ? base : new Font(Font.SANS_SERIF, style, 12)).deriveFont(style, size);
    }

    public static Font mono(float size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
    }

    private static Font load(String bold, String regular) {
        String key = bold + "|" + regular;
        Font cached = BASES.get(key);
        if (cached != null) {
            return cached;
        }
        for (String name : new String[]{bold, regular}) {
            try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + name)) {
                if (in == null) {
                    continue;
                }
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                BASES.put(key, font);
                return font;
            } catch (FontFormatException | IOException ignored) {
                // fall back to the system font
            }
        }
        return null;
    }

    public static RoundPanel card(LayoutManager layout, int padding) {
        RoundPanel panel = new RoundPanel(SURFACE_LOW, OUTLINE_SOFT, 12);
        panel.setLayout(layout);
        if (padding > 0) {
            panel.setBorder(new EmptyBorder(padding, padding + 2, padding, padding + 2));
        }
        return panel;
    }

    public static RoundPanel panel(Color fill, int radius) {
        return new RoundPanel(fill, null, radius);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JLabel heading(String text) {
        return label(text, displayTracked(11f), OUTLINE);
    }

    public static JLabel title(String text, float size) {
        JLabel label = label(text, display(size), TEXT);
        return label;
    }

    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    public static void styleInput(JTextField field) {
        field.setBackground(SURFACE_HIGH);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(body(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE_SOFT),
                new EmptyBorder(7, 10, 7, 10)));
    }

    public static <T> JComboBox<T> combo() {
        JComboBox<T> combo = new JComboBox<>();
        styleCombo(combo);
        return combo;
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(SURFACE_HIGH);
        combo.setForeground(TEXT);
        combo.setFont(body(13f));
        combo.setBorder(BorderFactory.createLineBorder(OUTLINE_SOFT));
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setBackground(SURFACE_HIGH);
        spinner.setForeground(TEXT);
        spinner.setFont(body(13f));
        spinner.setBorder(BorderFactory.createLineBorder(OUTLINE_SOFT));
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            styleInput(editor.getTextField());
            editor.getTextField().setBorder(new EmptyBorder(5, 8, 5, 8));
            editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    public static void styleProgress(JProgressBar bar) {
        bar.setBackground(SURFACE_HIGH);
        bar.setForeground(ACCENT);
        bar.setBorderPainted(false);
        bar.setStringPainted(true);
        bar.setString("");
        bar.setFont(mono(10));
        bar.setForeground(ACCENT);
    }

    public static void styleLog(JTextArea area) {
        area.setBackground(SURFACE_LOW);
        area.setForeground(TEXT_MUTED);
        area.setCaretColor(ACCENT);
        area.setFont(mono(11));
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    public static void transparent(JComponent component) {
        component.setOpaque(false);
    }
}
