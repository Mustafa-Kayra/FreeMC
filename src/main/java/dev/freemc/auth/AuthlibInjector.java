package dev.freemc.auth;

import dev.freemc.Dirs;
import dev.freemc.game.Progress;
import dev.freemc.json.Json;
import dev.freemc.net.Download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** The authlib-injector agent that lets the game talk to a third-party session server. */
public final class AuthlibInjector {

    private static final String LATEST = "https://api.github.com/repos/yushijinhun/authlib-injector/releases/latest";
    private static final String FALLBACK = "https://github.com/yushijinhun/authlib-injector/releases/download/v1.2.8/authlib-injector-1.2.8.jar";

    private AuthlibInjector() {
    }

    public static Path ensure(Progress progress) throws IOException {
        Path dir = Dirs.ensure(Dirs.root().resolve("authlib-injector"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "authlib-injector-*.jar")) {
            for (Path jar : stream) {
                return jar;
            }
        }

        String url = FALLBACK;
        try {
            String body = Download.text(URI.create(LATEST));
            for (Object asset : Json.list(Json.object(body), "assets")) {
                String name = Json.str(asset, "name", "");
                String download = Json.str(asset, "browser_download_url");
                if (download != null && name.startsWith("authlib-injector") && name.endsWith(".jar")) {
                    url = download;
                    break;
                }
            }
        } catch (IOException ignored) {
            // the pinned release works when the GitHub API is rate limited
        }

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path target = dir.resolve(fileName);
        progress.log("authlib-injector indiriliyor: " + fileName);
        Download.file(URI.create(url), target, null, 0);
        return target;
    }
}
