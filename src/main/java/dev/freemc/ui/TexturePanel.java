package dev.freemc.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** A card that tiles a real Minecraft block texture behind its content. */
public final class TexturePanel extends RoundPanel {

    private final String[] texturePaths;

    public TexturePanel(int radius, String... texturePaths) {
        super(Theme.SURFACE_LOW, Theme.OUTLINE_SOFT, radius);
        this.texturePaths = texturePaths;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        Shape shape = new RoundRectangle2D.Double(0, 0, width - 1, height - 1, 12, 12);

        g.setColor(Theme.SURFACE_LOW);
        g.fill(shape);

        BufferedImage texture = null;
        for (String path : texturePaths) {
            texture = Images.texture(path);
            if (texture != null) {
                break;
            }
        }
        if (texture != null) {
            Shape previousClip = g.getClip();
            g.clip(shape);
            BufferedImage tile = Images.scale(texture, 48, 48, false);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
            for (int y = 0; y < height; y += 48) {
                for (int x = 0; x < width; x += 48) {
                    g.drawImage(tile, x, y, null);
                }
            }
            g.setComposite(AlphaComposite.SrcOver);
            g.setPaint(new GradientPaint(0, 0, withAlpha(Theme.SURFACE_LOW, 0.82f),
                    width, 0, withAlpha(Theme.SURFACE_LOW, 0.35f)));
            g.fill(shape);
            g.setClip(previousClip);
        }

        g.setColor(Theme.OUTLINE_SOFT);
        g.draw(shape);
        g.dispose();
        super.paintComponent(graphics);
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 255));
    }
}
