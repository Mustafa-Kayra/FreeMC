package dev.freemc.game;

/** Progress sink used by the installer and the launcher. */
public interface Progress {

    void log(String message);

    default void files(int done, int total) {
    }

    Progress SILENT = message -> {
    };
}
