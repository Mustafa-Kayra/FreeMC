package dev.freemc.game;

import dev.freemc.Dirs;
import dev.freemc.json.Json;

import java.util.List;
import java.util.Map;

/** A parsed Minecraft version file (versions/&lt;id&gt;/&lt;id&gt;.json). */
public final class Version {

    public final String id;
    public final String type;
    public final String mainClass;
    public final String assetsIndexId;
    public final RemoteFile assetIndex;
    public final RemoteFile client;
    public final int javaMajor;
    public final String logConfigArgument;
    public final RemoteFile logConfig;
    public final List<Object> argumentsJvm;
    public final List<Object> argumentsGame;
    public final String minecraftArguments;
    public final List<Library> libraries;
    /** The version this one layers on top of (Fabric/Quilt profiles), or null. */
    public final String inheritsFrom;
    /** Version id whose client jar this one runs, so a child profile reuses the parent jar. */
    public final String clientOwner;

    Version(String id, String type, String mainClass, String assetsIndexId, RemoteFile assetIndex,
            RemoteFile client, int javaMajor, String logConfigArgument, RemoteFile logConfig,
            List<Object> argumentsJvm, List<Object> argumentsGame, String minecraftArguments,
            List<Library> libraries, String inheritsFrom, String clientOwner) {
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
        this.inheritsFrom = inheritsFrom;
        this.clientOwner = clientOwner;
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
        if (jvmArgs != null && jvmArgs.isEmpty()) {
            jvmArgs = null;
        }

        String id = Json.str(root, "id");
        return new Version(
                id,
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
                Library.parseAll(Json.list(root, "libraries"), features),
                Json.str(root, "inheritsFrom"),
                id);
    }

    /** Layers a profile (Fabric/Quilt) over its parent version file. */
    static Version inherit(Version child, Version parent) {
        List<Library> libraries = new java.util.ArrayList<>(parent.libraries);
        libraries.addAll(child.libraries);

        List<Object> jvm = new java.util.ArrayList<>();
        if (parent.argumentsJvm != null) {
            jvm.addAll(parent.argumentsJvm);
        }
        if (child.argumentsJvm != null) {
            jvm.addAll(child.argumentsJvm);
        }

        List<Object> game = new java.util.ArrayList<>();
        if (parent.argumentsGame != null) {
            game.addAll(parent.argumentsGame);
        }
        if (child.argumentsGame != null) {
            game.addAll(child.argumentsGame);
        }

        return new Version(
                child.id,
                child.type,
                child.mainClass != null ? child.mainClass : parent.mainClass,
                child.assetIndex != null ? child.assetsIndexId : parent.assetsIndexId,
                child.assetIndex != null ? child.assetIndex : parent.assetIndex,
                child.client != null ? child.client : parent.client,
                Math.max(child.javaMajor, parent.javaMajor),
                child.logConfigArgument != null ? child.logConfigArgument : parent.logConfigArgument,
                child.logConfig != null ? child.logConfig : parent.logConfig,
                jvm.isEmpty() ? null : jvm,
                game.isEmpty() ? null : game,
                child.minecraftArguments != null ? child.minecraftArguments : parent.minecraftArguments,
                libraries,
                child.inheritsFrom,
                child.client != null ? child.id : parent.clientOwner);
    }

    /**
     * The client jar this version runs. A loader profile gets its own copy named after itself,
     * because loaders key their module ignore-lists on the version id.
     */
    public java.nio.file.Path clientJarPath() {
        return Dirs.versionDir(id).resolve(id + ".jar");
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
