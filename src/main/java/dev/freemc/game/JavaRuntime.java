package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.Os;
import dev.freemc.json.Json;
import dev.freemc.net.Archive;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Downloads a matching Temurin JRE from Adoptium when the machine has none new enough, so a
 * version that needs Java 21 or 25 runs without the user installing anything.
 */
public final class JavaRuntime {

    private static final String API = "https://api.adoptium.net/v3/assets/latest/%d/hotspot?architecture=%s&image_type=jre&os=%s&vendor=eclipse";

    private JavaRuntime() {
    }

    public static String ensure(int major, Progress progress) throws IOException {
        String local = localExecutable(major);
        if (local != null) {
            return local;
        }
        if (major < 17) {
            return dev.freemc.Jvm.forVersion(major, progress::log);
        }

        progress.log("Java " + major + " indiriliyor (Adoptium)");
        String url = String.format(API, major, architecture(), platform());
        List<Object> assets = Json.array(Download.text(URI.create(url)));
        if (assets.isEmpty()) {
            return dev.freemc.Jvm.forVersion(major, progress::log);
        }
        Map<String, Object> pkg = Json.map(Json.map(assets.get(0), "binary"), "package");
        String link = Json.str(pkg, "link");
        String name = Json.str(pkg, "name", "jre.tar.gz");
        if (link == null) {
            return dev.freemc.Jvm.forVersion(major, progress::log);
        }

        Path jvmRoot = Dirs.ensure(Dirs.root().resolve("jvm"));
        Path archive = Dirs.ensure(jvmRoot.resolve("downloads")).resolve(name);
        Download.file(URI.create(link), archive, null, 0);

        Path staging = Dirs.ensure(jvmRoot.resolve("staging-" + major));
        deleteTree(staging);
        Archive.extract(archive, staging);

        Path home = findHome(staging);
        if (home == null) {
            throw new IOException("indirilen Java arşivi çözülemedi: " + name);
        }
        Path target = jvmRoot.resolve("jre-" + major);
        deleteTree(target);
        Files.move(home, target, StandardCopyOption.REPLACE_EXISTING);
        deleteTree(staging);
        makeExecutable(target);
        String executable = localExecutable(major);
        if (executable == null) {
            throw new IOException("Java " + major + " kurulamadı: " + target);
        }
        progress.log("Java " + major + " hazır: " + executable);
        return executable;
    }

    private static String localExecutable(int major) {
        Path home = Dirs.root().resolve("jvm").resolve("jre-" + major);
        Path bin = home.resolve("bin").resolve(Os.isWindows() ? "javaw.exe" : "java");
        if (!Files.isRegularFile(bin)) {
            bin = home.resolve("bin").resolve("java");
        }
        return Files.isRegularFile(bin) ? bin.toAbsolutePath().toString() : null;
    }

    /** Tar archives carry the executable bit in metadata we do not restore, so set it here. */
    private static void makeExecutable(Path home) {
        Path bin = home.resolve("bin");
        if (!Files.isDirectory(bin)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bin)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    file.toFile().setExecutable(true, false);
                }
            }
        } catch (IOException ignored) {
            // running the JVM will report a clearer error than this
        }
        home.toFile().setExecutable(true, false);
    }

    /** The directory that holds bin/java, which is the JVM home we keep. */
    private static Path findHome(Path root) throws IOException {
        if (Files.isRegularFile(root.resolve("bin").resolve("java"))
                || Files.isRegularFile(root.resolve("bin").resolve("java.exe"))) {
            return root;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    Path found = findHome(child);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private static String platform() {
        return switch (Os.NAME) {
            case "windows" -> "windows";
            case "osx" -> "mac";
            default -> "linux";
        };
    }

    private static String architecture() {
        return switch (Os.ARCH) {
            case "x86_64" -> "x64";
            case "arm64" -> "aarch64";
            default -> "x86";
        };
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    deleteTree(child);
                } else {
                    Files.deleteIfExists(child);
                }
            }
        }
        Files.deleteIfExists(dir);
    }
}
