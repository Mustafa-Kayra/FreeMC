package dev.freemc.ui;

import dev.freemc.Lang;
import dev.freemc.game.Profile;
import dev.freemc.game.Progress;
import dev.freemc.mods.ModUpdates;
import dev.freemc.mods.Modpacks;
import dev.freemc.mods.Modrinth;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Modrinth browser for modpacks and single mods, with the profile's installed list beside it. */
public final class LibraryPage implements Page {

    private final AppState state;
    private final Shell shell;

    private final JPanel view = new JPanel(new BorderLayout(0, 12));
    private final JPanel body = new JPanel(new BorderLayout(14, 0));
    private final JPanel results = new JPanel();
    private final JPanel installedBox = new JPanel();
    private final JTextField query = Theme.field(24);
    private final FlatButton packsTab = new FlatButton(Lang.t("lib.modpacks"), FlatButton.Kind.TAB);
    private final FlatButton modsTab = new FlatButton(Lang.t("lib.mods"), FlatButton.Kind.TAB);
    private final FlatButton searchButton = new FlatButton(Lang.t("lib.search"), FlatButton.Kind.GHOST)
            .icon("search");

    private String mode = "modpacks";
    private JComponent detail;
    private boolean busy;

    LibraryPage(AppState state, Shell shell) {
        this.state = state;
        this.shell = shell;
        view.setBackground(Theme.BG);
        view.setBorder(new EmptyBorder(16, 22, 16, 22));

        view.add(toolbar(), BorderLayout.NORTH);
        view.add(body, BorderLayout.CENTER);

        packsTab.addActionListener(event -> {
            mode = "modpacks";
            detail = null;
            refreshTabs();
            searchFor(query.getText());
        });
        modsTab.addActionListener(event -> {
            mode = "mods";
            detail = null;
            refreshTabs();
            searchFor(query.getText());
        });
        searchButton.addActionListener(event -> searchFor(query.getText()));
        query.addActionListener(event -> searchFor(query.getText()));

        refreshTabs();
        rebuildBody();
    }

    @Override
    public String key() {
        return "library";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        rebuildBody();
    }

    private JComponent toolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(packsTab);
        left.add(modsTab);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);
        query.setPreferredSize(new Dimension(320, 32));
        query.setToolTipText(Lang.t("lib.search.ph"));
        right.add(query, BorderLayout.CENTER);
        right.add(searchButton, BorderLayout.EAST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void refreshTabs() {
        packsTab.active("modpacks".equals(mode));
        modsTab.active("mods".equals(mode));
    }

    public void searchFor(String text) {
        if (busy) {
            return;
        }
        detail = null;
        rebuildBody();
        busy = true;
        searchButton.setEnabled(false);
        results.removeAll();
        results.add(empty(Lang.t("lib.search") + "…"));
        results.revalidate();

        Thread worker = new Thread(() -> {
            try {
                if ("modpacks".equals(mode)) {
                    List<Modpacks.Hit> hits = Modpacks.search(text, 20);
                    SwingUtilities.invokeLater(() -> renderPacks(hits));
                } else {
                    Profile active = state.active();
                    if (active == null) {
                        SwingUtilities.invokeLater(() -> results.add(empty(Lang.t("lib.need.profile"))));
                        return;
                    }
                    List<Modrinth.Hit> hits = Modrinth.search(text, active.mcVersion, active.loader, 20);
                    SwingUtilities.invokeLater(() -> renderMods(hits));
                }
            } catch (IOException e) {
                shell.log("Modrinth hatası: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    results.removeAll();
                    results.add(empty(e.getMessage()));
                    results.revalidate();
                });
            } finally {
                busy = false;
                SwingUtilities.invokeLater(() -> searchButton.setEnabled(true));
            }
        }, "freemc-library-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void rebuildBody() {
        body.removeAll();
        if (detail != null) {
            body.add(detail, BorderLayout.CENTER);
        } else {
            RoundPanel listCard = Theme.card(new BorderLayout(0, 8), 12);
            listCard.add(headerRow(), BorderLayout.NORTH);
            results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
            results.setOpaque(false);
            if (results.getComponentCount() == 0) {
                results.add(empty(Lang.t("lib.search.ph")));
            }
            JScrollPane scroll = new JScrollPane(results);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Theme.SURFACE_LOW);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            listCard.add(scroll, BorderLayout.CENTER);
            body.add(listCard, BorderLayout.CENTER);

            installedBox.setLayout(new BoxLayout(installedBox, BoxLayout.Y_AXIS));
            body.add(installedCard(), BorderLayout.EAST);
        }
        body.revalidate();
        body.repaint();
    }

    private JComponent headerRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        String title = "modpacks".equals(mode) ? Lang.t("lib.modpacks") : Lang.t("lib.mods");
        row.add(Theme.heading(title), BorderLayout.WEST);
        Profile active = state.active();
        if (active != null) {
            row.add(Chip.mint(active.name), BorderLayout.EAST);
        }
        return row;
    }

    private JComponent installedCard() {
        RoundPanel card = Theme.card(new BorderLayout(0, 8), 12);
        card.setPreferredSize(new Dimension(330, 10));
        card.add(Theme.heading(Lang.t("lib.installed")), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);

        if ("mods".equals(mode)) {
            Profile active = state.active();
            if (active == null) {
                list.add(empty(Lang.t("lib.need.profile")));
            } else if (!active.modded()) {
                list.add(empty(Lang.t("lib.need.modded")));
            } else {
                List<Path> jars = Modrinth.installed(active.modsDir());
                if (jars.isEmpty()) {
                    list.add(empty(Lang.t("lib.empty")));
                }
                for (Path jar : jars) {
                    list.add(modRow(jar));
                    list.add(Box.createVerticalStrut(5));
                }
                FlatButton add = new FlatButton(Lang.t("lib.addfile"), FlatButton.Kind.GHOST).icon("download");
                add.addActionListener(event -> addLocalMod());
                FlatButton updates = new FlatButton(Lang.t("lib.checkupdates"), FlatButton.Kind.GHOST)
                        .icon("refresh");
                updates.addActionListener(event -> checkUpdates(updates));
                actions.add(add);
                actions.add(updates);
            }
        } else {
            for (Profile profile : state.profiles()) {
                if (!profile.source.startsWith("modrinth:")) {
                    continue;
                }
                list.add(packRow(profile));
                list.add(Box.createVerticalStrut(5));
            }
            if (list.getComponentCount() == 0) {
                list.add(empty(Lang.t("lib.empty")));
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SURFACE_LOW);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        if (actions.getComponentCount() > 0) {
            card.add(actions, BorderLayout.SOUTH);
        }
        return card;
    }

    private JComponent packRow(Profile profile) {
        RoundPanel row = new RoundPanel(Theme.SURFACE, Theme.OUTLINE_SOFT, 8);
        row.setLayout(new BorderLayout(6, 0));
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(profile.name, Theme.display(15f), Theme.TEXT));
        text.add(Theme.label(profile.describe(), Theme.mono(10f), Theme.OUTLINE));
        FlatButton play = new FlatButton("", FlatButton.Kind.ICON).icon("play");
        play.addActionListener(event -> shell.launch(profile));
        row.add(text, BorderLayout.CENTER);
        row.add(play, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return row;
    }

    private JComponent modRow(Path jar) {
        RoundPanel row = new RoundPanel(Theme.SURFACE, Theme.OUTLINE_SOFT, 8);
        row.setLayout(new BorderLayout(6, 0));
        row.setBorder(new EmptyBorder(7, 10, 7, 10));
        String name = jar.getFileName().toString().replace(".jar", "");
        if (name.length() > 26) {
            name = name.substring(0, 25) + "…";
        }
        row.add(Theme.label(name, Theme.body(11.5f), Theme.TEXT), BorderLayout.CENTER);
        FlatButton remove = new FlatButton("", FlatButton.Kind.ICON).icon("trash");
        remove.addActionListener(event -> {
            try {
                Modrinth.remove(jar);
                shell.log("Silindi: " + jar.getFileName());
                rebuildBody();
            } catch (IOException e) {
                shell.log("Silinemedi: " + e.getMessage());
            }
        });
        row.add(remove, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return row;
    }

    // ------------------------------------------------------------- results

    private void renderPacks(List<Modpacks.Hit> hits) {
        results.removeAll();
        if (hits.isEmpty()) {
            results.add(empty(Lang.t("lib.empty")));
        }
        for (Modpacks.Hit hit : hits) {
            results.add(packResult(hit));
            results.add(Box.createVerticalStrut(6));
        }
        results.revalidate();
        results.repaint();
    }

    private JComponent packResult(Modpacks.Hit hit) {
        RoundPanel row = Theme.card(new BorderLayout(10, 0), 12);
        row.setBackground(Theme.SURFACE);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(hit.title(), Theme.display(17f), Theme.TEXT));
        text.add(Theme.label(shorten(hit.description(), 90), Theme.body(11.5f), Theme.TEXT_MUTED));
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        meta.setOpaque(false);
        meta.add(Chip.muted(hit.author()));
        meta.add(Chip.muted(hit.downloads() + " " + Lang.t("lib.downloads")));
        text.add(meta);

        FlatButton install = new FlatButton(Lang.t("lib.install"), FlatButton.Kind.GHOST).icon("download");
        install.addActionListener(event -> installPack(hit, install));
        row.add(Images.tile(hit.title(), 44), BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(install, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void installPack(Modpacks.Hit hit, FlatButton button) {
        button.setEnabled(false);
        button.setText("…");
        shell.status(Lang.t("status.install"), true);
        Thread worker = new Thread(() -> {
            try {
                Profile profile = Modpacks.install(hit.slug(), new Progress() {
                    @Override
                    public void log(String message) {
                        shell.log(message);
                    }
                });
                state.add(profile);
                SwingUtilities.invokeLater(() -> {
                    button.setText(Lang.t("lib.installed.tag"));
                    shell.status(Lang.t("header.ready"), false);
                    shell.refreshAll();
                });
            } catch (IOException e) {
                shell.log("Paket kurulumu başarısız: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    button.setText(Lang.t("lib.install"));
                    button.setEnabled(true);
                });
            }
        }, "freemc-pack-install");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderMods(List<Modrinth.Hit> hits) {
        results.removeAll();
        if (hits.isEmpty()) {
            results.add(empty(Lang.t("lib.empty")));
        }
        for (Modrinth.Hit hit : hits) {
            results.add(modResult(hit));
            results.add(Box.createVerticalStrut(6));
        }
        results.revalidate();
        results.repaint();
    }

    private JComponent modResult(Modrinth.Hit hit) {
        RoundPanel row = Theme.card(new BorderLayout(10, 0), 12);
        row.setBackground(Theme.SURFACE);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(hit.title(), Theme.display(17f), Theme.TEXT));
        text.add(Theme.label(shorten(hit.description(), 96), Theme.body(11.5f), Theme.TEXT_MUTED));
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        meta.setOpaque(false);
        meta.add(Chip.muted(hit.author()));
        meta.add(Chip.muted(hit.downloads() + " " + Lang.t("lib.downloads")));
        text.add(meta);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        FlatButton info = new FlatButton("", FlatButton.Kind.ICON).icon("back");
        info.setToolTipText(Lang.t("lib.detail.about"));
        info.addActionListener(event -> openDetail(hit));
        FlatButton install = new FlatButton(Lang.t("lib.install"), FlatButton.Kind.GHOST).icon("download");
        Profile active = state.active();
        install.setEnabled(active != null && active.modded());
        install.addActionListener(event -> installMod(hit, install));
        actions.add(info);
        actions.add(install);

        row.add(Images.tile(hit.title(), 44), BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void installMod(Modrinth.Hit hit, FlatButton button) {
        Profile active = state.active();
        if (active == null || !active.modded()) {
            shell.log(Lang.t("lib.need.modded"));
            return;
        }
        button.setEnabled(false);
        button.setText("…");
        Thread worker = new Thread(() -> {
            try {
                List<String> written = Modrinth.install(hit.slug(), active.modsDir(), active.mcVersion,
                        active.loader, new Progress() {
                            @Override
                            public void log(String message) {
                                shell.log(message);
                            }
                        });
                SwingUtilities.invokeLater(() -> {
                    button.setText(Lang.t("lib.installed.tag"));
                    rebuildBody();
                });
                if (written.isEmpty()) {
                    shell.log(hit.title() + ": yeni dosya yok");
                }
            } catch (IOException e) {
                shell.log("Kurulum hatası: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    button.setText(Lang.t("lib.install"));
                    button.setEnabled(true);
                });
            }
        }, "freemc-mod-install");
        worker.setDaemon(true);
        worker.start();
    }

    private void openDetail(Modrinth.Hit hit) {
        Profile active = state.active();
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);

        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        FlatButton back = new FlatButton(Lang.t("lib.back"), FlatButton.Kind.GHOST).icon("back");
        back.addActionListener(event -> {
            detail = null;
            rebuildBody();
        });
        head.add(back, BorderLayout.WEST);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(Theme.label(hit.title(), Theme.display(26f), Theme.TEXT));
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        meta.setOpaque(false);
        meta.add(Chip.muted(hit.author()));
        meta.add(Chip.muted(hit.downloads() + " " + Lang.t("lib.downloads")));
        if (active != null) {
            meta.add(Chip.mint(active.describe()));
        }
        titleBox.add(meta);

        JPanel headInfo = new JPanel(new BorderLayout(12, 0));
        headInfo.setOpaque(false);
        headInfo.add(Images.tile(hit.title(), 56), BorderLayout.WEST);
        headInfo.add(titleBox, BorderLayout.CENTER);
        head.add(headInfo, BorderLayout.CENTER);

        FlatButton install = new FlatButton(Lang.t("lib.detail.add"), FlatButton.Kind.PRIMARY).icon("download");
        install.addActionListener(event -> installMod(hit, install));
        head.add(install, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(Theme.heading(Lang.t("lib.detail.about")));
        content.add(Box.createVerticalStrut(4));
        JLabel description = Theme.label("<html><div style='width:760px'>" + escape(hit.description())
                + "</div></html>", Theme.body(12.5f), Theme.TEXT_MUTED);
        content.add(description);
        content.add(Box.createVerticalStrut(14));
        content.add(Theme.heading(Lang.t("lib.detail.versions")));
        content.add(Box.createVerticalStrut(6));
        JPanel versions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        versions.setOpaque(false);
        versions.add(Theme.label("yükleniyor…", Theme.mono(10f), Theme.OUTLINE));
        content.add(versions);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        page.add(scroll, BorderLayout.CENTER);

        detail = page;
        rebuildBody();

        Thread worker = new Thread(() -> {
            try {
                Modrinth.Project project = Modrinth.project(hit.slug());
                List<String> numbers = Modrinth.versionNumbers(hit.slug(),
                        active == null ? "1.20.1" : active.mcVersion,
                        active == null ? "fabric" : active.loader, 8);
                SwingUtilities.invokeLater(() -> {
                    description.setText("<html><div style='width:760px'>"
                            + escape(project.description().isBlank() ? project.body() : project.description())
                            + "</div></html>");
                    versions.removeAll();
                    for (String number : numbers) {
                        versions.add(Chip.muted(number));
                    }
                    if (numbers.isEmpty()) {
                        versions.add(Theme.label("uygun sürüm yok", Theme.mono(10f), Theme.OUTLINE));
                    }
                    versions.revalidate();
                });
            } catch (IOException e) {
                shell.log("Mod ayrıntısı alınamadı: " + e.getMessage());
            }
        }, "freemc-mod-detail");
        worker.setDaemon(true);
        worker.start();
    }

    private void addLocalMod() {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Mod jar (*.jar)", "jar"));
        if (chooser.showOpenDialog(view) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path target = Modrinth.addLocal(chooser.getSelectedFile().toPath(), active.modsDir());
            shell.log("Eklendi: " + target.getFileName());
            rebuildBody();
        } catch (IOException e) {
            shell.log("Eklenemedi: " + e.getMessage());
        }
    }

    private void checkUpdates(FlatButton button) {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        button.setEnabled(false);
        Thread worker = new Thread(() -> {
            try {
                List<ModUpdates.Update> updates = ModUpdates.check(active, new Progress() {
                    @Override
                    public void log(String message) {
                        shell.log(message);
                    }
                });
                if (updates.isEmpty()) {
                    shell.log("Tüm modlar güncel.");
                } else {
                    for (ModUpdates.Update update : updates) {
                        shell.log("Güncelleme: " + update.localFile() + " → " + update.versionNumber());
                    }
                    ModUpdates.apply(updates, active, new Progress() {
                        @Override
                        public void log(String message) {
                            shell.log(message);
                        }
                    });
                    shell.log(updates.size() + " mod güncellendi");
                }
                SwingUtilities.invokeLater(() -> {
                    button.setEnabled(true);
                    rebuildBody();
                });
            } catch (IOException e) {
                shell.log("Güncelleme denetimi başarısız: " + e.getMessage());
                SwingUtilities.invokeLater(() -> button.setEnabled(true));
            }
        }, "freemc-mod-updates");
        worker.setDaemon(true);
        worker.start();
    }

    private JComponent empty(String text) {
        JLabel label = Theme.label(text, Theme.body(12f), Theme.OUTLINE);
        label.setBorder(new EmptyBorder(10, 4, 10, 4));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String shorten(String text, int limit) {
        if (text == null) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit - 1) + "…";
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
