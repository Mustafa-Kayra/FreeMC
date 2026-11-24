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

/** Flat, rounded button drawn by hand so the theme stays consistent on every platform. */
public final class FlatButton extends JButton {

    public enum Kind { PRIMARY, GHOST, DANGER, TAB }

    private final Kind kind;
    private boolean hovered;
    private boolean active;

    public FlatButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(kind == Kind.PRIMARY ? new Font(Font.SANS_SERIF, Font.BOLD, 14) : Theme.BODY);
        setForeground(kind == Kind.PRIMARY ? Theme.ACCENT_INK : Theme.TEXT);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
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

    public FlatButton active(boolean value) {
        this.active = value;
        repaint();
        return this;
    }

    private java.awt.Insets padding() {
        return switch (kind) {
            case PRIMARY -> new java.awt.Insets(11, 26, 11, 26);
            case TAB -> new java.awt.Insets(8, 16, 8, 16);
            case DANGER, GHOST -> new java.awt.Insets(7, 14, 7, 14);
        };
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, kind == Kind.PRIMARY ? 44 : 32);
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
        int radius = kind == Kind.PRIMARY ? 12 : 10;

        Color background = switch (kind) {
            case PRIMARY -> enabled ? (hovered ? Theme.ACCENT.brighter() : Theme.ACCENT) : Theme.SURFACE_ALT;
            case GHOST -> active ? Theme.SURFACE_ALT : (hovered ? Theme.SURFACE_ALT : new Color(0, 0, 0, 0));
            case TAB -> active ? Theme.SURFACE_ALT : (hovered ? new Color(0x1A1A24) : new Color(0, 0, 0, 0));
            case DANGER -> hovered ? new Color(0x3A1E22) : new Color(0x24161A);
        };
        if (background.getAlpha() > 0) {
            g.setColor(background);
            g.fillRoundRect(0, 0, width, height, radius, radius);
        }
        if (kind == Kind.TAB && active) {
            g.setColor(Theme.ACCENT);
            g.fillRoundRect(10, height - 3, width - 20, 2, 2, 2);
        }

        Color ink = switch (kind) {
            case PRIMARY -> enabled ? Theme.ACCENT_INK : Theme.MUTED;
            case DANGER -> Theme.DANGER;
            default -> enabled ? (active ? Theme.ACCENT : Theme.TEXT) : Theme.MUTED;
        };
        g.setColor(ink);
        g.setFont(getFont());
        FontMetrics metrics = g.getFontMetrics();
        String text = getText();
        int textWidth = metrics.stringWidth(text);
        int x = (width - textWidth) / 2;
        int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, x, y);
        g.dispose();
    }
}
