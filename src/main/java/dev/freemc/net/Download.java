package dev.freemc.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** HTTP(S) file transfer with retries, resume-free atomic writes and SHA-1 verification. */
public final class Download {

    private static final int ATTEMPTS = 4;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Download() {
    }

    public static void file(URI uri, Path target, String sha1, long size) throws IOException {
        if (Files.isRegularFile(target) && looksComplete(target, sha1, size)) {
            return;
        }
        Files.createDirectories(target.getParent());
        Path part = target.resolveSibling(target.getFileName() + ".part");
        IOException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                transfer(uri, part);
                if (sha1 != null && !sha1Matches(part, sha1)) {
                    throw new IOException("bozuk indirme (sha1 uyuşmuyor): " + uri);
                }
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
                Files.deleteIfExists(part);
                if (attempt < ATTEMPTS) {
                    sleep(500L * attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("indirme kesildi: " + uri, e);
            }
        }
        throw new IOException("indirilemedi: " + uri + " (" + last.getMessage() + ")", last);
    }

    public static String text(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "FreeMC/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("istek kesildi: " + uri, e);
        }
    }


    public static String postJson(URI uri, String body) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "FreeMC/1.0 (launcher)")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("istek kesildi: " + uri, e);
        }
    }

    private static void transfer(URI uri, Path part) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "FreeMC/1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            try (InputStream ignored = response.body()) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
        }
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(part)) {
            in.transferTo(out);
        }
    }

    private static boolean looksComplete(Path target, String sha1, long size) throws IOException {
        long actual = Files.size(target);
        if (size > 0 && actual != size) {
            return false;
        }
        if (sha1 != null && actual == size) {
            return true;
        }
        if (sha1 != null) {
            return sha1Matches(target, sha1);
        }
        return actual > 0 || size == 0;
    }

    private static boolean sha1Matches(Path file, String expected) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            byte[] buffer = new byte[64 * 1024];
            while (in.read(buffer) != -1) {
                // hashing happens in the digest stream
            }
        }
        return HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(expected);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
