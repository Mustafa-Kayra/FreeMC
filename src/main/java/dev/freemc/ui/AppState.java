package dev.freemc.ui;

import dev.freemc.Config;
import dev.freemc.Lang;
import dev.freemc.game.Profile;
import dev.freemc.game.VersionManifest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Shared application state: settings, profiles, the active profile and the version list. */
public final class AppState {

    public final Config config;
    private final List<Profile> profiles = new ArrayList<>();
    private Profile active;
    private VersionManifest manifest;
    private final List<Consumer<AppState>> listeners = new ArrayList<>();

    public AppState() {
        this.config = Config.load();
        Lang.english("en".equalsIgnoreCase(config.language()));
        reloadProfiles();
        String wanted = config.profile();
        for (Profile profile : profiles) {
            if (profile.id.equals(wanted)) {
                active = profile;
                break;
            }
        }
        if (active == null && !profiles.isEmpty()) {
            active = profiles.get(0);
        }
        if (active != null) {
            config.profile(active.id).save();
        }
    }

    public void reloadProfiles() {
        profiles.clear();
        profiles.addAll(Profile.all());
        if (active != null) {
            for (Profile profile : profiles) {
                if (profile.id.equals(active.id)) {
                    active = profile;
                }
            }
        }
    }

    public List<Profile> profiles() {
        return new ArrayList<>(profiles);
    }

    public Profile active() {
        return active;
    }

    public void setActive(Profile profile) {
        this.active = profile;
        if (profile != null) {
            config.profile(profile.id).save();
        }
        notifyListeners();
    }

    public void add(Profile profile) {
        reloadProfiles();
        setActive(profile);
    }

    public VersionManifest manifest() {
        return manifest;
    }

    public void manifest(VersionManifest value) {
        this.manifest = value;
        notifyListeners();
    }

    public String username() {
        return config.username();
    }

    public void username(String value) {
        config.username(value).save();
        notifyListeners();
    }

    public void save() {
        config.save();
        if (active != null) {
            active.save();
        }
    }

    public List<String> releaseVersions() {
        List<String> versions = new ArrayList<>();
        if (manifest != null) {
            for (VersionManifest.Entry entry : manifest.releases()) {
                versions.add(entry.id());
            }
        }
        return versions;
    }

    public long totalPlaySeconds() {
        long total = 0;
        for (Profile profile : profiles) {
            total += profile.playSeconds;
        }
        return total;
    }

    public int totalMods() {
        int total = 0;
        for (Profile profile : profiles) {
            total += profile.modCount();
        }
        return total;
    }

    public void addListener(Consumer<AppState> listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        for (Consumer<AppState> listener : listeners) {
            listener.accept(this);
        }
    }

    public void loadManifest(ProgressSink sink) {
        Thread worker = new Thread(() -> {
            try {
                manifest = dev.freemc.game.Installer.manifest(false, sink::log);
                sink.log("Sürüm listesi hazır: " + manifest.releases().size() + " sürüm");
                notifyListeners();
            } catch (IOException e) {
                sink.log("Sürüm listesi alınamadı: " + e.getMessage());
            }
        }, "freemc-manifest");
        worker.setDaemon(true);
        worker.start();
    }

    /** Minimal logging callback so the state layer stays free of UI types. */
    public interface ProgressSink {
        void log(String message);
    }
}
