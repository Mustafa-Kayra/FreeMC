package dev.freemc.auth;

import dev.freemc.game.Profile;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Ely.by account support: the Yggdrasil-style login, plus the skin and cape the service publishes
 * for an account. Only the session token is stored, never the password.
 */
public final class ElyBy {

    public static final String API_ROOT = "https://authserver.ely.by/api/authlib-injector";
    private static final String AUTH = "https://authserver.ely.by/auth/authenticate";
    private static final String REFRESH = "https://authserver.ely.by/auth/refresh";
    private static final String VALIDATE = "https://authserver.ely.by/auth/validate";
    private static final String TEXTURES = "https://skinsystem.ely.by/textures/";

    private ElyBy() {
    }

    public record Account(String name, String uuid, String accessToken, String clientToken) {
    }

    public record Textures(String skinUrl, String capeUrl) {
        public boolean empty() {
            return skinUrl == null && capeUrl == null;
        }
    }

    // ------------------------------------------------------------ accounts

    public static Account login(String username, String password) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("clientToken", UUID.randomUUID().toString());
        body.put("agent", Map.of("name", "Minecraft", "version", 1));
        Map<String, Object> response = Json.object(Download.postJson(URI.create(AUTH), Json.write(body)));
        String accessToken = Json.str(response, "accessToken");
        String clientToken = Json.str(response, "clientToken", Json.str(body, "clientToken"));
        Map<String, Object> profile = Json.map(response, "selectedProfile");
        if (accessToken == null || profile == null) {
            throw new IOException("Ely.by girişi başarısız: yanıt eksik");
        }
        return new Account(Json.str(profile, "name", username), dashed(Json.str(profile, "id", "")),
                accessToken, clientToken);
    }

    public static Account refresh(Account account) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", account.accessToken());
        body.put("clientToken", account.clientToken());
        Map<String, Object> response = Json.object(Download.postJson(URI.create(REFRESH), Json.write(body)));
        String accessToken = Json.str(response, "accessToken");
        if (accessToken == null) {
            throw new IOException("Ely.by oturumu yenilenemedi");
        }
        return new Account(account.name(), account.uuid(), accessToken,
                Json.str(response, "clientToken", account.clientToken()));
    }

    public static boolean valid(Account account) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", account.accessToken());
        body.put("clientToken", account.clientToken());
        try {
            Download.postJson(URI.create(VALIDATE), Json.write(body));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String dashed(String id) {
        String value = id == null ? "" : id.replace("-", "").toLowerCase(Locale.ROOT);
        if (value.length() != 32) {
            return id == null ? "" : id;
        }
        return value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-"
                + value.substring(16, 20) + "-" + value.substring(20);
    }

    // ------------------------------------------------------------ storage

    public static Path storeFile(Profile profile) {
        return profile.dir().resolve("elyby.json");
    }

    public static Account stored(Profile profile) {
        if (profile == null) {
            return null;
        }
        Path file = storeFile(profile);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            Map<String, Object> data = Json.object(Files.readString(file, StandardCharsets.UTF_8));
            String token = Json.str(data, "accessToken");
            if (token == null) {
                return null;
            }
            return new Account(Json.str(data, "name", "Player"), Json.str(data, "uuid", ""), token,
                    Json.str(data, "clientToken", ""));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public static void store(Profile profile, Account account) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", account.name());
        data.put("uuid", account.uuid());
        data.put("accessToken", account.accessToken());
        data.put("clientToken", account.clientToken());
        Files.createDirectories(profile.dir());
        Files.writeString(storeFile(profile), Json.write(data), StandardCharsets.UTF_8);
    }

    public static void clear(Profile profile) throws IOException {
        Files.deleteIfExists(storeFile(profile));
    }

    // ------------------------------------------------------------ textures

    /** Looks up the public skin and cape of a nickname or UUID. */
    public static Textures textures(String name) throws IOException {
        String url = TEXTURES + URLEncoder.encode(name, StandardCharsets.UTF_8);
        String body = Download.textOrNull(URI.create(url));
        if (body == null || body.isBlank()) {
            return new Textures(null, null);
        }
        Map<String, Object> root = Json.object(body);
        String skin = Json.str(Json.map(root, "SKIN"), "url");
        String cape = Json.str(Json.map(root, "CAPE"), "url");
        return new Textures(skin, cape);
    }

    public static Path download(String url, Path target) throws IOException {
        Download.file(URI.create(url), target, null, 0);
        return target;
    }
}
