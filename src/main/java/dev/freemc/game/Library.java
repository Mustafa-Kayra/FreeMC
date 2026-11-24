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

            RemoteFile artifact = remote(Json.map(downloads, "artifact"));
            if (artifact == null && downloads == null && nativesClassifier == null) {
                String path = mavenPath(name, null, "jar");
                if (path != null) {
                    artifact = new RemoteFile(path, MOJANG_LIBRARIES + path, null, 0);
                }
            }

            RemoteFile natives = null;
            Map<String, Object> classifiers = Json.map(downloads, "classifiers");
            if (nativesClassifier != null) {
                natives = remote(Json.map(classifiers, nativesClassifier));
                if (natives == null) {
                    String path = mavenPath(name, nativesClassifier, "jar");
                    if (path != null) {
                        natives = new RemoteFile(path, MOJANG_LIBRARIES + path, null, 0);
                    }
                }
            }

            List<String> exclude = new ArrayList<>();
            Map<String, Object> extract = Json.map(entry, "extract");
            if (extract != null) {
                for (Object pattern : Json.list(extract, "exclude")) {
                    if (pattern instanceof String) {
                        exclude.add((String) pattern);
                    }
                }
            }

            if (artifact != null || natives != null) {
                libraries.add(new Library(name, artifact, natives, exclude));
            }
        }
        return libraries;
    }

    private static RemoteFile remote(Map<String, Object> node) {
        if (node == null) {
            return null;
        }
        String path = Json.str(node, "path");
        String url = Json.str(node, "url");
        if (url == null) {
            return null;
        }
        return new RemoteFile(path, url, Json.str(node, "sha1"), Json.num(node, "size", 0));
    }

    /** "group:artifact:version[:classifier][@ext]" to a path inside the libraries root. */
    static String mavenPath(String coordinates, String classifier, String defaultExt) {
        String ext = defaultExt;
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
