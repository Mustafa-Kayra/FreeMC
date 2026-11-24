package dev.freemc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Root data directory, identical in layout on Windows and Linux. */
public final class Dirs {

    private static final String APP = "FreeMC";
    private static Path root;

    private Dirs() {
    }

    public static synchronized Path root() {
        if (root == null) {
            String override = System.getProperty("freemc.home");
            if (override != null && !override.isBlank()) {
                root = Paths.get(override).toAbsolutePath().normalize();
            } else if (Os.isWindows()) {
                String appData = System.getenv("APPDATA");
                Path base = (appData == null || appData.isBlank())
                        ? Paths.get(System.getProperty("user.home"))
                        : Paths.get(appData);
                root = base.resolve(APP);
            } else {
                root = Paths.get(System.getProperty("user.home")).resolve(".freemc");
            }
        }
        return root;
    }

    public static Path versions() {
        return ensure(root().resolve("versions"));
    }

    public static Path libraries() {
        return ensure(root().resolve("libraries"));
    }

    public static Path assets() {
        return ensure(root().resolve("assets"));
    }

    public static Path instances() {
        return ensure(root().resolve("instances"));
    }

    public static Path instance(String instanceId) {
        Path dir = ensure(instances().resolve(instanceId));
        ensure(dir.resolve("mods"));
        return dir;
    }

    public static Path versionDir(String versionId) {
        return ensure(versions().resolve(versionId));
    }

    public static Path nativesDir(String versionId) {
        return versionDir(versionId).resolve("natives");
    }

    public static Path ensure(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("klasör oluşturulamadı: " + path, e);
        }
        return path;
    }
}
