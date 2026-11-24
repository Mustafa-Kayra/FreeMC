package dev.freemc.ui;

import dev.freemc.game.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The version + loader the user currently has selected, shared by every tab. */
final class Selection {

    private String mcVersion = "";
    private String loader = Instance.VANILLA;
    private final List<Consumer<Selection>> listeners = new ArrayList<>();

    String mcVersion() {
        return mcVersion;
    }

    String loader() {
        return loader;
    }

    boolean ready() {
        return !mcVersion.isEmpty();
    }

    Instance instance() {
        return Instance.of(mcVersion, loader);
    }

    void set(String mcVersion, String loader) {
        boolean changed = !this.mcVersion.equals(mcVersion) || !this.loader.equals(loader);
        this.mcVersion = mcVersion == null ? "" : mcVersion;
        this.loader = Instance.FABRIC.equals(loader) ? Instance.FABRIC : Instance.VANILLA;
        if (changed) {
            for (Consumer<Selection> listener : listeners) {
                listener.accept(this);
            }
        }
    }

    void addListener(Consumer<Selection> listener) {
        listeners.add(listener);
    }
}
