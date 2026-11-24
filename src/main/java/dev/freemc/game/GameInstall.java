package dev.freemc.game;

import java.nio.file.Path;
import java.util.List;

/** Everything the launcher needs on disk after installation. */
public final class GameInstall {

    public final Version version;
    public final Path gameDir;
    public final Path assetsRoot;
    public final Path nativesDir;
    public final List<Path> classpath;

    GameInstall(Version version, Path gameDir, Path assetsRoot, Path nativesDir, List<Path> classpath) {
        this.version = version;
        this.gameDir = gameDir;
        this.assetsRoot = assetsRoot;
        this.nativesDir = nativesDir;
        this.classpath = classpath;
    }
}
