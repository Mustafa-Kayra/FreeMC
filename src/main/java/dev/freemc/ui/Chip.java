package dev.freemc.ui;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small pill label used for metadata and status badges. */
public final class Chip extends JLabel {

    private final Color fill;

    public Chip(String text, Color fill, Color ink, Font font) {
        super(text);
        this.fill = fill;
        setForeground(ink);
        setFont(font);
        setBorder(new EmptyBorder(3, 8, 3, 8));
        setOpaque(false);
    }

    public static Chip accent(String text) {
        return new Chip(text, Theme.ACCENT, Theme.ACCENT_INK, Theme.body(Font.PLAIN, 11f));
    }

    public static Chip muted(String text) {
        return new Chip(text, Theme.SURFACE_HIGH, Theme.TEXT_MUTED, Theme.body(Font.PLAIN, 11f));
    }

    public static Chip mint(String text) {
        return new Chip(text, Theme.MINT, Theme.MINT_INK, Theme.body(Font.PLAIN, 11f));
    }

    public static Chip danger(String text) {
        return new Chip(text, Theme.DANGER_BG, Theme.DANGER, Theme.body(Font.PLAIN, 11f));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
        g.dispose();
        super.paintComponent(graphics);
    }
}
