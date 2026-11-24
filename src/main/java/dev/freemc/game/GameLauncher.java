package dev.freemc.game;

import dev.freemc.auth.OfflineAuth;

import java.io.IOException;
import java.util.List;

/** Ensures a profile is installed and starts the game. */
public final class GameLauncher {

    private GameLauncher() {
    }

    public static GameInstall ensure(Profile profile, Progress progress) throws IOException {
        if (Profile.FABRIC.equals(profile.loader)) {
            String loaderVersion = profile.loaderVersion == null || profile.loaderVersion.isBlank()
                    ? Fabric.latestLoader(profile.mcVersion)
                    : profile.loaderVersion;
            profile.loaderVersion = loaderVersion;
            profile.save();
            progress.log("Fabric " + loaderVersion + " · Minecraft " + profile.mcVersion);
            return Installer.installProfile(Fabric.profileUrl(profile.mcVersion, loaderVersion),
                    profile.gameDir(), progress);
        }
        if (Profile.FORGE.equals(profile.loader) || Profile.NEOFORGE.equals(profile.loader)) {
            String versionId = Forge.install(profile, progress);
            return Installer.install(versionId, profile.gameDir(), progress);
        }
        return Installer.install(profile.mcVersion, profile.gameDir(), progress);
    }

    public static Process start(Profile profile, String username, Progress progress) throws IOException {
        GameInstall install = ensure(profile, progress);
        OfflineAuth auth = OfflineAuth.of(username);
        String java = JavaRuntime.ensure(install.version.javaMajor, progress);
        List<String> command = CommandBuilder.build(install, java, auth.name(), auth.uuid(), auth.accessToken(),
                profile.ramMb, profile.jvmArgs, profile.quickServer);
        progress.log("Başlatılıyor: " + auth.name() + "  (" + install.version.id + ")");
        return GameProcess.launch(command, install.gameDir, progress::log);
    }

    public static String commandLine(Profile profile, String username) throws IOException {
        GameInstall install = ensure(profile, new Progress() {
            @Override
            public void log(String message) {
            }
        });
        OfflineAuth auth = OfflineAuth.of(username);
        String java = JavaRuntime.ensure(install.version.javaMajor, message -> {
        });
        return String.join(System.lineSeparator(),
                CommandBuilder.build(install, java, auth.name(), auth.uuid(), auth.accessToken(),
                        profile.ramMb, profile.jvmArgs, profile.quickServer));
    }
}
