package dev.freemc;

import dev.freemc.auth.OfflineAuth;
import dev.freemc.game.CommandBuilder;
import dev.freemc.game.GameInstall;
import dev.freemc.game.GameProcess;
import dev.freemc.game.Installer;
import dev.freemc.game.Progress;
import dev.freemc.game.VersionManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Headless mode: install and optionally start a version without the window. */
final class Cli {

    private Cli() {
    }

    static void run(String[] args) {
        String versionId = "1.20.1";
        String username = "Player";
        int ramMb = 2048;
        boolean launch = false;
        boolean list = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--version", "-v" -> versionId = i + 1 < args.length ? args[++i] : versionId;
                case "--user", "-u" -> username = i + 1 < args.length ? args[++i] : username;
                case "--ram" -> ramMb = i + 1 < args.length ? parseInt(args[++i], ramMb) : ramMb;
                case "--launch" -> launch = true;
                case "--list-versions" -> list = true;
                default -> {
                    if (args[i].startsWith("--")) {
                        // unknown flag, kept for forward compatibility
                    }
                }
            }
        }

        Progress progress = new Progress() {
            @Override
            public void log(String message) {
                System.out.println("[freemc] " + message);
            }

            @Override
            public void files(int done, int total) {
                if (total > 0 && (done == total || done % 250 == 0)) {
                    System.out.println("[freemc] " + done + "/" + total + " dosya");
                }
            }
        };

        try {
            if (list) {
                VersionManifest manifest = Installer.manifest(true, progress);
                System.out.println("En son sürüm: " + manifest.latestRelease + " (snapshot " + manifest.latestSnapshot + ")");
                for (VersionManifest.Entry entry : manifest.releases()) {
                    System.out.println(entry.id() + "\t" + entry.releaseTime());
                }
                return;
            }

            GameInstall install = Installer.install(versionId, progress);
            OfflineAuth auth = OfflineAuth.of(username);
            String java = Jvm.forVersion(install.version.javaMajor, message -> System.out.println("[freemc] " + message));
            List<String> command = CommandBuilder.build(install, java, auth.name(), auth.uuid(), auth.accessToken(), ramMb);

            Path commandFile = Dirs.root().resolve("last-command.txt");
            Files.writeString(commandFile, String.join(System.lineSeparator(), command), StandardCharsets.UTF_8);
            System.out.println("[freemc] oyuncu: " + auth.name() + " uuid=" + auth.uuid());
            System.out.println("[freemc] komut dosyası: " + commandFile);

            if (launch) {
                Process process = GameProcess.launch(command, install.gameDir, System.out::println);
                System.out.println("[freemc] oyun başlatıldı, pid=" + process.pid());
                int code = process.waitFor();
                System.out.println("[freemc] çıkış kodu: " + code);
                System.exit(code == 0 ? 0 : 1);
            }
        } catch (IOException e) {
            System.err.println("[freemc] hata: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(130);
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
