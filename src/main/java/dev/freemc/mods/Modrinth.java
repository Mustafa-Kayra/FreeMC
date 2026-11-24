package dev.freemc.mods;

import dev.freemc.game.Progress;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Modrinth search and one-click installation into an instance's mods folder. */
public final class Modrinth {

    private static final String API = "https://api.modrinth.com/v2";

    private Modrinth() {
    }

    public record Hit(String projectId, String slug, String title, String description, String author,
                      long downloads, String iconUrl) {
    }

    public record FileRef(String url, String filename, String sha1, long size) {
    }

    public record Dependency(String projectId, String type) {
    }

    public record VersionRef(String id, String number, List<FileRef> files, List<Dependency> dependencies) {
    }

    public static List<Hit> search(String query, String mcVersion, String loader, int limit) throws IOException {
        StringBuilder url = new StringBuilder(API).append("/search?limit=").append(limit)
                .append("&index=relevance")
                .append("&query=").append(encode(query == null ? "" : query))
                .append("&facets=").append(encode(facets(mcVersion, loader)));
        Map<String, Object> root = Json.object(Download.text(URI.create(url.toString())));
        List<Hit> hits = new ArrayList<>();
        for (Object element : Json.list(root, "hits")) {
            String projectId = Json.str(element, "project_id");
            String title = Json.str(element, "title");
            if (projectId == null || title == null) {
                continue;
            }
            hits.add(new Hit(projectId, Json.str(element, "slug", projectId), title,
                    Json.str(element, "description", ""), Json.str(element, "author", ""),
                    Json.num(element, "downloads", 0), Json.str(element, "icon_url")));
        }
        return hits;
    }

    /**
     * Installs the newest matching file for a project and every required dependency.
     * Returns the file names written, in install order.
     */
    public static List<String> install(String projectIdOrSlug, Path modsDir, String mcVersion, String loader,
                                       Progress progress) throws IOException {
        Files.createDirectories(modsDir);
        List<String> written = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(projectIdOrSlug);

        while (!queue.isEmpty()) {
            String project = queue.poll();
            if (!visited.add(project) || visited.size() > 16) {
                continue;
            }
            VersionRef version = newestVersion(project, mcVersion, loader);
            if (version == null) {
                progress.log("Uygun sürüm yok, atlandı: " + project);
                continue;
            }
            FileRef file = version.files().isEmpty() ? null : version.files().get(0);
            if (file == null) {
                continue;
            }
            Path target = modsDir.resolve(file.filename());
            if (Files.isRegularFile(target)) {
                progress.log("Zaten kurulu: " + file.filename());
            } else {
                progress.log("İndiriliyor: " + file.filename());
                Download.file(URI.create(file.url()), target, file.sha1(), file.size());
                written.add(file.filename());
            }
            for (Dependency dependency : version.dependencies()) {
                if ("required".equals(dependency.type()) && dependency.projectId() != null) {
                    queue.add(dependency.projectId());
                }
            }
        }
        return written;
    }

    public static VersionRef newestVersion(String projectIdOrSlug, String mcVersion, String loader) throws IOException {
        StringBuilder url = new StringBuilder(API).append("/project/")
                .append(encode(projectIdOrSlug)).append("/version?game_versions=")
                .append(encode("[\"" + mcVersion + "\"]"));
        if (loader != null && !"vanilla".equals(loader)) {
            url.append("&loaders=").append(encode("[\"" + loader + "\"]"));
        }
        List<Object> versions = Json.array(Download.text(URI.create(url.toString())));
        for (Object element : versions) {
            String id = Json.str(element, "id");
            if (id == null) {
                continue;
            }
            List<FileRef> files = new ArrayList<>();
            for (Object fileNode : Json.list(element, "files")) {
                String fileUrl = Json.str(fileNode, "url");
                String filename = Json.str(fileNode, "filename");
                if (fileUrl == null || filename == null) {
                    continue;
                }
                String sha1 = Json.str(Json.map(fileNode, "hashes"), "sha1");
                files.add(new FileRef(fileUrl, filename, sha1, Json.num(fileNode, "size", 0)));
            }
            List<Dependency> dependencies = new ArrayList<>();
            for (Object depNode : Json.list(element, "dependencies")) {
                String type = Json.str(depNode, "dependency_type");
                String project = Json.str(depNode, "project_id");
                if (type != null) {
                    dependencies.add(new Dependency(project, type));
                }
            }
            return new VersionRef(id, Json.str(element, "version_number", id), files, dependencies);
        }
        return null;
    }

    public static List<Path> installed(Path modsDir) {
        List<Path> jars = new ArrayList<>();
        if (!Files.isDirectory(modsDir)) {
            return jars;
        }
        try (Stream<Path> stream = Files.list(modsDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().forEach(jars::add);
        } catch (IOException ignored) {
            // an unreadable folder simply lists nothing
        }
        return jars;
    }

    public static Path addLocal(Path jar, Path modsDir) throws IOException {
        Files.createDirectories(modsDir);
        Path target = modsDir.resolve(jar.getFileName().toString());
        Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public static void remove(Path jar) throws IOException {
        Files.deleteIfExists(jar);
    }

    private static String facets(String mcVersion, String loader) {
        StringBuilder facets = new StringBuilder("[[\"project_type:mod\"]");
        facets.append(",[\"versions:").append(mcVersion).append("\"]");
        if (loader != null && !"vanilla".equals(loader)) {
            facets.append(",[\"categories:").append(loader).append("\"]");
        }
        return facets.append("]").toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
