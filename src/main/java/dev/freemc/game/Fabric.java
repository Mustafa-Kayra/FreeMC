package dev.freemc.game;

import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Fabric Loader versions and the launch profile they publish. */
public final class Fabric {

    private static final String META = "https://meta.fabricmc.net/v2/versions/loader/";

    private Fabric() {
    }

    /** Newest stable loader for the Minecraft version, falling back to the newest build. */
    public static String latestLoader(String mcVersion) throws IOException {
        List<Object> parsed = Json.array(Download.text(URI.create(META + mcVersion)));
        String fallback = null;
        for (Object element : parsed) {
            Map<String, Object> entry = Json.map(element, "loader");
            String version = Json.str(entry, "version");
            if (version == null) {
                continue;
            }
            if (Json.bool(entry, "stable", false)) {
                return version;
            }
            if (fallback == null) {
                fallback = version;
            }
        }
        if (fallback == null) {
            throw new IOException("Bu sürüm için Fabric loader bulunamadı: " + mcVersion);
        }
        return fallback;
    }

    public static String profileUrl(String mcVersion, String loaderVersion) {
        return META + mcVersion + "/" + loaderVersion + "/profile/json";
    }
}
