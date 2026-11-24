package dev.freemc.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Archive extraction for downloaded runtimes and modpack bundles. */
public final class Archive {

    private Archive() {
    }

    public static void extract(Path archive, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        String name = archive.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".mrpack") || name.endsWith(".jar")) {
            unzip(archive, targetDir);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            untar(archive, targetDir);
        } else {
            throw new IOException("bilinmeyen arşiv türü: " + name);
        }
    }

    private static void unzip(Path archive, Path targetDir) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = safe(targetDir, entry.getName());
                if (target == null) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    zip.transferTo(out);
                }
            }
        }
    }

    private static void untar(Path archive, Path targetDir) throws IOException {
        try (InputStream raw = Files.newInputStream(archive);
             GZIPInputStream gzip = new GZIPInputStream(raw)) {
            byte[] header = new byte[512];
            while (true) {
                int read = readFully(gzip, header);
                if (read < 512) {
                    return;
                }
                String name = string(header, 0, 100);
                if (name.isEmpty()) {
                    return;
                }
                long size = octal(header, 124, 12);
                char type = (char) header[156];
                String prefix = string(header, 345, 155);
                String fullName = prefix.isEmpty() ? name : prefix + "/" + name;
                long padded = (size + 511) / 512 * 512;

                if (type == '5') {
                    Path dir = safe(targetDir, fullName);
                    if (dir != null) {
                        Files.createDirectories(dir);
                    }
                    skip(gzip, padded);
                    continue;
                }
                if (type != '0' && type != 0) {
                    skip(gzip, padded);
                    continue;
                }
                Path target = safe(targetDir, fullName);
                if (target != null) {
                    Files.createDirectories(target.getParent());
                    copy(gzip, target, size);
                } else {
                    skip(gzip, size);
                }
                skip(gzip, padded - size);
            }
        }
    }

    private static Path safe(Path root, String name) {
        Path target = root.resolve(name).normalize();
        return target.startsWith(root) ? target : null;
    }

    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return total;
    }

    private static void copy(InputStream in, Path target, long size) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long remaining = size;
        try (OutputStream out = Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.WRITE)) {
            while (remaining > 0) {
                int want = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, want);
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    private static void skip(InputStream in, long count) throws IOException {
        long remaining = count;
        byte[] buffer = new byte[64 * 1024];
        while (remaining > 0) {
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                return;
            }
            remaining -= read;
        }
    }

    private static String string(byte[] data, int offset, int length) {
        int end = offset;
        while (end < offset + length && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset).trim();
    }

    private static long octal(byte[] data, int offset, int length) {
        String text = string(data, offset, length).trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(text, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
