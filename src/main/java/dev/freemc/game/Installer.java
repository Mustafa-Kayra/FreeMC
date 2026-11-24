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
 * platform libraries, natives and the asset index.
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

    public static GameInstall install(String versionId, Progress progress) throws IOException {
        AtomicInteger done = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        VersionManifest manifest = manifest(false, progress);
        VersionManifest.Entry entry = manifest.byId(versionId);
        if (entry == null) {
            throw new IOException("Sürüm bulunamadı: " + versionId);
        }

        Path versionJson = Dirs.versionDir(versionId).resolve(versionId + ".json");
        progress.log("Sürüm dosyası: " + versionId);
        Download.file(URI.create(entry.url()), versionJson, entry.sha1(), 0);

        Version version = Version.parse(Files.readString(versionJson, StandardCharsets.UTF_8), features());
        if (version.mainClass == null) {
            throw new IOException("Sürüm dosyası geçersiz (mainClass yok): " + versionId);
        }

        Path librariesRoot = Dirs.libraries();
        Path assetsRoot = Dirs.assets();
        Path clientJar = Dirs.versionDir(versionId).resolve(versionId + ".jar");

        List<RemoteFile> batch = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (version.client != null && seen.add(version.client.path == null ? "client" : version.client.path)) {
            batch.add(new RemoteFile(null, version.client.url, version.client.sha1, version.client.size));
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
        fetch(batch, librariesRoot, assetsRoot, clientJar, progress, done, total);

        Path nativesDir = Dirs.nativesDir(versionId);
        extractNatives(version, librariesRoot, nativesDir, progress);

        Path assetsDir = installAssets(version, Dirs.game(), assetsRoot, progress, done, total);

        List<Path> classpath = new ArrayList<>();
        for (Library library : version.libraries) {
            if (library.artifact != null && library.artifact.path != null) {
                classpath.add(librariesRoot.resolve(library.artifact.path));
            }
        }
        classpath.add(clientJar);

        progress.log("Kurulum tamam: " + versionId + " (" + version.libraries.size() + " kütüphane)");
        return new GameInstall(version, Dirs.game(), assetsDir, nativesDir, classpath);
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

    /**
     * Fetches a batch. Files with a null path go to the client jar; asset-index and log-config
     * entries are routed to their own roots.
     */
    private static void fetch(List<RemoteFile> files, Path librariesRoot, Path assetsRoot, Path clientJar,
                              Progress progress, AtomicInteger done, AtomicInteger total) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        total.addAndGet(files.size());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(8, files.size()));
        AtomicReference<IOException> failure = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();
        for (RemoteFile file : files) {
            futures.add(pool.submit(() -> {
                if (failure.get() != null) {
                    return;
                }
                try {
                    Download.file(URI.create(file.url), target(file, librariesRoot, assetsRoot, clientJar),
                            file.sha1, file.size);
                } catch (IOException e) {
                    String message = String.valueOf(e.getMessage());
                    if (message.contains("HTTP 404")) {
                        progress.log("atlandı (404): " + file.url);
                    } else {
                        failure.compareAndSet(null, e);
                        return;
                    }
                }
                progress.files(done.incrementAndGet(), total.get());
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
        progress.log("Natives açıldı (" + count + " dosya)");
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

    private static Path installAssets(Version version, Path gameDir, Path assetsRoot, Progress progress,
                                      AtomicInteger done, AtomicInteger total) throws IOException {
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

        progress.log("Varlıklar indiriliyor (" + objects.size() + " nesne)");
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
        fetch(batch, assetsRoot, assetsRoot, assetsRoot.resolve("indexes/client.jar"), progress, done, total);

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
}
