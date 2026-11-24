package dev.freemc;

import dev.freemc.auth.OfflineAuth;
import dev.freemc.game.CommandBuilder;
import dev.freemc.game.GameInstall;
import dev.freemc.game.GameLauncher;
import dev.freemc.game.GameProcess;
import dev.freemc.game.Profile;
import dev.freemc.game.Installer;
import dev.freemc.game.Progress;
import dev.freemc.game.VersionManifest;
import dev.freemc.mods.Modrinth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Headless mode: install, add mods and optionally start a version without the window. */
final class Cli {

    private Cli() {
    }

    static void run(String[] args) {
        String versionId = "1.20.1";
        String username = "Player";
        String loader = Profile.VANILLA;
        String modSlug = null;
        String packSlug = null;
        int ramMb = 2048;
        boolean launch = false;
        boolean list = false;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--version", "-v" -> versionId = value(args, i, versionId);
                case "--user", "-u" -> username = value(args, i, username);
                case "--loader" -> loader = value(args, i, loader);
                case "--mod" -> modSlug = value(args, i, null);
                case "--pack" -> packSlug = value(args, i, null);
                case "--ram" -> ramMb = parseInt(value(args, i, Integer.toString(ramMb)), ramMb);
                case "--launch" -> launch = true;
                case "--list-versions" -> list = true;
                case "--help", "-h" -> help = true;
                default -> {
                    // unknown flags are ignored
                }
            }
            if (args[i].startsWith("--") && i + 1 < args.length && !args[i + 1].startsWith("--")) {
                i++;
            }
        }

        Progress progress = new Progress() {
            @Override
            public void log(String message) {
                System.out.println("[freemc] " + message);
            }

            @Override
            public void files(int done, int total) {
                if (total > 0 && (done == total || done % 500 == 0)) {
                    System.out.println("[freemc] " + done + "/" + total + " dosya");
                }
            }
        };

        if (help) {
            usage();
            return;
        }

        try {
            if (list) {
                VersionManifest manifest = Installer.manifest(true, progress);
                System.out.println("En son sürüm: " + manifest.latestRelease + " (snapshot " + manifest.latestSnapshot + ")");
                for (VersionManifest.Entry entry : manifest.releases()) {
                    System.out.println(entry.id() + "\t" + entry.releaseTime());
                }
                return;
            }

            Profile profile;
            if (packSlug != null) {
                profile = runPack(packSlug, progress);
                versionId = profile.mcVersion;
                loader = profile.loader;
            } else {
                profile = findOrCreate(versionId, loader);
            }
            GameInstall install = GameLauncher.ensure(profile, progress);

            if (modSlug != null) {
                List<String> installed = Modrinth.install(modSlug, profile.modsDir(), versionId,
                        profile.loader, progress);
                System.out.println("[freemc] modlar: " + (installed.isEmpty() ? "yeni dosya yok" : String.join(", ", installed)));
            }

            OfflineAuth auth = OfflineAuth.of(username);
            String java = Jvm.forVersion(install.version.javaMajor, message -> System.out.println("[freemc] " + message));
            profile.ramMb = ramMb;
            profile.save();
            List<String> command = CommandBuilder.build(install, java, auth.name(), auth.uuid(), auth.accessToken(),
                    ramMb, profile.jvmArgs, profile.quickServer);
            Path commandFile = Dirs.root().resolve("last-command.txt");
            Files.writeString(commandFile, String.join(System.lineSeparator(), command), StandardCharsets.UTF_8);
            System.out.println("[freemc] oyuncu: " + auth.name() + " uuid=" + auth.uuid());
            System.out.println("[freemc] komut dosyası: " + commandFile);

            if (launch) {
                Process process = GameLauncher.start(profile, username, progress);
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

    private static Profile runPack(String slug, Progress progress) throws IOException {
        for (Profile profile : Profile.all()) {
            if (profile.source.equals("modrinth:" + slug)) {
                progress.log("Paket zaten kurulu: " + profile.name);
                return profile;
            }
        }
        return dev.freemc.mods.Modpacks.install(slug, progress);
    }

    private static Profile findOrCreate(String versionId, String loader) {
        for (Profile profile : Profile.all()) {
            if (profile.mcVersion.equals(versionId) && profile.loader.equals(Profile.normalizeLoader(loader))) {
                return profile;
            }
        }
        return Profile.create(versionId + " " + loader, versionId, Profile.normalizeLoader(loader), "");
    }

    private static void usage() {
        System.out.println("""
                FreeMC - offline Minecraft launcher

                Kullanim:
                  java -jar freemc.jar                      arayuzu ac
                  java -jar freemc.jar --cli [secenekler]   arayuzsuz kurulum/baslatma

                Secenekler:
                  --version, -v <surum>   Minecraft surumu (varsayilan 1.20.1)
                  --loader <ad>           vanilla | fabric (varsayilan vanilla)
                  --user, -u <ad>         oyuncu adi (varsayilan Player)
                  --ram <mb>              ayrilacak bellek (varsayilan 2048)
                  --mod <slug>            Modrinth'ten mod kur
                  --pack <slug>           Modrinth mod paketi kur
                  --launch                kurulumdan sonra oyunu baslat
                  --list-versions         surumleri listele
                  --help, -h              bu metni goster
                """);
    }

    private static String value(String[] args, int index, String fallback) {
        return index + 1 < args.length ? args[index + 1] : fallback;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
