package dev.freemc.ui;

import dev.freemc.Dirs;
import dev.freemc.Lang;
import dev.freemc.game.GameLauncher;
import dev.freemc.game.Profile;
import dev.freemc.game.Progress;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/** Application shell: sidebar, header, page area and the console drawer. */
public final class Shell extends JFrame {

    private final AppState state;
    private final Console console = new Console();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, FlatButton> navButtons = new LinkedHashMap<>();

    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusText = Theme.label(Lang.t("header.ready"), Theme.body(11f), Theme.TEXT_MUTED);
    private final JLabel userLabel = Theme.label("", Theme.body(Font13), Theme.TEXT);
    private final JTextField search = Theme.field(22);
    private final JPanel drawer = new JPanel(new BorderLayout(0, 4));
    private final JPanel bottomBox = new JPanel(new BorderLayout());

    private static final float Font13 = 12.5f;

    public Shell(AppState state) {
        super("FreeMC");
        this.state = state;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);

        DashboardPage dashboard = new DashboardPage(state, this);
        LibraryPage library = new LibraryPage(state, this);
        ProfilePage profile = new ProfilePage(state, this);
        SettingsPage settings = new SettingsPage(state, this);
        for (Page page : new Page[]{dashboard, library, profile, settings}) {
            pages.put(page.key(), page);
            content.add(page.view(), page.key());
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(sidebar(), BorderLayout.WEST);

        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(Theme.BG);
        centre.add(header(), BorderLayout.NORTH);
        centre.add(content, BorderLayout.CENTER);
        bottomBox.setBackground(Theme.BG);
        bottomBox.setBorder(new EmptyBorder(0, 22, 14, 22));
        centre.add(bottomBox, BorderLayout.SOUTH);
        root.add(centre, BorderLayout.CENTER);
        setContentPane(root);

        state.addListener(updated -> refreshUser());
        refreshUser();
        show("dashboard");
        rebuildDrawer();
    }

    /** Shows the window and kicks off the version list fetch. */
    public void start() {
        setVisible(true);
        SwingUtilities.invokeLater(() -> state.loadManifest(console::log));
    }

    /** Rebuilds the whole window, used after switching language or theme. */
    public void restartUi() {
        AppState fresh = new AppState();
        Theme.init(!"light".equals(fresh.config.theme()));
        Shell shell = new Shell(fresh);
        shell.start();
        dispose();
    }

    // ---------------------------------------------------------------- chrome

    private JComponent sidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SURFACE_LOW);
        sidebar.setPreferredSize(new Dimension(232, 10));

        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(Theme.SURFACE_LOW);
        brand.setBorder(new EmptyBorder(16, 18, 14, 18));
        JLabel name = Theme.label("FREEMC", Theme.displayTracked(19f), Theme.TEXT);
        JLabel tag = Theme.label(Lang.t("app.tagline"), Theme.body(10.5f), Theme.OUTLINE);
        brand.add(name, BorderLayout.NORTH);
        brand.add(tag, BorderLayout.SOUTH);

        JPanel nav = new JPanel();
        nav.setBackground(Theme.SURFACE_LOW);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(4, 8, 8, 8));
        JLabel heading = Theme.heading(Lang.t("nav.navigate"));
        heading.setBorder(new EmptyBorder(4, 8, 8, 8));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(heading);
        addNav(nav, "dashboard", Lang.t("nav.dashboard"), "dashboard");
        addNav(nav, "library", Lang.t("nav.library"), "library");
        addNav(nav, "profile", Lang.t("nav.profile"), "user");
        addNav(nav, "settings", Lang.t("nav.settings"), "settings");
        nav.add(Box.createVerticalGlue());

        JPanel footer = new JPanel();
        footer.setBackground(Theme.SURFACE_LOW);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(10, 16, 14, 16));
        JLabel capLabel = Theme.label("RAM", Theme.heading(Lang.t("header.ready")).getFont(), Theme.OUTLINE);
        JLabel ramText = Theme.label(ramSummary(), Theme.body(11f), Theme.MINT);
        JPanel ramRow = new JPanel(new BorderLayout());
        ramRow.setOpaque(false);
        ramRow.add(capLabel, BorderLayout.WEST);
        ramRow.add(ramText, BorderLayout.EAST);
        ramRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(ramRow);
        footer.add(Box.createVerticalStrut(6));
        RoundPanel bar = new RoundPanel(Theme.SURFACE_TOP, null, 3);
        bar.setLayout(new BorderLayout());
        bar.setPreferredSize(new Dimension(10, 4));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(bar);
        footer.add(Box.createVerticalStrut(10));
        JLabel version = Theme.label("v1.2.0 · offline", Theme.mono(10f), Theme.OUTLINE);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(version);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private String ramSummary() {
        Profile active = state.active();
        long total = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        return (active == null ? "-" : active.ramMb + " MB") + " / " + total + " MB";
    }

    private void addNav(JPanel parent, String key, String label, String icon) {
        FlatButton button = new FlatButton(label, FlatButton.Kind.NAV).icon(icon);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        button.addActionListener(event -> show(key));
        navButtons.put(key, button);
        parent.add(button);
        parent.add(Box.createVerticalStrut(2));
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.SURFACE_LOW);
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        RoundPanel searchBox = new RoundPanel(Theme.SURFACE_HIGH, Theme.OUTLINE_SOFT, 8);
        searchBox.setLayout(new BorderLayout(6, 0));
        searchBox.setBorder(new EmptyBorder(2, 8, 2, 8));
        searchBox.setPreferredSize(new Dimension(340, 34));
        searchBox.add(new SearchIcon(), BorderLayout.WEST);
        search.setOpaque(false);
        search.setBorder(new EmptyBorder(6, 2, 6, 2));
        search.setToolTipText(Lang.t("header.search"));
        search.addActionListener(event -> {
            show("library");
            Page page = pages.get("library");
            if (page instanceof LibraryPage library) {
                library.searchFor(search.getText());
            }
        });
        searchBox.add(search, BorderLayout.CENTER);
        left.add(searchBox, BorderLayout.WEST);

        JPanel statusBox = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        statusBox.setOpaque(false);
        statusDot.setFont(Theme.body(10f));
        statusDot.setForeground(Theme.MINT);
        statusBox.add(statusDot);
        statusBox.add(statusText);
        left.add(statusBox, BorderLayout.CENTER);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        FlatButton consoleButton = new FlatButton("", FlatButton.Kind.ICON).icon("layers");
        consoleButton.setToolTipText(Lang.t("common.console"));
        consoleButton.addActionListener(event -> toggleConsole());
        right.add(consoleButton);

        RoundPanel userChip = new RoundPanel(Theme.SURFACE_HIGH, Theme.OUTLINE_SOFT, 8);
        userChip.setLayout(new BorderLayout(8, 0));
        userChip.setBorder(new EmptyBorder(3, 8, 3, 12));
        RoundPanel avatar = new RoundPanel(Theme.ACCENT, (Color) null, 16);
        avatar.setPreferredSize(new Dimension(26, 26));
        avatar.setLayout(new BorderLayout());
        JLabel initial = Theme.label("?", Theme.display(13f), Theme.ACCENT_INK);
        initial.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        avatar.add(initial, BorderLayout.CENTER);
        userChip.add(avatar, BorderLayout.WEST);
        JPanel userText = new JPanel(new GridLayout(2, 1, 0, -2));
        userText.setOpaque(false);
        userText.add(userLabel);
        userText.add(Theme.label(Lang.t("header.online"), Theme.body(10f), Theme.MINT));
        userChip.add(userText, BorderLayout.CENTER);
        right.add(userChip);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        userInitial = initial;
        return header;
    }

    private JLabel userInitial;

    private void refreshUser() {
        if (userLabel != null) {
            userLabel.setText(state.username());
        }
        if (userInitial != null) {
            String name = state.username();
            userInitial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        }
    }

    private JComponent searchIcon() {
        return new SearchIcon();
    }

    private static final class SearchIcon extends JComponent {
        SearchIcon() {
            setPreferredSize(new Dimension(16, 16));
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            Icons.draw((java.awt.Graphics2D) graphics, "search", 16, Theme.OUTLINE);
        }
    }

    private void rebuildDrawer() {
        drawer.setBackground(Theme.BG);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(Theme.heading(Lang.t("common.console")), BorderLayout.WEST);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        FlatButton clear = new FlatButton(Lang.t("common.clear"), FlatButton.Kind.GHOST);
        clear.addActionListener(event -> console.clear());
        FlatButton close = new FlatButton("", FlatButton.Kind.ICON).icon("close");
        close.addActionListener(event -> toggleConsole());
        actions.add(clear);
        actions.add(close);
        bar.add(actions, BorderLayout.EAST);
        drawer.removeAll();
        drawer.add(bar, BorderLayout.NORTH);
        drawer.add(console, BorderLayout.CENTER);
        drawer.setPreferredSize(new Dimension(10, 190));
        drawer.setBorder(new EmptyBorder(6, 0, 8, 0));
        bottomBox.removeAll();
        bottomBox.add(drawer, BorderLayout.CENTER);
        bottomBox.setVisible(false);
    }

    public void toggleConsole() {
        boolean show = !bottomBox.isVisible();
        bottomBox.setVisible(show);
        bottomBox.revalidate();
        bottomBox.repaint();
    }

    // ---------------------------------------------------------------- behaviour

    public void show(String key) {
        Page page = pages.get(key);
        if (page == null) {
            return;
        }
        cards.show(content, key);
        for (Map.Entry<String, FlatButton> entry : navButtons.entrySet()) {
            entry.getValue().active(entry.getKey().equals(key));
        }
        page.onShow();
    }

    public Console console() {
        return console;
    }

    public AppState state() {
        return state;
    }

    public void log(String line) {
        console.log(line);
    }

    public void status(String text, boolean busy) {
        SwingUtilities.invokeLater(() -> {
            statusText.setText(text);
            statusDot.setForeground(busy ? Theme.ACCENT : Theme.MINT);
        });
    }

    public void refreshAll() {
        state.reloadProfiles();
        refreshUser();
        for (Page page : pages.values()) {
            if (page.view() == null) {
                continue;
            }
            page.onShow();
        }
    }

    public void launch(Profile profile) {
        if (profile == null) {
            return;
        }
        state.setActive(profile);
        status(Lang.t("status.install") + " · " + profile.name, true);
        long started = System.currentTimeMillis();
        Thread worker = new Thread(() -> {
            try {
                Process process = GameLauncher.start(profile, state.username(), new Progress() {
                    @Override
                    public void log(String message) {
                        console.log(message);
                    }
                });
                status(Lang.t("status.running") + " · pid " + process.pid(), true);
                int code = process.waitFor();
                profile.addPlayTime((System.currentTimeMillis() - started) / 1000);
                console.log("Oyun kapandı (çıkış kodu " + code + ")");
                status(Lang.t("status.closed"), false);
                SwingUtilities.invokeLater(this::refreshAll);
            } catch (Exception e) {
                console.log("HATA: " + e.getMessage());
                status(Lang.t("status.error") + ": " + e.getMessage(), false);
            }
        }, "freemc-launch");
        worker.setDaemon(true);
        worker.start();
    }

    public Profile newProfile() {
        ProfileDialog dialog = new ProfileDialog(this, state);
        Profile created = dialog.open();
        if (created != null) {
            state.add(created);
            log("Profil oluşturuldu: " + created.name);
            refreshAll();
        }
        return created;
    }

    public void openDataFolder() {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(Dirs.root().toFile());
                return;
            }
        } catch (Exception ignored) {
            // fall through
        }
        log("Klasör: " + Dirs.root());
    }

    /** Small modal for creating a profile. */
    private static final class ProfileDialog extends JDialog {

        private final AppState state;
        private Profile result;

        ProfileDialog(JFrame owner, AppState state) {
            super(owner, Lang.t("dash.new"), true);
            this.state = state;
            setLayout(new BorderLayout(0, 12));
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(new EmptyBorder(18, 18, 8, 18));
            body.setBackground(Theme.SURFACE_LOW);

            JTextField name = Theme.field(18);
            javax.swing.JComboBox<String> versionBox = Theme.combo();
            for (String version : state.releaseVersions()) {
                versionBox.addItem(version);
            }
            if (versionBox.getItemCount() == 0) {
                versionBox.addItem("1.20.1");
            }
            javax.swing.JComboBox<String> loaderBox = Theme.combo();
            loaderBox.addItem("Vanilla");
            loaderBox.addItem("Fabric");
            loaderBox.addItem("Forge");
            loaderBox.addItem("NeoForge");
            javax.swing.JSpinner ram = new javax.swing.JSpinner(
                    new javax.swing.SpinnerNumberModel(4096, 1024, 32768, 512));
            Theme.styleSpinner(ram);

            body.add(Theme.label(Lang.t("prof.name"), Theme.body(11f), Theme.OUTLINE));
            body.add(name);
            body.add(javax.swing.Box.createVerticalStrut(10));
            body.add(Theme.label("Minecraft", Theme.body(11f), Theme.OUTLINE));
            body.add(versionBox);
            body.add(javax.swing.Box.createVerticalStrut(10));
            body.add(Theme.label("Loader", Theme.body(11f), Theme.OUTLINE));
            body.add(loaderBox);
            body.add(javax.swing.Box.createVerticalStrut(10));
            body.add(Theme.label(Lang.t("set.memory"), Theme.body(11f), Theme.OUTLINE));
            body.add(ram);

            JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
            actions.setBackground(Theme.SURFACE_LOW);
            FlatButton cancel = new FlatButton(Lang.t("common.cancel"), FlatButton.Kind.GHOST);
            cancel.addActionListener(event -> dispose());
            FlatButton create = new FlatButton(Lang.t("dash.create"), FlatButton.Kind.PRIMARY);
            create.addActionListener(event -> {
                String profileName = name.getText().isBlank()
                        ? (String) versionBox.getSelectedItem() + " " + loaderBox.getSelectedItem()
                        : name.getText().trim();
                String loader = switch ((String) loaderBox.getSelectedItem()) {
                    case "Fabric" -> Profile.FABRIC;
                    case "Forge" -> Profile.FORGE;
                    case "NeoForge" -> Profile.NEOFORGE;
                    default -> Profile.VANILLA;
                };
                result = Profile.create(profileName, (String) versionBox.getSelectedItem(), loader, "");
                result.ramMb = (Integer) ram.getValue();
                result.save();
                dispose();
            });
            actions.add(cancel);
            actions.add(create);

            add(body, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setSize(420, 400);
            setLocationRelativeTo(owner);
        }

        Profile open() {
            setVisible(true);
            return result;
        }
    }
}
