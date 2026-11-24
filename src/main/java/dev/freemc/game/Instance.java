package dev.freemc.game;

import dev.freemc.Dirs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** One playable setup: a Minecraft version plus the mod loader layered on it. */
public final class Instance {

    public static final String VANILLA = "vanilla";
    public static final String FABRIC = "fabric";

    public final String mcVersion;
    public final String loader;
    private final Path dir;

    private Instance(String mcVersion, String loader) {
        this.mcVersion = mcVersion;
        this.loader = loader;
        this.dir = Dirs.instance(id(mcVersion, loader));
    }

    public static Instance of(String mcVersion, String loader) {
        String normalized = FABRIC.equals(loader) ? FABRIC : VANILLA;
        return new Instance(mcVersion, normalized);
    }

    public static String id(String mcVersion, String loader) {
        return FABRIC.equals(loader) ? mcVersion + "-fabric" : mcVersion;
    }

    public String id() {
        return id(mcVersion, loader);
    }

    public Path gameDir() {
        return dir;
    }

    public Path modsDir() {
        Path mods = dir.resolve("mods");
        Dirs.ensure(mods);
        return mods;
    }

    public String loaderVersion() {
        Path file = dir.resolve("loader.txt");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    public void loaderVersion(String version) {
        try {
            Files.writeString(dir.resolve("loader.txt"), version == null ? "" : version, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // the loader version is only informational
        }
    }

    public String describe() {
        if (FABRIC.equals(loader)) {
            String version = loaderVersion();
            return mcVersion + " · Fabric" + (version == null ? "" : " " + version);
        }
        return mcVersion + " · Vanilla";
    }
}
