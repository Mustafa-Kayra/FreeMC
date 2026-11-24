package dev.freemc.game;

import dev.freemc.Os;
import dev.freemc.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A single library entry, resolved for the current operating system. */
final class Library {

    static final String MOJANG_LIBRARIES = "https://libraries.minecraft.net/";

    final String name;
    final RemoteFile artifact;
    final RemoteFile natives;
    final List<String> extractExclude;

    private Library(String name, RemoteFile artifact, RemoteFile natives, List<String> extractExclude) {
        this.name = name;
        this.artifact = artifact;
        this.natives = natives;
        this.extractExclude = extractExclude;
    }

    static List<Library> parseAll(List<Object> raw, Map<String, Boolean> features) {
        List<Library> libraries = new ArrayList<>();
        for (Object element : raw) {
            if (!(element instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) element;
            if (!Rule.allows(Rule.parse(Json.list(entry, "rules")), features)) {
                continue;
            }
            String name = Json.str(entry, "name");
            if (name == null) {
                continue;
            }

            Map<String, Object> downloads = Json.map(entry, "downloads");
            Map<String, Object> nativesMap = Json.map(entry, "natives");
            String nativesClassifier = nativesMap == null ? null : Json.str(nativesMap, Os.NAME);
            String repository = Json.str(entry, "url", MOJANG_LIBRARIES);

            RemoteFile artifact = remote(Json.map(downloads, "artifact"));
            if (artifact == null && downloads == null && nativesClassifier == null) {
                artifact = maven(name, null, repository, Json.str(entry, "sha1"), Json.num(entry, "size", 0));
            }

            RemoteFile natives = null;
            if (nativesClassifier != null) {
                natives = remote(Json.map(Json.map(downloads, "classifiers"), nativesClassifier));
                if (natives == null) {
                    natives = maven(name, nativesClassifier, repository, null, 0);
                }
            }

            List<String> exclude = new ArrayList<>();
            Map<String, Object> extract = Json.map(entry, "extract");
            if (extract != null) {
                for (Object pattern : Json.list(extract, "exclude")) {
                    if (pattern instanceof String text) {
                        exclude.add(text);
                    }
                }
            }

            if (artifact != null || natives != null) {
                libraries.add(new Library(name, artifact, natives, exclude));
            }
        }
        return libraries;
    }

    private static RemoteFile maven(String coordinates, String classifier, String repository, String sha1, long size) {
        String path = mavenPath(coordinates, classifier);
        if (path == null) {
            return null;
        }
        String base = repository.endsWith("/") ? repository : repository + "/";
        return new RemoteFile(path, base + path, sha1, size);
    }

    private static RemoteFile remote(Map<String, Object> node) {
        if (node == null) {
            return null;
        }
        String url = Json.str(node, "url");
        if (url == null) {
            return null;
        }
        return new RemoteFile(Json.str(node, "path"), url, Json.str(node, "sha1"), Json.num(node, "size", 0));
    }

    /** "group:artifact:version[:classifier][@ext]" to a path inside the libraries root. */
    static String mavenPath(String coordinates, String classifier) {
        String ext = "jar";
        String name = coordinates;
        int at = name.indexOf('@');
        if (at >= 0) {
            ext = name.substring(at + 1);
            name = name.substring(0, at);
        }
        String[] parts = name.split(":");
        if (parts.length < 3) {
            return null;
        }
        String group = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String usedClassifier = parts.length > 3 ? parts[3] : classifier;
        StringBuilder path = new StringBuilder()
                .append(group).append('/')
                .append(artifactId).append('/')
                .append(version).append('/')
                .append(artifactId).append('-').append(version);
        if (usedClassifier != null) {
            path.append('-').append(usedClassifier);
        }
        return path.append('.').append(ext).toString();
    }
}
