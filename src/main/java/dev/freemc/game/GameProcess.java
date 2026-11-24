package dev.freemc.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Starts the game and forwards its console output. */
public final class GameProcess {

    private GameProcess() {
    }

    public static Process launch(List<String> command, Path gameDir, Consumer<String> output) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(gameDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Thread reader = new Thread(() -> {
            try (BufferedReader reader1 = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader1.readLine()) != null) {
                    output.accept(line);
                }
            } catch (IOException ignored) {
                // the process exiting closes the stream, nothing to do
            }
        }, "freemc-game-output");
        reader.setDaemon(true);
        reader.start();
        return process;
    }
}
