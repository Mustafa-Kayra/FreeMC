package dev.freemc;

import dev.freemc.ui.AppState;
import dev.freemc.ui.Shell;
import dev.freemc.ui.Theme;

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
        AppState state = new AppState();
        Theme.init(!"light".equals(state.config.theme()));
        javax.swing.SwingUtilities.invokeLater(() -> new Shell(state).start());
    }
}
