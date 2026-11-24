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
        list.add(sectionProfile());
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
