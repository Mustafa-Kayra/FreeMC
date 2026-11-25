package dev.freemc.game;

import dev.freemc.auth.OfflineAuth;
import dev.freemc.mods.Modrinth;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A profile's own skin. The file lives with the profile; applying it installs CustomSkinLoader and
 * drops the texture where that mod looks for local skins, which is what makes offline skins work.
 */
public final class Skin {

    private Skin() {
    }

    public static Path file(Profile profile) {
        return profile.dir().resolve("skin.png");
    }

    public static boolean exists(Profile profile) {
        return profile != null && Files.isRegularFile(file(profile));
    }

    /** Accepts 64x64 (modern) and 64x32 (legacy) skins. */
    public static void save(Profile profile, Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("Bu dosya bir görsel değil: " + source.getFileName());
        }
        if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
            throw new IOException("Skin 64×64 (veya 64×32) olmalı, bu " + image.getWidth() + "×" + image.getHeight());
        }
        Files.createDirectories(profile.dir());
        Files.copy(source, file(profile), StandardCopyOption.REPLACE_EXISTING);
    }

    /** The skin to draw in the interface: the profile's own, or the default when none is set. */
    public static BufferedImage display(Profile profile) {
        BufferedImage own = image(profile);
        return own != null ? own : dev.freemc.ui.SkinTemplates.steve();
    }

    public static void clear(Profile profile) throws IOException {
        Files.deleteIfExists(file(profile));
        Path applied = appliedPath(profile, OfflineAuth.sanitize("Player"));
        Files.deleteIfExists(applied);
    }

    public static BufferedImage image(Profile profile) {
        if (!exists(profile)) {
            return null;
        }
        try {
            return ImageIO.read(file(profile).toFile());
        } catch (IOException e) {
            return null;
        }
    }

    /** Renders the head (face plus hat layer) at the requested scale. */
    public static BufferedImage head(BufferedImage skin, int scale) {
        if (skin == null) {
            return null;
        }
        int size = 8 * Math.max(1, scale);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        draw(g, skin, 8, 8, 8, 8, 0, 0, size);
        if (skin.getHeight() >= 64) {
            draw(g, skin, 40, 8, 8, 8, 0, 0, size);
        }
        g.dispose();
        return out;
    }

    private static void draw(Graphics2D g, BufferedImage source, int sx, int sy, int sw, int sh,
                             int dx, int dy, int size) {
        if (sx + sw > source.getWidth() || sy + sh > source.getHeight()) {
            return;
        }
        g.drawImage(source, dx, dy, dx + size, dy + size, sx, sy, sx + sw, sy + sh, null);
    }

    public static Path capeFile(Profile profile) {
        return profile.dir().resolve("cape.png");
    }

    public static boolean hasCape(Profile profile) {
        return profile != null && Files.isRegularFile(capeFile(profile));
    }

    public static void saveCape(Profile profile, Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null || image.getWidth() < 22) {
            throw new IOException("Cape dosyası geçersiz: " + source.getFileName());
        }
        Files.copy(source, capeFile(profile), StandardCopyOption.REPLACE_EXISTING);
    }

    public static BufferedImage cape(Profile profile) {
        if (!hasCape(profile)) {
            return null;
        }
        try {
            return ImageIO.read(capeFile(profile).toFile());
        } catch (IOException e) {
            return null;
        }
    }

    /** Front view of the whole skin: head, torso, arms and legs. */
    public static BufferedImage body(BufferedImage skin, int scale) {
        if (skin == null) {
            return null;
        }
        int unit = Math.max(2, scale);
        int width = 16 * unit;
        int height = 32 * unit;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        part(g, skin, 8, 8, 8, 8, 4, 0, unit);
        part(g, skin, 20, 20, 8, 12, 4, 8, unit);
        part(g, skin, 44, 20, 4, 12, 0, 8, unit);
        part(g, skin, 36, 52, 4, 12, 12, 8, unit);
        part(g, skin, 4, 20, 4, 12, 4, 20, unit);
        part(g, skin, 20, 52, 4, 12, 8, 20, unit);
        if (skin.getHeight() >= 64) {
            part(g, skin, 40, 8, 8, 8, 4, 0, unit);
            part(g, skin, 20, 36, 8, 12, 4, 8, unit);
            part(g, skin, 44, 36, 4, 12, 0, 8, unit);
            part(g, skin, 52, 52, 4, 12, 12, 8, unit);
            part(g, skin, 4, 36, 4, 12, 4, 20, unit);
            part(g, skin, 4, 52, 4, 12, 8, 20, unit);
        }
        g.dispose();
        return out;
    }

    private static void part(Graphics2D g, BufferedImage source, int sx, int sy, int sw, int sh,
                             int ux, int uy, int unit) {
        if (sx + sw > source.getWidth() || sy + sh > source.getHeight()) {
            return;
        }
        int dx = ux * unit;
        int dy = uy * unit;
        g.drawImage(source, dx, dy, dx + sw * unit, dy + sh * unit, sx, sy, sx + sw, sy + sh, null);
    }

    public static Path appliedPath(Profile profile, String username) {
        return profile.gameDir().resolve("CustomSkinLoader").resolve("LocalSkin").resolve("skins")
                .resolve(username + ".png");
    }

    /**
     * Installs CustomSkinLoader for the profile and copies the skin into its local folder.
     * Returns the path the mod reads.
     */
    public static Path apply(Profile profile, String username, Progress progress) throws IOException {
        if (!profile.modded()) {
            throw new IOException("Kostüm için Fabric, Forge veya NeoForge profili gerekiyor.");
        }
        if (!exists(profile)) {
            throw new IOException("Önce bir skin dosyası seç.");
        }
        Modrinth.install("customskinloader", profile.modsDir(), profile.mcVersion, profile.loader, progress);
        String name = OfflineAuth.sanitize(username);
        Path target = appliedPath(profile, name);
        Files.createDirectories(target.getParent());
        Files.copy(file(profile), target, StandardCopyOption.REPLACE_EXISTING);
        if (hasCape(profile)) {
            Path capeTarget = target.getParent().getParent().resolve("capes").resolve(name + ".png");
            Files.createDirectories(capeTarget.getParent());
            Files.copy(capeFile(profile), capeTarget, StandardCopyOption.REPLACE_EXISTING);
        }
        progress.log("Kostüm uygulandı: " + target);
        return target;
    }
}
