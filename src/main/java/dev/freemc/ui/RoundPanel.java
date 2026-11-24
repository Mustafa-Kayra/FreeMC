package dev.freemc.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A panel with a rounded background and a hairline border. */
public class RoundPanel extends JPanel {

    private final Color fill;
    private final Color border;
    private final int radius;

    public RoundPanel(Color fill, Color border, int radius) {
        this.fill = fill;
        this.border = border;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        g.setColor(fill);
        g.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        if (border != null) {
            g.setColor(border);
            g.drawRoundRect(0, 0, width - 1, height - 1, radius, radius);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
