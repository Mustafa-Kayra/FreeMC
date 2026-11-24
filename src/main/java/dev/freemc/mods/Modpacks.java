package dev.freemc.mods;

import dev.freemc.Dirs;
import dev.freemc.game.Progress;
import dev.freemc.game.Profile;
import dev.freemc.json.Json;
import dev.freemc.net.Archive;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Modrinth modpacks: search and .mrpack installation with the pack's overrides. */
public final class Modpacks {

    private static final String API = "https://api.modrinth.com/v2";

    private Modpacks() {
    }

    public record Hit(String slug, String title, String description, String author, long downloads,
                      String iconUrl) {
    }

    public static List<Hit> search(String query, int limit) throws IOException {
        String url = API + "/search?limit=" + limit + "&index=relevance&query="
                + URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8)
                + "&facets=" + URLEncoder.encode("[[\"project_type:modpack\"]]", StandardCharsets.UTF_8);
        Map<String, Object> root = Json.object(Download.text(URI.create(url)));
        List<Hit> hits = new ArrayList<>();
        for (Object element : Json.list(root, "hits")) {
            String slug = Json.str(element, "slug");
            if (slug == null) {
                continue;
            }
            hits.add(new Hit(slug, Json.str(element, "title", slug), Json.str(element, "description", ""),
                    Json.str(element, "author", ""), Json.num(element, "downloads", 0),
                    Json.str(element, "icon_url")));
        }
        return hits;
    }

    /** Downloads a pack's newest .mrpack and materialises it as a new profile. */
    public static Profile install(String slug, Progress progress) throws IOException {
        Map<String, Object> project = Json.object(Download.text(URI.create(API + "/project/" + slug)));
        String title = Json.str(project, "title", slug);
        progress.log("Mod paketi: " + title);

        List<Object> versions = Json.array(Download.text(URI.create(API + "/project/" + slug + "/version")));
        String packUrl = null;
        for (Object versionNode : versions) {
            for (Object fileNode : Json.list(versionNode, "files")) {
                String filename = Json.str(fileNode, "filename", "");
                String url = Json.str(fileNode, "url");
                if (url != null && filename.endsWith(".mrpack")) {
                    packUrl = url;
                    break;
                }
            }
            if (packUrl != null) {
                break;
            }
        }
        if (packUrl == null) {
            throw new IOException("Bu paket için .mrpack dosyası bulunamadı: " + slug);
        }

        Path work = Dirs.ensure(Dirs.root().resolve("tmp").resolve("pack-" + System.currentTimeMillis()));
        Path archive = work.resolve("pack.mrpack");
        Download.file(URI.create(packUrl), archive, null, 0);
        Path unpacked = Dirs.ensure(work.resolve("unpacked"));
        Archive.extract(archive, unpacked);

        Path indexFile = unpacked.resolve("modrinth.index.json");
        if (!Files.isRegularFile(indexFile)) {
            throw new IOException("Paket indeksi yok: " + slug);
        }
        Map<String, Object> index = Json.object(Files.readString(indexFile, StandardCharsets.UTF_8));
        Map<String, Object> dependencies = Json.map(index, "dependencies");
        String mcVersion = Json.str(dependencies, "minecraft");
        if (mcVersion == null) {
            throw new IOException("Paket Minecraft sürümü belirtmiyor: " + slug);
        }
        String loader = Profile.VANILLA;
        String loaderVersion = "";
        if (Json.str(dependencies, "fabric-loader") != null) {
            loader = Profile.FABRIC;
            loaderVersion = Json.str(dependencies, "fabric-loader");
        } else if (Json.str(dependencies, "forge") != null) {
            loader = Profile.FORGE;
            loaderVersion = Json.str(dependencies, "forge");
        } else if (Json.str(dependencies, "neoforge") != null) {
            loader = Profile.NEOFORGE;
            loaderVersion = Json.str(dependencies, "neoforge");
        }

        Profile profile = Profile.create(title, mcVersion, loader, loaderVersion);
        profile.source = "modrinth:" + slug;
        profile.ramMb = 4096;
        profile.save();
        progress.log("Profil oluşturuldu: " + profile.name + " (" + profile.describe() + ")");

        int downloaded = 0;
        int skipped = 0;
        List<Object> files = Json.list(index, "files");
        for (Object fileNode : files) {
            String env = Json.str(Json.map(fileNode, "env"), "client", "required");
            String path = Json.str(fileNode, "path");
            if (path == null || "unsupported".equals(env)) {
                skipped++;
                continue;
            }
            List<Object> urls = Json.list(fileNode, "downloads");
            if (urls.isEmpty() || !(urls.get(0) instanceof String url)) {
                skipped++;
                continue;
            }
            String sha1 = Json.str(Json.map(fileNode, "hashes"), "sha1");
            Path target = profile.gameDir().resolve(path).normalize();
            if (!target.startsWith(profile.gameDir())) {
                continue;
            }
            Download.file(URI.create(url), target, sha1, Json.num(fileNode, "fileSize", 0));
            downloaded++;
            progress.files(downloaded, files.size());
        }

        int overrides = copyOverrides(unpacked.resolve("overrides"), profile.gameDir())
                + copyOverrides(unpacked.resolve("client-overrides"), profile.gameDir());
        deleteTree(work);
        progress.log("Paket kuruldu: " + downloaded + " dosya, " + overrides + " override, "
                + skipped + " atlandı");
        return profile;
    }

    private static int copyOverrides(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return 0;
        }
        int copied = 0;
        try (var walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
        }
        return copied;
    }

    public static List<Path> installedPacks() {
        List<Path> packs = new ArrayList<>();
        for (Profile profile : Profile.all()) {
            if (profile.source.startsWith("modrinth:")) {
                packs.add(profile.dir());
            }
        }
        return packs;
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
