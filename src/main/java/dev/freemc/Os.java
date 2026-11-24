package dev.freemc;

/** Maps the running JVM onto the os/arch vocabulary that Minecraft version files use. */
public final class Os {

    public static final String NAME;
    public static final String ARCH;
    public static final String VERSION;

    static {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            NAME = "windows";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            NAME = "osx";
        } else {
            NAME = "linux";
        }

        String osArch = System.getProperty("os.arch", "").toLowerCase();
        if (osArch.equals("amd64") || osArch.equals("x86_64") || osArch.equals("x64")) {
            ARCH = "x86_64";
        } else if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            ARCH = "arm64";
        } else if (osArch.startsWith("arm")) {
            ARCH = "arm32";
        } else {
            ARCH = "x86";
        }

        VERSION = System.getProperty("os.version", "");
    }

    private Os() {
    }

    public static boolean isWindows() {
        return "windows".equals(NAME);
    }

    public static String javaExecutableName() {
        return isWindows() ? "javaw.exe" : "java";
    }
}
