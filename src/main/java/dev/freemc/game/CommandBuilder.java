package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.Os;
import dev.freemc.json.Json;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the real java command line Minecraft expects, from the version file's templates. */
public final class CommandBuilder {

    public static final String LAUNCHER_NAME = "FreeMC";
    public static final String LAUNCHER_VERSION = "1.0.0";

    private CommandBuilder() {
    }

    public static List<String> build(GameInstall install, String javaCommand, String username, String uuid,
                                     String accessToken, int ramMb) {
        return build(install, javaCommand, username, uuid, accessToken, ramMb, "", "");
    }

    public static List<String> build(GameInstall install, String javaCommand, String username, String uuid,
                                     String accessToken, int ramMb, String extraJvmArgs, String quickServer) {
        return build(install, javaCommand, username, uuid, accessToken, ramMb, extraJvmArgs, quickServer, "legacy");
    }

    public static List<String> build(GameInstall install, String javaCommand, String username, String uuid,
                                     String accessToken, int ramMb, String extraJvmArgs, String quickServer,
                                     String userType) {
        Version version = install.version;
        Map<String, String> values = new HashMap<>();
        values.put("auth_player_name", username);
        values.put("auth_uuid", uuid);
        values.put("auth_access_token", accessToken);
        values.put("auth_session", "token:" + accessToken + ":" + uuid.replace("-", ""));
        values.put("auth_xuid", "");
        values.put("clientid", "");
        values.put("user_type", userType == null ? "legacy" : userType);
        values.put("user_properties", "{}");
        values.put("version_name", version.id);
        values.put("version_type", version.type);
        values.put("game_directory", install.gameDir.toAbsolutePath().toString());
        values.put("assets_root", install.assetsRoot.toAbsolutePath().toString());
        values.put("game_assets", install.assetsRoot.toAbsolutePath().toString());
        values.put("assets_index_name", version.assetsIndexId);
        values.put("natives_directory", install.nativesDir.toAbsolutePath().toString());
        values.put("launcher_name", LAUNCHER_NAME);
        values.put("launcher_version", LAUNCHER_VERSION);
        values.put("classpath", classpath(install));
        values.put("classpath_separator", File.pathSeparator);
        values.put("library_directory", Dirs.libraries().toAbsolutePath().toString());
        values.put("resolution_width", "854");
        values.put("resolution_height", "480");
        values.put("path", logConfigPath(version));

        List<String> command = new ArrayList<>();
        command.add(javaCommand);
        command.add("-Xms" + Math.min(512, ramMb) + "M");
        command.add("-Xmx" + Math.max(512, ramMb) + "M");
        if (extraJvmArgs != null && !extraJvmArgs.isBlank()) {
            for (String token : extraJvmArgs.trim().split("\\s+")) {
                if (!token.isBlank()) {
                    command.add(token);
                }
            }
        }

        if (version.argumentsJvm == null || version.argumentsJvm.isEmpty()) {
            command.add("-Dminecraft.launcher.brand=" + LAUNCHER_NAME);
            command.add("-Dminecraft.launcher.version=" + LAUNCHER_VERSION);
            command.add("-Djava.library.path=" + values.get("natives_directory"));
            command.add("-cp");
            command.add(values.get("classpath"));
        } else {
            command.addAll(resolve(version.argumentsJvm, values, booleans()));
        }

        command.add(version.mainClass);
        if (version.argumentsGame != null && !version.argumentsGame.isEmpty()) {
            command.addAll(resolve(version.argumentsGame, values, booleans()));
        } else if (version.minecraftArguments != null && !version.minecraftArguments.isBlank()) {
            for (String token : version.minecraftArguments.trim().split("\\s+")) {
                command.add(substitute(token, values));
            }
        } else {
            command.addAll(legacyArguments(values));
        }
        if (quickServer != null && !quickServer.isBlank()) {
            String host = quickServer.trim();
            String port = "25565";
            int colon = host.lastIndexOf(':');
            if (colon > 0) {
                port = host.substring(colon + 1);
                host = host.substring(0, colon);
            }
            command.add("--server");
            command.add(host);
            command.add("--port");
            command.add(port);
        }
        widenNativePath(command, javaCommand);
        return command;
    }

    /**
     * Old LWJGL (pre-1.13) links against libjawt/libawt, which live in the JDK's own lib and bin
     * directories rather than on java.library.path. Adding them keeps legacy versions loadable on
     * modern JVMs; newer versions simply ignore the extra path.
     */
    private static void widenNativePath(List<String> command, String javaCommand) {
        Path bin = Paths.get(javaCommand).toAbsolutePath().getParent();
        if (bin == null) {
            return;
        }
        Path home = bin.getParent();
        String extra = bin + File.pathSeparator + (home == null ? "" : home.resolve("lib").toString());
        for (int i = 0; i < command.size(); i++) {
            String arg = command.get(i);
            if (arg.startsWith("-Djava.library.path=")) {
                command.set(i, arg + File.pathSeparator + extra);
                return;
            }
        }
    }

    private static List<String> legacyArguments(Map<String, String> values) {
        List<String> args = new ArrayList<>();
        args.add("--username");
        args.add(values.get("auth_player_name"));
        args.add("--version");
        args.add(values.get("version_name"));
        args.add("--gameDir");
        args.add(values.get("game_directory"));
        args.add("--assetsDir");
        args.add(values.get("assets_root"));
        args.add("--assetIndex");
        args.add(values.get("assets_index_name"));
        args.add("--uuid");
        args.add(values.get("auth_uuid"));
        args.add("--accessToken");
        args.add(values.get("auth_access_token"));
        args.add("--userType");
        args.add(values.getOrDefault("user_type", "legacy"));
        args.add("--userProperties");
        args.add("{}");
        return args;
    }

    private static Map<String, Boolean> booleans() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("is_demo_user", false);
        features.put("has_custom_resolution", false);
        features.put("has_quick_plays_support", false);
        features.put("is_quick_play_singleplayer", false);
        features.put("is_quick_play_multiplayer", false);
        features.put("is_quick_play_realms", false);
        return features;
    }

    private static List<String> resolve(List<Object> raw, Map<String, String> values, Map<String, Boolean> features) {
        List<String> result = new ArrayList<>();
        for (Object element : raw) {
            if (element instanceof String text) {
                result.add(substitute(text, values));
            } else if (element instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) element;
                if (!Rule.allows(Rule.parse(Json.list(entry, "rules")), features)) {
                    continue;
                }
                Object value = entry.get("value");
                if (value instanceof String text) {
                    result.add(substitute(text, values));
                } else if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof String text) {
                            result.add(substitute(text, values));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String substitute(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String token = "${" + entry.getKey() + "}";
            if (result.contains(token)) {
                result = result.replace(token, entry.getValue());
            }
        }
        int open;
        while ((open = result.indexOf("${")) >= 0) {
            int close = result.indexOf('}', open);
            if (close < 0) {
                break;
            }
            result = result.substring(0, open) + result.substring(close + 1);
        }
        return result;
    }

    private static String classpath(GameInstall install) {
        return install.classpath.stream()
                .map(path -> path.toAbsolutePath().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    private static String logConfigPath(Version version) {
        if (version.logConfig == null) {
            return "";
        }
        return Dirs.assets().resolve(version.logConfig.path).toAbsolutePath().toString();
    }
}
