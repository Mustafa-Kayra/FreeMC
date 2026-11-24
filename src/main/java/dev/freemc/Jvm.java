package dev.freemc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Finds a Java runtime new enough for the version being launched. */
public final class Jvm {

    private Jvm() {
    }

    public static String javaCommand() {
        return currentJava();
    }

    public static String forVersion(int requiredMajor, ProgressHook hook) {
        String current = currentJava();
        int currentMajor = majorOf(current);
        if (currentMajor >= requiredMajor) {
            if (requiredMajor <= 8 && currentMajor > 8) {
                // pre-1.13 versions ship LWJGL 2 and run most reliably on the Java they were built for
                String exact = findExact(candidates(), requiredMajor);
                if (exact != null) {
                    hook.info("Java " + requiredMajor + " kullanılıyor (eski sürüm): " + exact);
                    return exact;
                }
            }
            return current;
        }
        String best = null;
        int bestMajor = currentMajor;
        for (String candidate : candidates()) {
            int major = majorOf(candidate);
            if (major >= requiredMajor && major > bestMajor) {
                best = candidate;
                bestMajor = major;
            }
        }
        if (best != null) {
            hook.info("Java " + bestMajor + " kullanılıyor: " + best);
            return best;
        }
        hook.info("Uyarı: bu sürüm Java " + requiredMajor + " istiyor, bulunan en yüksek sürüm Java "
                + currentMajor + ". Oyun açılmayabilir; Java " + requiredMajor + "+ kurmayı dene.");
        return current;
    }

    public interface ProgressHook {
        void info(String message);
    }

    private static String findExact(Set<String> candidates, int requiredMajor) {
        for (String candidate : candidates) {
            if (majorOf(candidate) == requiredMajor) {
                return candidate;
            }
        }
        return null;
    }

    private static String currentJava() {
        String home = System.getProperty("java.home");
        Path executable = Paths.get(home, "bin", Os.javaExecutableName());
        if (!Files.isExecutable(executable)) {
            executable = Paths.get(home, "bin", "java");
        }
        return executable.toAbsolutePath().toString();
    }

    private static Set<String> candidates() {
        Set<Path> homes = new LinkedHashSet<>();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            homes.add(Paths.get(javaHome));
        }
        List<Path> roots = new ArrayList<>();
        if (Os.isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFiles != null) {
                roots.add(Paths.get(programFiles, "Java"));
                roots.add(Paths.get(programFiles, "Eclipse Adoptium"));
                roots.add(Paths.get(programFiles, "Microsoft"));
                roots.add(Paths.get(programFiles, "Zulu"));
                roots.add(Paths.get(programFiles, "BellSoft"));
            }
            if (programFilesX86 != null) {
                roots.add(Paths.get(programFilesX86, "Java"));
            }
        } else {
            roots.add(Paths.get("/usr/lib/jvm"));
            roots.add(Paths.get("/usr/java"));
            roots.add(Paths.get(System.getProperty("user.home"), ".jdks"));
        }
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.list(root)) {
                stream.filter(Files::isDirectory).forEach(homes::add);
            } catch (IOException ignored) {
                // unreadable candidates are skipped
            }
        }
        Set<String> executables = new LinkedHashSet<>();
        for (Path home : homes) {
            Path executable = home.resolve("bin").resolve(Os.javaExecutableName());
            if (!Files.isExecutable(executable)) {
                executable = home.resolve("bin").resolve("java");
            }
            if (Files.isExecutable(executable)) {
                executables.add(executable.toAbsolutePath().toString());
            }
        }
        return executables;
    }

    private static int majorOf(String executable) {
        try {
            Process process = new ProcessBuilder(executable, "-version")
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (var in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            int index = output.indexOf("version \"");
            if (index >= 0) {
                String version = output.substring(index + 9);
                int end = version.indexOf('"');
                if (end > 0) {
                    version = version.substring(0, end);
                }
                String[] parts = version.split("[._-]");
                if (parts.length > 0) {
                    int major = Integer.parseInt(parts[0]);
                    if (major == 1 && parts.length > 1) {
                        return Integer.parseInt(parts[1]);
                    }
                    return major;
                }
            }
        } catch (IOException | InterruptedException | NumberFormatException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return 0;
    }
}
