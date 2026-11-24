package dev.freemc.game;

import dev.freemc.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The launcher-facing version list published by Mojang. */
public final class VersionManifest {

    public record Entry(String id, String type, String url, String sha1, String releaseTime) {
    }

    public final String latestRelease;
    public final String latestSnapshot;
    public final List<Entry> versions;

    private VersionManifest(String latestRelease, String latestSnapshot, List<Entry> versions) {
        this.latestRelease = latestRelease;
        this.latestSnapshot = latestSnapshot;
        this.versions = versions;
    }

    static VersionManifest parse(String json) {
        Map<String, Object> root = Json.object(json);
        Map<String, Object> latest = Json.map(root, "latest");
        List<Entry> entries = new ArrayList<>();
        for (Object element : Json.list(root, "versions")) {
            String id = Json.str(element, "id");
            String url = Json.str(element, "url");
            if (id == null || url == null) {
                continue;
            }
            entries.add(new Entry(id, Json.str(element, "type", "release"), url,
                    Json.str(element, "sha1"), Json.str(element, "releaseTime", "")));
        }
        return new VersionManifest(Json.str(latest, "release"), Json.str(latest, "snapshot"), entries);
    }

    public Entry byId(String id) {
        for (Entry entry : versions) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    public List<Entry> releases() {
        List<Entry> releases = new ArrayList<>();
        for (Entry entry : versions) {
            if ("release".equals(entry.type())) {
                releases.add(entry);
            }
        }
        return releases;
    }
}
