package dev.freemc.ui;

import dev.freemc.Dirs;
import dev.freemc.Jvm;
import dev.freemc.Lang;
import dev.freemc.game.Profile;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import java.nio.file.Files;
import java.nio.file.Path;

/** Global and per-profile settings: language, theme, memory, JVM flags and the data folder. */
public final class SettingsPage implements Page {

    private final AppState state;
    private final Shell shell;
    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JPanel list = new JPanel();

    SettingsPage(AppState state, Shell shell) {
        this.state = state;
        this.shell = shell;
        view.setBackground(Theme.BG);
        view.setBorder(new EmptyBorder(16, 22, 16, 22));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        view.add(list, BorderLayout.NORTH);
    }

    @Override
    public String key() {
        return "settings";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        build();
    }

    private void build() {
        list.removeAll();
        list.add(sectionLanguage());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionTheme());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionMemory());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionSkin());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionProfile());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionJava());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionCache());
        list.add(Box.createVerticalStrut(12));
        list.add(sectionAbout());
        list.revalidate();
        list.repaint();
    }

    private JComponent frame(String title, JComponent content) {
        RoundPanel card = Theme.card(new BorderLayout(12, 8), 18);
        card.add(Theme.heading(title), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JComponent segmented(boolean first, String firstText, String secondText, Runnable onFirst,
                                 Runnable onSecond) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        FlatButton left = new FlatButton(firstText, FlatButton.Kind.TAB).active(first);
        FlatButton right = new FlatButton(secondText, FlatButton.Kind.TAB).active(!first);
        left.addActionListener(event -> onFirst.run());
        right.addActionListener(event -> onSecond.run());
        row.add(left);
        row.add(right);
        return row;
    }

    private JComponent sectionLanguage() {
        boolean turkish = !Lang.isEnglish();
        return frame(Lang.t("set.language"), segmented(turkish, "Türkçe", "English", () -> {
            if (!turkish) {
                applyLanguage("tr");
            }
        }, () -> {
            if (turkish) {
                applyLanguage("en");
            }
        }));
    }

    private void applyLanguage(String code) {
        state.config.language(code).save();
        shell.restartUi();
    }

    private JComponent sectionTheme() {
        boolean dark = Theme.isDark();
        return frame(Lang.t("set.theme"), segmented(dark, Lang.t("set.dark"), Lang.t("set.light"),
                () -> {
                    if (!dark) {
                        applyTheme("dark");
                    }
                }, () -> {
                    if (dark) {
                        applyTheme("light");
                    }
                }));
    }

    private void applyTheme(String theme) {
        state.config.theme(theme).save();
        shell.restartUi();
    }

    private JComponent sectionMemory() {
        Profile active = state.active();
        int current = active == null ? 4096 : active.ramMb;

        JPanel box = new JPanel(new BorderLayout(12, 6));
        box.setOpaque(false);

        JSlider slider = new JSlider(1024, 32768, Math.min(32768, Math.max(1024, current)));
        slider.setBackground(Theme.SURFACE_LOW);
        slider.setForeground(Theme.ACCENT);
        slider.setMajorTickSpacing(8192);
        slider.setPaintTicks(false);
        JLabel value = Theme.label(current + " MB", Theme.display(20f), Theme.ACCENT);

        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        presets.setOpaque(false);
        for (int mb : new int[]{2048, 4096, 8192, 16384}) {
            FlatButton preset = new FlatButton(mb / 1024 + " GB", FlatButton.Kind.GHOST);
            preset.addActionListener(event -> {
                slider.setValue(mb);
                value.setText(mb + " MB");
                applyMemory(mb);
            });
            presets.add(preset);
        }

        slider.addChangeListener(event -> {
            int mb = slider.getValue();
            value.setText(mb + " MB");
            if (!slider.getValueIsAdjusting()) {
                applyMemory(mb);
            }
        });

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label(Lang.t("set.memory.hint"), Theme.body(11.5f), Theme.TEXT_MUTED), BorderLayout.WEST);
        head.add(value, BorderLayout.EAST);

        box.add(head, BorderLayout.NORTH);
        box.add(slider, BorderLayout.CENTER);
        box.add(presets, BorderLayout.SOUTH);
        JComponent card = frame(Lang.t("set.memory"), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        return card;
    }

    private void applyMemory(int mb) {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        active.ramMb = mb;
        active.save();
        shell.log("Bellek güncellendi: " + mb + " MB (" + active.name + ")");
    }

    private JComponent sectionProfile() {
        Profile active = state.active();
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JTextField jvm = Theme.field(30);
        JTextField server = Theme.field(30);
        if (active != null) {
            jvm.setText(active.jvmArgs);
            server.setText(active.quickServer);
        }
        jvm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        server.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel jvmLabel = Theme.label(Lang.t("set.jvm"), Theme.body(11f), Theme.OUTLINE);
        jvmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(jvmLabel);
        box.add(Box.createVerticalStrut(4));
        box.add(jvm);
        box.add(Box.createVerticalStrut(10));
        JLabel serverLabel = Theme.label(Lang.t("set.server") + " — " + Lang.t("set.server.hint"),
                Theme.body(11f), Theme.OUTLINE);
        serverLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(serverLabel);
        box.add(Box.createVerticalStrut(4));
        box.add(server);

        FlatButton save = new FlatButton(Lang.t("set.save"), FlatButton.Kind.PRIMARY);
        save.addActionListener(event -> {
            if (active == null) {
                return;
            }
            active.jvmArgs = jvm.getText().trim();
            active.quickServer = server.getText().trim();
            active.save();
            shell.log(Lang.t("set.saved") + ": " + active.name);
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(save);
        box.add(Box.createVerticalStrut(10));
        box.add(actions);

        JComponent card = frame(Lang.t("nav.profile") + " · " + (active == null ? "—" : active.name), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        return card;
    }


    private JComponent sectionSkin() {
        Profile active = state.active();
        JPanel box = new JPanel(new BorderLayout(16, 0));
        box.setOpaque(false);

        java.awt.image.BufferedImage head = dev.freemc.game.Skin.head(
                dev.freemc.game.Skin.display(active), 8);
        JPanel previewBox = new JPanel(new BorderLayout());
        previewBox.setOpaque(false);
        previewBox.setPreferredSize(new Dimension(84, 84));
        previewBox.add(new HeadView(head), BorderLayout.CENTER);
        box.add(previewBox, BorderLayout.WEST);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

        JLabel note = Theme.label(active == null ? Lang.t("lib.need.profile")
                : active.modded() ? Lang.t("set.skin.note") : Lang.t("set.skin.vanilla"),
                Theme.body(11f), Theme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(note);
        actions.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        FlatButton pick = new FlatButton(Lang.t("set.skin.pick"), FlatButton.Kind.GHOST).icon("folder");
        pick.addActionListener(event -> pickSkin());
        buttons.add(pick);

        FlatButton apply = new FlatButton(Lang.t("set.skin.apply"), FlatButton.Kind.PRIMARY);
        apply.setEnabled(active != null && active.modded() && dev.freemc.game.Skin.exists(active));
        apply.addActionListener(event -> applySkin());
        buttons.add(apply);

        FlatButton clear = new FlatButton(Lang.t("set.skin.clear"), FlatButton.Kind.DANGER);
        clear.setEnabled(active != null && dev.freemc.game.Skin.exists(active));
        clear.addActionListener(event -> {
            try {
                dev.freemc.game.Skin.clear(active);
                shell.log("Kostüm kaldırıldı.");
                build();
            } catch (java.io.IOException e) {
                shell.log("Kostüm kaldırılamadı: " + e.getMessage());
            }
        });
        buttons.add(clear);

        actions.add(buttons);
        box.add(actions, BorderLayout.CENTER);

        JComponent card = frame(Lang.t("set.skin"), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        return card;
    }

    private void pickSkin() {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Skin PNG (*.png)", "png"));
        if (chooser.showOpenDialog(view) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            dev.freemc.game.Skin.save(active, chooser.getSelectedFile().toPath());
            shell.log("Skin kaydedildi: " + chooser.getSelectedFile().getName());
            build();
        } catch (java.io.IOException e) {
            shell.log("Skin yüklenemedi: " + e.getMessage());
        }
    }

    private void applySkin() {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        shell.status(Lang.t("set.skin.apply"), true);
        Thread worker = new Thread(() -> {
            try {
                dev.freemc.game.Skin.apply(active, state.username(), new dev.freemc.game.Progress() {
                    @Override
                    public void log(String message) {
                        shell.log(message);
                    }
                });
                shell.status(Lang.t("header.ready"), false);
            } catch (java.io.IOException e) {
                shell.log("Kostüm uygulanamadı: " + e.getMessage());
                shell.status(Lang.t("status.error"), false);
            }
        }, "freemc-skin");
        worker.setDaemon(true);
        worker.start();
    }

    private JComponent sectionJava() {
        JPanel box = new JPanel(new BorderLayout(10, 0));
        box.setOpaque(false);
        JLabel info = Theme.label("<html>" + Jvm.javaCommand()
                + "<br>" + Lang.t("set.java.auto") + "</html>", Theme.mono(10f), Theme.OUTLINE);
        box.add(info, BorderLayout.CENTER);
        FlatButton redownload = new FlatButton(Lang.t("set.java.redownload"), FlatButton.Kind.GHOST)
                .icon("download");
        redownload.addActionListener(event -> {
            try {
                deleteTree(Dirs.root().resolve("jvm"));
                shell.log("Java önbelleği silindi; sonraki başlatmada yeniden inecek.");
            } catch (java.io.IOException e) {
                shell.log("Silinemedi: " + e.getMessage());
            }
        });
        box.add(redownload, BorderLayout.EAST);
        JComponent card = frame(Lang.t("set.java"), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        return card;
    }

    private JComponent sectionCache() {
        JPanel box = new JPanel(new BorderLayout(10, 6));
        box.setOpaque(false);
        long assets = sizeMb(Dirs.root().resolve("assets"));
        long libraries = sizeMb(Dirs.root().resolve("libraries"));
        long versions = sizeMb(Dirs.root().resolve("versions"));
        long jvm = sizeMb(Dirs.root().resolve("jvm"));
        JLabel info = Theme.label("<html>assets: " + assets + " MB · libraries: " + libraries
                + " MB · versions: " + versions + " MB · jvm: " + jvm + " MB</html>",
                Theme.mono(10f), Theme.OUTLINE);
        box.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        for (String area : new String[]{"assets", "libraries"}) {
            FlatButton clear = new FlatButton(area + " · " + Lang.t("set.cache.clear"), FlatButton.Kind.GHOST);
            clear.addActionListener(event -> {
                try {
                    deleteTree(Dirs.root().resolve(area));
                    shell.log(area + " temizlendi; oyun bir sonraki açılışta yeniden indirecek.");
                    build();
                } catch (java.io.IOException e) {
                    shell.log("Temizlenemedi: " + e.getMessage());
                }
            });
            buttons.add(clear);
        }
        box.add(buttons, BorderLayout.EAST);

        JComponent card = frame(Lang.t("set.cache"), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return card;
    }

    private static long sizeMb(Path path) {
        if (!Files.exists(path)) {
            return 0;
        }
        try (var walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (java.io.IOException e) {
                    return 0;
                }
            }).sum() / (1024 * 1024);
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    private static void deleteTree(Path dir) throws java.io.IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Files.deleteIfExists(path);
            }
        }
    }

    /** Renders a skin head, or a neutral block when none is set. */
    private static final class HeadView extends JComponent {
        private final java.awt.image.BufferedImage head;

        HeadView(java.awt.image.BufferedImage head) {
            this.head = head;
            setPreferredSize(new Dimension(84, 84));
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            g.setColor(Theme.SURFACE_HIGH);
            g.fillRoundRect(0, 0, size, size, 12, 12);
            if (head != null) {
                int inner = size - 16;
                g.drawImage(head, 8, 8, 8 + inner, 8 + inner, null);
            } else {
                g.setColor(Theme.OUTLINE_SOFT);
                g.setFont(Theme.display(12f));
                String text = Lang.t("set.skin.empty");
                int width = g.getFontMetrics().stringWidth(text);
                g.drawString(text, Math.max(4, (size - width) / 2), size / 2);
            }
            g.dispose();
        }
    }

    private JComponent sectionAbout() {
        JPanel box = new JPanel(new BorderLayout(10, 0));
        box.setOpaque(false);
        JLabel info = Theme.label("<html>Java: " + Jvm.javaCommand()
                + "<br>" + Lang.t("set.data") + ": " + Dirs.root() + "</html>", Theme.mono(10f), Theme.OUTLINE);
        box.add(info, BorderLayout.CENTER);
        FlatButton open = new FlatButton(Lang.t("set.open"), FlatButton.Kind.GHOST).icon("folder");
        open.addActionListener(event -> shell.openDataFolder());
        box.add(open, BorderLayout.EAST);
        JComponent card = frame(Lang.t("set.java"), box);
        ((RoundPanel) card).setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        return card;
    }
}
