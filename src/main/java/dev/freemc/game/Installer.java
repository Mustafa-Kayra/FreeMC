package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads and prepares a Minecraft version from Mojang's public CDN: version file, client jar,
 * platform libraries, natives and the asset index. Version files that declare {@code inheritsFrom}
 * (Fabric, Quilt) are layered over their parent before installation.
 */
public final class Installer {

    public static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    private Installer() {
    }

    public static VersionManifest manifest(boolean force, Progress progress) throws IOException {
        Path cache = Dirs.versions().resolve("version_manifest_v2.json");
        boolean stale = !Files.isRegularFile(cache)
                || Files.getLastModifiedTime(cache).toInstant().isBefore(Instant.now().minus(Duration.ofMinutes(30)));
        if (force || stale) {
            try {
                Download.file(URI.create(MANIFEST_URL), cache, null, 0);
            } catch (IOException e) {
                if (!Files.isRegularFile(cache)) {
                    throw new IOException("Sürüm listesi alınamadı, internet bağlantını kontrol et.", e);
                }
                progress.log("Uyarı: sürüm listesi güncellenemedi, önbellek kullanılıyor (" + e.getMessage() + ")");
            }
        }
        return VersionManifest.parse(Files.readString(cache, StandardCharsets.UTF_8));
    }

    /** Installs a vanilla version from the Mojang manifest. */
    public static GameInstall install(String versionId, Path gameDir, Progress progress) throws IOException {
        Pending pending = new Pending(progress);
        Version version = resolved(versionId, pending, 0);
        return finish(version, gameDir, pending);
    }

    /** Installs a custom profile (Fabric/Quilt) fetched from a meta endpoint. */
    public static GameInstall installProfile(String profileUrl, Path gameDir, Progress progress) throws IOException {
        Pending pending = new Pending(progress);
        String body = Download.text(URI.create(profileUrl));
        Map<String, Object> root = Json.object(body);
        String id = Json.str(root, "id");
        if (id == null) {
            throw new IOException("Yükleyici profili geçersiz: " + profileUrl);
        }
        Path json = Dirs.versionDir(id).resolve(id + ".json");
        Files.writeString(json, body, StandardCharsets.UTF_8);
        pending.progress.log("Yükleyici profili: " + id);
        Version version = resolved(id, pending, 0);
        return finish(version, gameDir, pending);
    }

    private static Version resolved(String versionId, Pending pending, int depth) throws IOException {
        if (depth > 4) {
            throw new IOException("Sürüm zinciri çok derin: " + versionId);
        }
        Path json = Dirs.versionDir(versionId).resolve(versionId + ".json");
        if (!Files.isRegularFile(json)) {
            VersionManifest manifest = manifest(false, pending.progress);
            VersionManifest.Entry entry = manifest.byId(versionId);
            if (entry == null) {
                throw new IOException("Sürüm bulunamadı: " + versionId);
            }
            pending.progress.log("Sürüm dosyası: " + versionId);
            Download.file(URI.create(entry.url()), json, entry.sha1(), 0);
        }
        Version version = Version.parse(Files.readString(json, StandardCharsets.UTF_8), features());
        if (version.inheritsFrom == null) {
            return version;
        }
        Version parent = resolved(version.inheritsFrom, pending, depth + 1);
        return Version.inherit(version, parent);
    }

    private static GameInstall finish(Version version, Path gameDir, Pending pending) throws IOException {
        Progress progress = pending.progress;
        Path librariesRoot = Dirs.libraries();
        Path assetsRoot = Dirs.assets();
        Path clientJar = version.clientJarPath();

        List<RemoteFile> batch = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (version.client != null) {
            batch.add(new RemoteFile(clientJar.getFileName().toString(), version.client.url,
                    version.client.sha1, version.client.size));
        }
        for (Library library : version.libraries) {
            if (library.artifact != null && library.artifact.path != null && seen.add(library.artifact.path)) {
                batch.add(library.artifact);
            }
            if (library.natives != null && library.natives.path != null && seen.add(library.natives.path)) {
                batch.add(library.natives);
            }
        }
        if (version.assetIndex != null) {
            batch.add(version.assetIndex);
        }
        if (version.logConfig != null) {
            batch.add(version.logConfig);
        }

        progress.log("Kütüphaneler ve oyun dosyası indiriliyor");
        fetch(batch, librariesRoot, assetsRoot, clientJar, pending);

        Path nativesDir = Dirs.nativesDir(version.id);
        extractNatives(version, librariesRoot, nativesDir, progress);

        Path assetsDir = installAssets(version, gameDir, assetsRoot, pending);

        List<Path> classpath = new ArrayList<>();
        for (Library library : version.libraries) {
            if (library.artifact != null && library.artifact.path != null) {
                classpath.add(librariesRoot.resolve(library.artifact.path));
            }
        }
        classpath.add(clientJar);

        progress.log("Kurulum tamam: " + version.id + " (" + version.libraries.size() + " kütüphane)");
        return new GameInstall(version, gameDir, assetsDir, nativesDir, classpath);
    }

    private static Map<String, Boolean> features() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("is_demo_user", false);
        features.put("has_custom_resolution", false);
        features.put("has_quick_plays_support", false);
        features.put("is_quick_play_singleplayer", false);
        features.put("is_quick_play_multiplayer", false);
        features.put("is_quick_play_realms", false);
        return features;
    }

    /** Files with the client jar's name go there; asset-index and log-config entries have their own roots. */
    private static void fetch(List<RemoteFile> files, Path librariesRoot, Path assetsRoot, Path clientJar,
                              Pending pending) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        Progress progress = pending.progress;
        pending.total.addAndGet(files.size());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(8, files.size()));
        AtomicReference<IOException> failure = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();
        for (RemoteFile file : files) {
            futures.add(pool.submit(() -> {
                if (failure.get() != null) {
                    return;
                }
                try {
                    Path target = target(file, librariesRoot, assetsRoot, clientJar);
                    Download.file(URI.create(file.url), target, file.sha1, file.size);
                } catch (IOException e) {
                    String message = String.valueOf(e.getMessage());
                    if (message.contains("HTTP 404")) {
                        progress.log("atlandı (404): " + file.url);
                    } else {
                        failure.compareAndSet(null, e);
                        return;
                    }
                }
                progress.files(pending.done.incrementAndGet(), pending.total.get());
            }));
        }
        pool.shutdown();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("indirme kesildi", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof IOException io ? io : new IOException(cause);
            }
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private static Path target(RemoteFile file, Path librariesRoot, Path assetsRoot, Path clientJar) {
        if (file.path == null) {
            return clientJar;
        }
        if (file.path.startsWith("indexes/") || file.path.startsWith("log_configs/")) {
            return assetsRoot.resolve(file.path);
        }
        String clientName = clientJar.getFileName().toString();
        if (file.path.equals(clientName)) {
            return clientJar;
        }
        return librariesRoot.resolve(file.path);
    }

    private static void extractNatives(Version version, Path librariesRoot, Path nativesDir, Progress progress)
            throws IOException {
        if (Files.isDirectory(nativesDir)) {
            deleteTree(nativesDir);
        }
        Files.createDirectories(nativesDir);
        int count = 0;
        for (Library library : version.libraries) {
            if (library.natives == null || library.natives.path == null) {
                continue;
            }
            Path jar = librariesRoot.resolve(library.natives.path);
            if (!Files.isRegularFile(jar)) {
                continue;
            }
            count += extract(jar, nativesDir, library.extractExclude);
        }
        if (count > 0) {
            progress.log("Natives açıldı (" + count + " dosya)");
        }
    }

    private static int extract(Path jar, Path targetDir, List<String> excludes) throws IOException {
        int written = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                boolean excluded = false;
                for (String pattern : excludes) {
                    if (name.startsWith(pattern)) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) {
                    continue;
                }
                Path target = targetDir.resolve(name).normalize();
                if (!target.startsWith(targetDir)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                written++;
            }
        }
        return written;
    }

    private static Path installAssets(Version version, Path gameDir, Path assetsRoot, Pending pending)
            throws IOException {
        Progress progress = pending.progress;
        if (version.assetIndex == null) {
            progress.log("Bu sürümde asset indeksi yok, atlanıyor");
            return assetsRoot;
        }
        Path indexFile = assetsRoot.resolve(version.assetIndex.path);
        Map<String, Object> index = Json.object(Files.readString(indexFile, StandardCharsets.UTF_8));
        Map<String, Object> objects = Json.map(index, "objects");
        if (objects == null) {
            return assetsRoot;
        }

        progress.log("Varlıklar hazırlanıyor (" + objects.size() + " nesne)");
        List<RemoteFile> batch = new ArrayList<>();
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            String hash = Json.str(entry.getValue(), "hash");
            if (hash == null || hash.length() < 2) {
                continue;
            }
            long size = Json.num(entry.getValue(), "size", 0);
            String prefix = hash.substring(0, 2);
            batch.add(new RemoteFile("objects/" + prefix + "/" + hash,
                    RESOURCES_URL + prefix + "/" + hash, hash, size));
        }
        fetch(batch, assetsRoot, assetsRoot, assetsRoot.resolve("indexes/client.jar"), pending);

        boolean virtual = Json.bool(index, "virtual", false);
        boolean mapToResources = Json.bool(index, "map_to_resources", false);
        if (!virtual && !mapToResources) {
            return assetsRoot;
        }

        Path targetRoot = virtual ? assetsRoot.resolve("virtual").resolve(version.assetsIndexId)
                : gameDir.resolve("resources");
        Files.createDirectories(targetRoot);
        int copied = 0;
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            String hash = Json.str(entry.getValue(), "hash");
            if (hash == null || hash.length() < 2) {
                continue;
            }
            Path source = assetsRoot.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            Path target = targetRoot.resolve(entry.getKey());
            if (!Files.isRegularFile(source)) {
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        progress.log("Eski sürüm varlık düzeni hazırlandı (" + copied + " dosya)");
        return targetRoot;
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

    /** Per-install progress counters shared by the helper methods. */
    private static final class Pending {
        final Progress progress;
        final AtomicInteger done = new AtomicInteger();
        final AtomicInteger total = new AtomicInteger();

        Pending(Progress progress) {
            this.progress = progress;
        }
    }
}
