package dev.freemc.ui;

import dev.freemc.Config;
import dev.freemc.Dirs;
import dev.freemc.game.Installer;
import dev.freemc.game.Progress;
import dev.freemc.game.VersionManifest;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;

/** Application shell: header, tab bar, panels and the shared console. */
public final class LauncherFrame extends JFrame {

    private final Config config = Config.load();
    private final Console console = new Console();
    private final Selection selection = new Selection();

    private final PlayPanel playPanel;
    private final ModsPanel modsPanel;
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final FlatButton playTab = new FlatButton("Oyna", FlatButton.Kind.TAB).active(true);
    private final FlatButton modsTab = new FlatButton("Modlar", FlatButton.Kind.TAB);
    private final JLabel instanceChip = Theme.label("sürüm yükleniyor…", Theme.SMALL, Theme.MUTED);

    public LauncherFrame() {
        super("FreeMC — Minecraft Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 660));
        setSize(1080, 780);
        setLocationRelativeTo(null);

        playPanel = new PlayPanel(config, selection, console);
        modsPanel = new ModsPanel(selection, console);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(body(), BorderLayout.CENTER);
        root.add(consoleArea(), BorderLayout.SOUTH);
        setContentPane(root);

        selection.addListener(updated -> instanceChip.setText(
                updated.ready() ? updated.instance().describe() : "sürüm seçilmedi"));
    }

    public void open() {
        setVisible(true);
        loadManifest();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 26, 6, 26));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(Theme.label("FreeMC", Theme.TITLE, Theme.ACCENT));
        brand.add(Box.createHorizontalStrut(12));
        brand.add(Theme.label("offline Minecraft launcher", Theme.SMALL, Theme.MUTED));
        brand.add(Box.createHorizontalGlue());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        RoundPanel chip = new RoundPanel(Theme.SURFACE, Theme.BORDER, 16);
        chip.setLayout(new BorderLayout());
        chip.setBorder(new EmptyBorder(6, 14, 6, 14));
        chip.add(instanceChip, BorderLayout.CENTER);
        right.add(chip);
        right.add(Box.createHorizontalStrut(10));
        FlatButton folder = new FlatButton("Klasör", FlatButton.Kind.GHOST);
        folder.addActionListener(event -> openDataFolder());
        right.add(folder);
        brand.add(right);
        header.add(brand, BorderLayout.NORTH);

        JPanel tabs = new JPanel();
        tabs.setOpaque(false);
        tabs.setLayout(new BoxLayout(tabs, BoxLayout.X_AXIS));
        playTab.addActionListener(event -> showTab(true));
        modsTab.addActionListener(event -> showTab(false));
        tabs.add(playTab);
        tabs.add(Box.createHorizontalStrut(6));
        tabs.add(modsTab);
        tabs.add(Box.createHorizontalGlue());
        header.add(tabs, BorderLayout.SOUTH);
        return header;
    }

    private JPanel body() {
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(4, 26, 6, 26));
        body.add(playPanel, "play");
        body.add(modsPanel, "mods");
        cards.show(body, "play");
        return body;
    }

    private JPanel consoleArea() {
        JPanel area = new JPanel(new BorderLayout(0, 6));
        area.setBackground(Theme.BG);
        area.setBorder(new EmptyBorder(0, 26, 18, 26));
        area.setPreferredSize(new Dimension(10, 210));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Konsol", Theme.SMALL, Theme.MUTED), BorderLayout.WEST);
        FlatButton clear = new FlatButton("Temizle", FlatButton.Kind.GHOST);
        clear.addActionListener(event -> console.clear());
        header.add(clear, BorderLayout.EAST);

        area.add(header, BorderLayout.NORTH);
        area.add(console, BorderLayout.CENTER);
        return area;
    }

    private void showTab(boolean play) {
        cards.show(body, play ? "play" : "mods");
        playTab.active(play);
        modsTab.active(!play);
        if (!play) {
            modsPanel.refreshInstalled();
        }
    }

    private void loadManifest() {
        console.log("Sürüm listesi alınıyor…");
        Thread worker = new Thread(() -> {
            try {
                VersionManifest manifest = Installer.manifest(false, new Progress() {
                    @Override
                    public void log(String message) {
                        console.log(message);
                    }
                });
                SwingUtilities.invokeLater(() -> {
                    playPanel.setVersions(manifest);
                    console.log("Sürüm listesi hazır: " + manifest.releases().size() + " sürüm");
                });
            } catch (IOException e) {
                console.log("Sürüm listesi alınamadı: " + e.getMessage());
                SwingUtilities.invokeLater(() -> instanceChip.setText("çevrimdışı"));
            }
        }, "freemc-manifest");
        worker.setDaemon(true);
        worker.start();
    }

    private void openDataFolder() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(Dirs.root().toFile());
                return;
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // fall through
        }
        console.log("Klasör: " + Dirs.root());
    }
}
