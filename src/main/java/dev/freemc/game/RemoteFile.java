package dev.freemc.game;

/** One file to fetch: where it goes (relative to a root) and where it comes from. */
final class RemoteFile {

    final String path;
    final String url;
    final String sha1;
    final long size;

    RemoteFile(String path, String url, String sha1, long size) {
        this.path = path;
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
    }
}
