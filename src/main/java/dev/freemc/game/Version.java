package dev.freemc.game;

import dev.freemc.json.Json;

import java.util.List;
import java.util.Map;

/** A parsed Minecraft version file (versions/&lt;id&gt;/&lt;id&gt;.json). */
public final class Version {

    public final String id;
    public final String type;
    final String mainClass;
    final String assetsIndexId;
    final RemoteFile assetIndex;
    final RemoteFile client;
    public final int javaMajor;
    final String logConfigArgument;
    final RemoteFile logConfig;
    final List<Object> argumentsJvm;
    final List<Object> argumentsGame;
    final String minecraftArguments;
    final List<Library> libraries;

    private Version(String id, String type, String mainClass, String assetsIndexId, RemoteFile assetIndex,
                    RemoteFile client, int javaMajor, String logConfigArgument, RemoteFile logConfig,
                    List<Object> argumentsJvm, List<Object> argumentsGame, String minecraftArguments,
                    List<Library> libraries) {
        this.id = id;
        this.type = type;
        this.mainClass = mainClass;
        this.assetsIndexId = assetsIndexId;
        this.assetIndex = assetIndex;
        this.client = client;
        this.javaMajor = javaMajor;
        this.logConfigArgument = logConfigArgument;
        this.logConfig = logConfig;
        this.argumentsJvm = argumentsJvm;
        this.argumentsGame = argumentsGame;
        this.minecraftArguments = minecraftArguments;
        this.libraries = libraries;
    }

    static Version parse(String json, Map<String, Boolean> features) {
        Map<String, Object> root = Json.object(json);
        Map<String, Object> downloads = Json.map(root, "downloads");
        RemoteFile client = remote(Json.map(downloads, "client"));

        Map<String, Object> assetIndexNode = Json.map(root, "assetIndex");
        String assetsId = Json.str(assetIndexNode, "id");
        if (assetsId == null) {
            assetsId = Json.str(root, "assets", "legacy");
        }
        RemoteFile assetIndex = remote(assetIndexNode);
        if (assetIndex != null) {
            assetIndex = new RemoteFile("indexes/" + assetsId + ".json", assetIndex.url, assetIndex.sha1, assetIndex.size);
        }

        Map<String, Object> loggingClient = Json.map(Json.map(root, "logging"), "client");
        String logArgument = Json.str(loggingClient, "argument");
        RemoteFile logConfig = remote(Json.map(loggingClient, "file"));
        if (logConfig != null) {
            String logId = Json.str(Json.map(loggingClient, "file"), "id", "client");
            logConfig = new RemoteFile("log_configs/" + logId, logConfig.url, logConfig.sha1, logConfig.size);
        }

        Map<String, Object> arguments = Json.map(root, "arguments");
        List<Object> jvmArgs = arguments == null ? null : Json.list(arguments, "jvm");
        List<Object> gameArgs = arguments == null ? null : Json.list(arguments, "game");
        if (gameArgs != null && gameArgs.isEmpty()) {
            gameArgs = null;
        }

        return new Version(
                Json.str(root, "id"),
                Json.str(root, "type", "release"),
                Json.str(root, "mainClass"),
                assetsId,
                assetIndex,
                client,
                (int) Json.num(Json.map(root, "javaVersion"), "majorVersion", 8),
                logArgument,
                logConfig,
                jvmArgs,
                gameArgs,
                Json.str(root, "minecraftArguments"),
                Library.parseAll(Json.list(root, "libraries"), features));
    }

    private static RemoteFile remote(Map<String, Object> node) {
        if (node == null) {
            return null;
        }
        String url = Json.str(node, "url");
        if (url == null) {
            return null;
        }
        return new RemoteFile(Json.str(node, "path"), url, Json.str(node, "sha1"), Json.num(node, "size", 0));
    }
}
