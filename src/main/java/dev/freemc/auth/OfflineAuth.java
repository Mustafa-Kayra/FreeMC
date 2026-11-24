package dev.freemc.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Offline ("crack") identity: no Mojang or Microsoft account is contacted. The UUID is the same
 * name-derived v3 UUID vanilla servers use for offline players, so single player, LAN and
 * offline-mode servers all accept it.
 */
public record OfflineAuth(String name, String uuid, String accessToken) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static OfflineAuth of(String username) {
        String name = sanitize(username);
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        byte[] token = new byte[16];
        RANDOM.nextBytes(token);
        return new OfflineAuth(name, uuid.toString(), HexFormat.of().formatHex(token));
    }

    public static String sanitize(String username) {
        if (username == null) {
            return "Player";
        }
        String cleaned = username.trim().replaceAll("[^A-Za-z0-9_]", "");
        if (cleaned.length() > 16) {
            cleaned = cleaned.substring(0, 16);
        }
        return cleaned.isEmpty() ? "Player" : cleaned;
    }

    public String sessionId() {
        return "token:" + accessToken + ":" + uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
