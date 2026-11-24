package dev.freemc.mods;

import dev.freemc.game.Progress;
import dev.freemc.game.Profile;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks installed jars against Modrinth's "version files update" endpoint. */
public final class ModUpdates {

    private static final String API = "https://api.modrinth.com/v2";

    private ModUpdates() {
    }

    public record Update(String localFile, String versionNumber, String downloadUrl, String fileName, String sha1) {
    }

    public static List<Update> check(Profile profile, Progress progress) throws IOException {
        List<Path> jars = Modrinth.installed(profile.modsDir());
        if (jars.isEmpty() || !profile.modded()) {
            return List.of();
        }
        Map<String, Path> byHash = new LinkedHashMap<>();
        for (Path jar : jars) {
            byHash.put(sha1(jar), jar);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hashes", new ArrayList<>(byHash.keySet()));
        body.put("algorithm", "sha1");
        body.put("game_versions", List.of(profile.mcVersion));
        body.put("loaders", List.of(profile.loader));

        String response = Download.postJson(URI.create(API + "/version_files/update"), Json.write(body));
        Map<String, Object> updated = Json.object(response);

        List<Update> updates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updated.entrySet()) {
            Path local = byHash.get(entry.getKey());
            if (local == null) {
                continue;
            }
            Object versionNode = entry.getValue();
            boolean sameFile = false;
            String url = null;
            String fileName = local.getFileName().toString();
            String sha1 = null;
            for (Object fileNode : Json.list(versionNode, "files")) {
                String fileHash = Json.str(Json.map(fileNode, "hashes"), "sha1");
                if (fileHash != null && fileHash.equalsIgnoreCase(entry.getKey())) {
                    sameFile = true;
                    break;
                }
                String fileUrl = Json.str(fileNode, "url");
                if (url == null && fileUrl != null) {
                    url = fileUrl;
                    fileName = Json.str(fileNode, "filename", fileName);
                    sha1 = fileHash;
                }
            }
            if (sameFile || url == null) {
                continue;
            }
            updates.add(new Update(local.getFileName().toString(), Json.str(versionNode, "version_number", "?"),
                    url, fileName, sha1));
        }
        progress.log(updates.size() + " mod güncellemesi bulundu");
        return updates;
    }

    public static void apply(List<Update> updates, Profile profile, Progress progress) throws IOException {
        for (Update update : updates) {
            Path target = profile.modsDir().resolve(update.fileName());
            Download.file(URI.create(update.downloadUrl()), target, update.sha1(), 0);
            if (!update.fileName().equals(update.localFile())) {
                Files.deleteIfExists(profile.modsDir().resolve(update.localFile()));
            }
            progress.log("Güncellendi: " + update.localFile() + " → " + update.fileName());
        }
    }

    private static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
                byte[] buffer = new byte[64 * 1024];
                while (in.read(buffer) != -1) {
                    // digest is updated by the stream
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
