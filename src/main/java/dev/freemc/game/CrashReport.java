package dev.freemc.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads the newest crash report and turns it into a short, human-readable finding. */
public final class CrashReport {

    public record Finding(Path file, String description, String cause, List<String> frames, String summary) {
    }

    private CrashReport() {
    }

    public static Finding latest(Path gameDir) {
        Path dir = gameDir.resolve("crash-reports");
        if (!Files.isDirectory(dir)) {
            return null;
        }
        Path newest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "crash-*.txt")) {
            List<Path> files = new ArrayList<>();
            for (Path path : stream) {
                files.add(path);
            }
            files.sort(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed());
            if (!files.isEmpty()) {
                newest = files.get(0);
            }
        } catch (IOException e) {
            return null;
        }
        if (newest == null) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(newest, StandardCharsets.UTF_8);
            String description = firstAfter(lines, "Description:", 1);
            String cause = null;
            List<String> frames = new ArrayList<>();
            boolean inStack = false;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Caused by:") && cause == null) {
                    cause = trimmed;
                }
                if (trimmed.startsWith("at ") && frames.size() < 6 && inStack) {
                    frames.add(trimmed);
                }
                if (trimmed.startsWith("java.") || trimmed.startsWith("net.minecraft") || trimmed.contains("Exception")
                        || trimmed.contains("Error")) {
                    inStack = true;
                }
            }
            String summary = (description == null ? "Bilinmeyen çökme" : description)
                    + (cause == null ? "" : " — " + cause);
            return new Finding(newest, description, cause, frames, summary);
        } catch (IOException e) {
            return null;
        }
    }

    private static String firstAfter(List<String> lines, String marker, int offset) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(marker)) {
                int index = i + offset;
                if (index < lines.size()) {
                    return lines.get(index).trim();
                }
            }
        }
        return null;
    }
}
