package dev.freemc;

import dev.freemc.ui.LauncherFrame;

/** Entry point: opens the graphical launcher, or the headless CLI when asked. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--cli") || arg.startsWith("--list-versions") || arg.startsWith("--help")) {
                Cli.run(args);
                return;
            }
        }
        javax.swing.SwingUtilities.invokeLater(() -> new LauncherFrame().open());
    }
}
