package dev.freemc.ui;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Flat rounded button with an optional drawn glyph, matching the launcher theme. */
public final class FlatButton extends JButton {

    public enum Kind { PRIMARY, GHOST, DANGER, NAV, TAB, ICON }

    private final Kind kind;
    private String icon;
    private boolean hovered;
    private boolean active;

    public FlatButton(String text, Kind kind) {
        super(text == null ? "" : text);
        this.kind = kind;
        setFont(kind == Kind.PRIMARY ? Theme.display(Font.BOLD, 17f)
                : kind == Kind.NAV ? Theme.body(Font.PLAIN, 13f) : Theme.body(Font.PLAIN, 12f));
        setForeground(Theme.TEXT);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(padding().top, padding().left, padding().bottom, padding().right));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    public FlatButton icon(String iconName) {
        this.icon = iconName;
        return this;
    }

    public FlatButton active(boolean value) {
        this.active = value;
        repaint();
        return this;
    }

    private java.awt.Insets padding() {
        return switch (kind) {
            case PRIMARY -> new java.awt.Insets(14, 30, 14, 30);
            case NAV -> new java.awt.Insets(9, 12, 9, 12);
            case TAB -> new java.awt.Insets(7, 14, 7, 14);
            case ICON -> new java.awt.Insets(6, 6, 6, 6);
            case DANGER, GHOST -> new java.awt.Insets(7, 12, 7, 12);
        };
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, switch (kind) {
            case PRIMARY -> 52;
            case ICON -> 30;
            default -> 32;
        });
        return size;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean enabled = isEnabled();
        int width = getWidth();
        int height = getHeight();
        int radius = kind == Kind.PRIMARY ? 10 : 8;

        Color background = switch (kind) {
            case PRIMARY -> enabled ? (hovered ? Theme.ACCENT.brighter() : Theme.ACCENT) : Theme.SURFACE_HIGH;
            case NAV -> active ? Theme.SURFACE_TOP : (hovered ? Theme.SURFACE_HIGH : null);
            case TAB -> active ? Theme.SURFACE_TOP : (hovered ? Theme.SURFACE : null);
            case GHOST -> hovered ? Theme.SURFACE_HIGH : null;
            case ICON -> hovered ? Theme.SURFACE_HIGH : null;
            case DANGER -> hovered ? Theme.DANGER_BG : null;
        };
        if (background != null) {
            g.setColor(background);
            g.fillRoundRect(0, 0, width, height, radius, radius);
        }

        Color ink = switch (kind) {
            case PRIMARY -> enabled ? Theme.ACCENT_INK : Theme.OUTLINE;
            case DANGER -> Theme.DANGER;
            case NAV, TAB -> !enabled ? Theme.OUTLINE : active ? Theme.ACCENT : Theme.TEXT_MUTED;
            default -> enabled ? (active ? Theme.ACCENT : Theme.TEXT) : Theme.OUTLINE;
        };

        g.setColor(ink);
        g.setFont(getFont());
        FontMetrics metrics = g.getFontMetrics();
        String text = getText();
        int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();

        int textWidth = metrics.stringWidth(text);
        int glyph = 16;
        int gap = icon == null ? 0 : 8;
        int block = textWidth + (icon == null ? 0 : glyph + gap);
        int x = kind == Kind.NAV ? getInsets().left : (width - block) / 2;

        if (icon != null) {
            Graphics2D iconGraphics = (Graphics2D) g.create();
            iconGraphics.translate(x, (height - glyph) / 2.0);
            Icons.draw(iconGraphics, icon, glyph, ink);
            iconGraphics.dispose();
            x += glyph + gap;
        }
        g.setColor(ink);
        g.drawString(text, x, textY);
        g.dispose();
    }
}
