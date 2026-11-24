package dev.freemc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small persisted settings file at the data root. */
public final class Config {

    private static final Path FILE = Dirs.root().resolve("freemc.properties");

    private final Properties props = new Properties();

    private Config() {
    }

    public static Config load() {
        Config config = new Config();
        config.props.setProperty("username", "Player");
        config.props.setProperty("ramMb", "2048");
        config.props.setProperty("version", "");
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                config.props.load(in);
            } catch (IOException ignored) {
                // a damaged settings file falls back to defaults
            }
        }
        return config;
    }

    public String username() {
        return props.getProperty("username", "Player");
    }

    public Config username(String value) {
        props.setProperty("username", value == null || value.isBlank() ? "Player" : value.trim());
        return this;
    }

    public int ramMb() {
        try {
            return Integer.parseInt(props.getProperty("ramMb", "2048"));
        } catch (NumberFormatException e) {
            return 2048;
        }
    }

    public Config ramMb(int value) {
        props.setProperty("ramMb", Integer.toString(value));
        return this;
    }

    public String version() {
        return props.getProperty("version", "");
    }

    public Config version(String value) {
        props.setProperty("version", value == null ? "" : value);
        return this;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "FreeMC settings");
            }
        } catch (IOException ignored) {
            // settings persistence is best-effort
        }
    }
}
