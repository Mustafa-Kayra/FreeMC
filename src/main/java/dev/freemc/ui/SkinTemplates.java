package dev.freemc.ui;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** The skins the editor starts from, painted in code so no texture files need shipping. */
public final class SkinTemplates {

    /** UV layout of the classic 64x64 skin, one entry per face. */
    private record Face(int x, int y, int w, int h) {
    }

    private SkinTemplates() {
    }

    public static final String[] NAMES = {"Steve", "Alex", "Boş"};

    public static BufferedImage create(String name) {
        return switch (name) {
            case "Alex" -> alex();
            case "Boş" -> empty();
            default -> steve();
        };
    }

    public static BufferedImage empty() {
        return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage steve() {
        BufferedImage skin = empty();
        Graphics2D g = skin.createGraphics();

        Color skinTone = new Color(0xC6, 0x9C, 0x6D);
        Color skinShade = new Color(0xA8, 0x7E, 0x53);
        Color hair = new Color(0x39, 0x23, 0x1A);
        Color shirt = new Color(0x00, 0xA8, 0xA8);
        Color shirtShade = new Color(0x00, 0x8B, 0x8B);
        Color pants = new Color(0x3B, 0x3B, 0xA8);
        Color shoes = new Color(0x4A, 0x4A, 0x4A);

        // head: skin all around, hair on top and the back of the skull
        fill(g, skinTone, 8, 8, 8, 8);
        fill(g, skinShade, 0, 8, 8, 8);
        fill(g, skinTone, 16, 8, 8, 8);
        fill(g, skinTone, 24, 8, 8, 8);
        fill(g, hair, 8, 0, 8, 8);
        fill(g, hair, 16, 0, 8, 8);
        fill(g, hair, 24, 8, 8, 3);
        fill(g, skinShade, 16, 0, 8, 2);

        // face: eyes, nose, mouth, hair fringe
        fill(g, hair, 8, 8, 8, 2);
        fill(g, Color.WHITE, 9, 11, 2, 1);
        fill(g, Color.WHITE, 13, 11, 2, 1);
        fill(g, new Color(0x3B, 0x3B, 0x8C), 10, 11, 1, 1);
        fill(g, new Color(0x3B, 0x3B, 0x8C), 14, 11, 1, 1);
        fill(g, skinShade, 11, 12, 2, 1);
        fill(g, new Color(0x6B, 0x3F, 0x33), 11, 14, 2, 1);

        // torso and arms: shirt, hands in skin
        fill(g, shirt, 20, 20, 8, 12);
        fill(g, shirtShade, 16, 20, 4, 12);
        fill(g, shirtShade, 28, 20, 4, 12);
        fill(g, shirtShade, 32, 20, 8, 12);
        fill(g, shirtShade, 20, 16, 8, 4);
        fill(g, shirtShade, 44, 20, 4, 12);
        fill(g, shirtShade, 40, 20, 4, 12);
        fill(g, shirtShade, 48, 20, 4, 12);
        fill(g, shirt, 52, 20, 4, 12);
        fill(g, shirt, 48, 16, 4, 4);
        fill(g, skinTone, 44, 28, 4, 4);
        fill(g, skinTone, 36, 60, 4, 4);
        fill(g, shirtShade, 36, 52, 4, 12);
        fill(g, shirtShade, 32, 52, 4, 12);
        fill(g, shirtShade, 40, 52, 4, 12);
        fill(g, shirt, 44, 52, 4, 12);

        // legs and shoes
        fill(g, pants, 4, 20, 4, 12);
        fill(g, pants, 0, 20, 4, 12);
        fill(g, pants, 8, 20, 4, 12);
        fill(g, pants, 12, 20, 4, 12);
        fill(g, shoes, 4, 30, 4, 2);
        fill(g, shoes, 0, 30, 4, 2);
        fill(g, pants, 20, 52, 4, 12);
        fill(g, pants, 16, 52, 4, 12);
        fill(g, pants, 24, 52, 4, 12);
        fill(g, pants, 28, 52, 4, 12);
        fill(g, shoes, 20, 62, 4, 2);
        fill(g, shoes, 16, 62, 4, 2);
        fill(g, pants, 28, 48, 4, 4);

        g.dispose();
        return skin;
    }

    public static BufferedImage alex() {
        BufferedImage skin = empty();
        Graphics2D g = skin.createGraphics();

        Color skinTone = new Color(0xF2, 0xD2, 0xB3);
        Color skinShade = new Color(0xDB, 0xB4, 0x92);
        Color hair = new Color(0xD7, 0x7A, 0x33);
        Color shirt = new Color(0x4E, 0x9A, 0x4B);
        Color shirtShade = new Color(0x3D, 0x7C, 0x3B);
        Color pants = new Color(0x6B, 0x5B, 0x44);
        Color shoes = new Color(0x3A, 0x32, 0x2A);

        fill(g, skinTone, 8, 8, 8, 8);
        fill(g, skinShade, 0, 8, 8, 8);
        fill(g, skinTone, 16, 8, 8, 8);
        fill(g, skinTone, 24, 8, 8, 8);
        fill(g, hair, 8, 0, 8, 8);
        fill(g, hair, 16, 0, 8, 8);
        fill(g, hair, 24, 8, 8, 4);
        fill(g, hair, 8, 8, 8, 3);
        fill(g, hair, 8, 8, 2, 6);
        fill(g, hair, 14, 8, 2, 6);

        fill(g, Color.WHITE, 10, 12, 2, 1);
        fill(g, Color.WHITE, 13, 12, 2, 1);
        fill(g, new Color(0x39, 0x5C, 0x2E), 11, 12, 1, 1);
        fill(g, new Color(0x39, 0x5C, 0x2E), 14, 12, 1, 1);
        fill(g, new Color(0x8C, 0x4B, 0x3F), 11, 15, 2, 1);

        fill(g, shirt, 20, 20, 8, 12);
        fill(g, shirtShade, 16, 20, 4, 12);
        fill(g, shirtShade, 28, 20, 4, 12);
        fill(g, shirtShade, 32, 20, 8, 12);
        fill(g, shirtShade, 20, 16, 8, 4);
        fill(g, shirt, 44, 20, 3, 12);
        fill(g, shirtShade, 40, 20, 3, 12);
        fill(g, shirtShade, 47, 20, 4, 12);
        fill(g, shirt, 51, 20, 5, 12);
        fill(g, shirt, 48, 16, 4, 4);
        fill(g, skinTone, 44, 28, 3, 4);
        fill(g, skinTone, 36, 60, 3, 4);
        fill(g, shirtShade, 36, 52, 3, 12);
        fill(g, shirtShade, 32, 52, 3, 12);
        fill(g, shirtShade, 39, 52, 4, 12);
        fill(g, shirt, 43, 52, 5, 12);

        fill(g, pants, 4, 20, 4, 12);
        fill(g, pants, 0, 20, 4, 12);
        fill(g, pants, 8, 20, 4, 12);
        fill(g, pants, 12, 20, 4, 12);
        fill(g, shoes, 4, 30, 4, 2);
        fill(g, shoes, 0, 30, 4, 2);
        fill(g, pants, 20, 52, 4, 12);
        fill(g, pants, 16, 52, 4, 12);
        fill(g, pants, 24, 52, 4, 12);
        fill(g, pants, 28, 52, 4, 12);
        fill(g, shoes, 20, 62, 4, 2);
        fill(g, shoes, 16, 62, 4, 2);

        g.dispose();
        return skin;
    }

    private static void fill(Graphics2D g, Color color, int x, int y, int w, int h) {
        g.setColor(color);
        g.fillRect(x, y, w, h);
    }

    /** Optional bundled texture, used only if someone drops one into resources. */
    public static BufferedImage fromResource(String name) {
        try (InputStream in = SkinTemplates.class.getResourceAsStream("/skins/" + name)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }
}
