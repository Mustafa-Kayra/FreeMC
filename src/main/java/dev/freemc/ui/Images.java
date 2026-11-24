package dev.freemc.ui;

import dev.freemc.Dirs;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Small image helpers: game textures for art, deterministic tiles for mod rows. */
public final class Images {

    private static final Map<String, BufferedImage> TEXTURES = new HashMap<>();
    private static boolean jarSearched;
    private static Path clientJar;

    private Images() {
    }

    /** A texture straight out of the installed client jar, or null when nothing is installed yet. */
    public static synchronized BufferedImage texture(String path) {
        BufferedImage cached = TEXTURES.get(path);
        if (cached != null) {
            return cached;
        }
        Path jar = clientJar();
        if (jar == null) {
            return null;
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("assets/minecraft/textures/" + path);
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    TEXTURES.put(path, image);
                }
                return image;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static synchronized Path clientJar() {
        if (jarSearched) {
            return clientJar;
        }
        jarSearched = true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Dirs.versions())) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, "*.jar")) {
                    for (Path jar : jars) {
                        clientJar = jar;
                        return clientJar;
                    }
                }
            }
        } catch (IOException ignored) {
            // no textures then, the gradient fallback stays
        }
        return clientJar;
    }

    public static BufferedImage scale(Image image, int width, int height, boolean smooth) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                smooth ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    /** Deterministic accent colour for a name, so every mod tile is stable across runs. */
    public static Color colorFor(String name) {
        int hash = name == null ? 0 : name.hashCode();
        float hue = Math.abs(hash % 360) / 360f;
        return Color.getHSBColor(hue, 0.45f, 0.55f);
    }

    /** Rounded square with the project's initials — used where no decodable artwork exists. */
    public static JComponent tile(String title, int size) {
        return new Tile(title, size);
    }

    private static final class Tile extends JComponent {
        private final String title;
        private final int size;

        Tile(String title, int size) {
            this.title = title == null ? "?" : title;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = colorFor(title);
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(base);
            g.fillRoundRect(0, 0, size, size, 8, 8);
            g.setColor(new Color(1f, 1f, 1f, 0.14f));
            g.fillRoundRect(0, 0, size, size / 2, 8, 8);
            g.setColor(new Color(1f, 1f, 1f, 0.92f));
            g.setFont(Theme.display(Font.BOLD, size * 0.42f));
            String initials = initials(title);
            FontMetrics metrics = g.getFontMetrics();
            int width = metrics.stringWidth(initials);
            g.drawString(initials, (size - width) / 2, size / 2 + metrics.getAscent() / 3);
            g.dispose();
        }

        private static String initials(String text) {
            String[] parts = text.trim().split("[\\s_\\-]+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty() && sb.length() < 2) {
                    sb.append(Character.toUpperCase(part.charAt(0)));
                }
            }
            if (sb.length() == 0 && !text.isEmpty()) {
                sb.append(Character.toUpperCase(text.charAt(0)));
            }
            return sb.toString();
        }
    }
}
