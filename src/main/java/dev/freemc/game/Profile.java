package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.json.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A named setup: version, loader, memory and its own game folder. */
public final class Profile {

    public static final String VANILLA = "vanilla";
    public static final String FABRIC = "fabric";
    public static final String FORGE = "forge";
    public static final String NEOFORGE = "neoforge";

    public final String id;
    public String name;
    public String mcVersion;
    public String loader;
    public String loaderVersion;
    public int ramMb;
    public String jvmArgs;
    public String quickServer;
    public String source;
    public long playSeconds;
    public long lastPlayed;

    public Profile(String id, String name, String mcVersion, String loader, String loaderVersion) {
        this.id = id;
        this.name = name;
        this.mcVersion = mcVersion;
        this.loader = normalizeLoader(loader);
        this.loaderVersion = loaderVersion == null ? "" : loaderVersion;
        this.ramMb = 4096;
        this.jvmArgs = "";
        this.quickServer = "";
        this.source = "";
    }

    public static String normalizeLoader(String loader) {
        if (loader == null) {
            return VANILLA;
        }
        String value = loader.toLowerCase(Locale.ROOT);
        return switch (value) {
            case FABRIC, FORGE, NEOFORGE -> value;
            default -> VANILLA;
        };
    }

    public static Path root() {
        return Dirs.ensure(Dirs.root().resolve("profiles"));
    }

    public Path dir() {
        return Dirs.ensure(root().resolve(id));
    }

    public Path gameDir() {
        return dir();
    }

    public Path modsDir() {
        return Dirs.ensure(dir().resolve("mods"));
    }

    public Path profileFile() {
        return dir().resolve("profile.json");
    }

    public boolean modded() {
        return !VANILLA.equals(loader);
    }

    public String loaderLabel() {
        if (loaderVersion.isEmpty()) {
            return switch (loader) {
                case FABRIC -> "Fabric";
                case FORGE -> "Forge";
                case NEOFORGE -> "NeoForge";
                default -> "Vanilla";
            };
        }
        return switch (loader) {
            case FABRIC -> "Fabric " + loaderVersion;
            case FORGE -> "Forge " + loaderVersion;
            case NEOFORGE -> "NeoForge " + loaderVersion;
            default -> "Vanilla";
        };
    }

    public String describe() {
        return mcVersion + " · " + loaderLabel();
    }

    public int modCount() {
        if (!Files.isDirectory(modsDir())) {
            return 0;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir(), "*.jar")) {
            int count = 0;
            for (Path ignored : stream) {
                count++;
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    public void addPlayTime(long seconds) {
        if (seconds > 0) {
            playSeconds += seconds;
        }
        lastPlayed = System.currentTimeMillis();
        save();
    }

    public void save() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("mcVersion", mcVersion);
        data.put("loader", loader);
        data.put("loaderVersion", loaderVersion);
        data.put("ramMb", ramMb);
        data.put("jvmArgs", jvmArgs);
        data.put("quickServer", quickServer);
        data.put("source", source);
        data.put("playSeconds", playSeconds);
        data.put("lastPlayed", lastPlayed);
        try {
            Files.createDirectories(dir());
            Files.writeString(profileFile(), Json.write(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // a profile that cannot be written still works for this session
        }
    }

    public static Profile load(Path dir) {
        Path file = dir.resolve("profile.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            Map<String, Object> data = Json.object(Files.readString(file, StandardCharsets.UTF_8));
            String id = Json.str(data, "id", dir.getFileName().toString());
            Profile profile = new Profile(id, Json.str(data, "name", id), Json.str(data, "mcVersion", ""),
                    Json.str(data, "loader", VANILLA), Json.str(data, "loaderVersion", ""));
            profile.ramMb = (int) Json.num(data, "ramMb", 4096);
            profile.jvmArgs = Json.str(data, "jvmArgs", "");
            profile.quickServer = Json.str(data, "quickServer", "");
            profile.source = Json.str(data, "source", "");
            profile.playSeconds = Json.num(data, "playSeconds", 0);
            profile.lastPlayed = Json.num(data, "lastPlayed", 0);
            return profile;
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public static List<Profile> all() {
        List<Profile> profiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root())) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                Profile profile = load(dir);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        } catch (IOException ignored) {
            // an unreadable profiles folder lists nothing
        }
        profiles.sort(Comparator.comparingLong((Profile p) -> p.lastPlayed).reversed());
        return profiles;
    }

    public static Profile create(String name, String mcVersion, String loader, String loaderVersion) {
        String base = slug(name);
        String id = base;
        int suffix = 2;
        while (Files.exists(root().resolve(id))) {
            id = base + "-" + suffix++;
        }
        Profile profile = new Profile(id, name, mcVersion, loader, loaderVersion);
        profile.save();
        profile.modsDir();
        return profile;
    }

    public static String slug(String name) {
        String slug = name == null ? "" : name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "profil-" + System.currentTimeMillis() % 100000 : slug;
    }

    public void delete() {
        try {
            deleteTree(dir());
        } catch (IOException ignored) {
            // leftovers are harmless
        }
    }

    private static void deleteTree(Path dir) throws IOException {
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
