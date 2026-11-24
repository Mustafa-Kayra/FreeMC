package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Forge and NeoForge, installed by running their own installer headlessly. */
public final class Forge {

    private static final String PROMOTIONS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge/";
    private static final String NEOFORGE_META = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String NEOFORGE_MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private Forge() {
    }

    /** Newest promoted Forge build for a Minecraft version. */
    public static String forgeVersion(String mcVersion) throws IOException {
        Map<String, Object> root = Json.object(Download.text(URI.create(PROMOTIONS)));
        Map<String, Object> promos = Json.map(root, "promos");
        String recommended = Json.str(promos, mcVersion + "-recommended");
        if (recommended != null) {
            return recommended;
        }
        String latest = Json.str(promos, mcVersion + "-latest");
        if (latest == null) {
            throw new IOException("Bu sürüm için Forge yok: " + mcVersion);
        }
        return latest;
    }

    /** Newest NeoForge build whose version prefix matches the Minecraft version. */
    public static String neoForgeVersion(String mcVersion) throws IOException {
        String prefix = neoPrefix(mcVersion);
        String xml = Download.text(URI.create(NEOFORGE_META));
        List<String> versions = new ArrayList<>();
        int index = 0;
        while (true) {
            int start = xml.indexOf("<version>", index);
            if (start < 0) {
                break;
            }
            int end = xml.indexOf("</version>", start);
            if (end < 0) {
                break;
            }
            String value = xml.substring(start + 9, end).trim();
            if (value.startsWith(prefix) && !value.contains("beta") && !value.contains("alpha")) {
                versions.add(value);
            }
            index = end;
        }
        if (versions.isEmpty()) {
            throw new IOException("Bu sürüm için NeoForge yok: " + mcVersion);
        }
        return versions.get(versions.size() - 1);
    }

    private static String neoPrefix(String mcVersion) {
        String[] parts = mcVersion.split("\\.");
        if (parts.length < 2) {
            return mcVersion;
        }
        String minor = parts[1];
        String patch = parts.length > 2 ? parts[2] : "0";
        return minor + "." + patch + ".";
    }

    /** Installs Forge or NeoForge into the shared data root and returns the version id it produced. */
    public static String install(Profile profile, Progress progress) throws IOException {
        boolean neo = Profile.NEOFORGE.equals(profile.loader);
        String loaderVersion = neo ? neoForgeVersion(profile.mcVersion) : forgeVersion(profile.mcVersion);
        String fileName = neo
                ? "neoforge-" + loaderVersion + "-installer.jar"
                : "forge-" + profile.mcVersion + "-" + loaderVersion + "-installer.jar";
        String url = neo
                ? NEOFORGE_MAVEN + loaderVersion + "/neoforge-" + loaderVersion + "-installer.jar"
                : FORGE_MAVEN + profile.mcVersion + "-" + loaderVersion + "/" + fileName;

        progress.log((neo ? "NeoForge " : "Forge ") + loaderVersion + " kuruluyor");
        ensureLauncherProfile();
        Path installer = Dirs.ensure(Dirs.root().resolve("installers")).resolve(fileName);
        Download.file(URI.create(url), installer, null, 0);

        String java = dev.freemc.Jvm.javaCommand();
        List<String> command = List.of(java, "-jar", installer.toAbsolutePath().toString(),
                "--installClient", Dirs.root().toAbsolutePath().toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Successfully") || line.contains("error") || line.contains("Exception")) {
                    progress.log(line.trim());
                }
            }
        }
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("yükleyici kesildi", e);
        }
        if (code != 0) {
            throw new IOException((neo ? "NeoForge" : "Forge") + " yükleyicisi başarısız (çıkış kodu " + code + ")");
        }

        String versionId = neo ? "neoforge-" + loaderVersion
                : profile.mcVersion + "-forge-" + loaderVersion;
        Path versionFile = Dirs.versionDir(versionId).resolve(versionId + ".json");
        if (!Files.isRegularFile(versionFile)) {
            String found = findVersionId(profile.mcVersion, neo);
            if (found == null) {
                throw new IOException("Yükleyici sürüm dosyası üretmedi: " + versionId);
            }
            versionId = found;
        }
        profile.loaderVersion = loaderVersion;
        profile.save();
        progress.log("Yükleyici hazır: " + versionId);
        return versionId;
    }

    /** The official installers refuse to run without a launcher profile file. */
    private static void ensureLauncherProfile() throws IOException {
        Path profile = Dirs.root().resolve("launcher_profiles.json");
        if (Files.isRegularFile(profile)) {
            return;
        }
        Files.writeString(profile, """
                {
                  "profiles": {},
                  "settings": {},
                  "version": 3
                }
                """, StandardCharsets.UTF_8);
    }

    /** Loader installers name their version folder slightly differently across releases. */
    private static String findVersionId(String mcVersion, boolean neo) throws IOException {
        try (var stream = Files.list(Dirs.versions())) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> neo ? name.startsWith("neoforge-")
                            : name.startsWith(mcVersion + "-forge-") || name.equals(mcVersion + "-forge"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
