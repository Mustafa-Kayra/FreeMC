package dev.freemc.game;

import dev.freemc.Jvm;
import dev.freemc.auth.OfflineAuth;

import java.io.IOException;
import java.util.List;

/** Ensures an instance is installed and starts the game. */
public final class GameLauncher {

    private GameLauncher() {
    }

    public static Process start(Instance instance, String username, int ramMb, Progress progress) throws IOException {
        GameInstall install = ensure(instance, progress);
        OfflineAuth auth = OfflineAuth.of(username);
        String java = Jvm.forVersion(install.version.javaMajor, progress::log);
        List<String> command = CommandBuilder.build(install, java, auth.name(), auth.uuid(), auth.accessToken(), ramMb);
        progress.log("Başlatılıyor: " + auth.name() + "  (" + install.version.id + ")");
        return GameProcess.launch(command, install.gameDir, progress::log);
    }

    /** Installs (or repairs) the instance without starting anything. */
    public static GameInstall ensure(Instance instance, Progress progress) throws IOException {
        if (Instance.FABRIC.equals(instance.loader)) {
            String loaderVersion = Fabric.latestLoader(instance.mcVersion);
            instance.loaderVersion(loaderVersion);
            progress.log("Fabric " + loaderVersion + " · Minecraft " + instance.mcVersion);
            return Installer.installProfile(Fabric.profileUrl(instance.mcVersion, loaderVersion),
                    instance.gameDir(), progress);
        }
        return Installer.install(instance.mcVersion, instance.gameDir(), progress);
    }
}
