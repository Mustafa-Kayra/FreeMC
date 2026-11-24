package dev.freemc.ui;

import dev.freemc.Lang;
import dev.freemc.game.CrashReport;
import dev.freemc.game.Profile;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/** Landing page: the active profile, the launch button and the library at a glance. */
public final class DashboardPage implements Page {

    private final AppState state;
    private final Shell shell;
    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JPanel heroSlot = new JPanel(new BorderLayout());
    private final JPanel bannerSlot = new JPanel(new BorderLayout());
    private final JPanel recentSlot = new JPanel(new BorderLayout());
    private final JPanel statsSlot = new JPanel(new BorderLayout());

    DashboardPage(AppState state, Shell shell) {
        this.state = state;
        this.shell = shell;
        view.setBackground(Theme.BG);
        view.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel body = new JPanel(new BorderLayout(14, 14));
        body.setOpaque(false);
        body.add(heroSlot, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);
        middle.add(bannerSlot, BorderLayout.NORTH);
        JPanel columns = new JPanel(new BorderLayout(14, 0));
        columns.setOpaque(false);
        columns.add(recentSlot, BorderLayout.CENTER);
        columns.add(statsSlot, BorderLayout.EAST);
        middle.add(columns, BorderLayout.CENTER);
        body.add(middle, BorderLayout.CENTER);
        view.add(body, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public String key() {
        return "dashboard";
    }

    @Override
    public JComponent view() {
        return view;
    }

    @Override
    public void onShow() {
        refresh();
    }

    private void refresh() {
        heroSlot.removeAll();
        heroSlot.add(hero(), BorderLayout.CENTER);
        bannerSlot.removeAll();
        JComponent banner = banner();
        bannerSlot.setVisible(banner != null);
        if (banner != null) {
            bannerSlot.add(banner, BorderLayout.CENTER);
        }
        recentSlot.removeAll();
        recentSlot.add(recentCard(), BorderLayout.CENTER);
        statsSlot.removeAll();
        statsSlot.add(statsCard(), BorderLayout.CENTER);
        view.revalidate();
        view.repaint();
    }

    private JComponent hero() {
        Profile active = state.active();
        RoundPanel card = Theme.card(new BorderLayout(18, 0), 22);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel cap = Theme.heading(Lang.t("dash.active"));
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(cap);
        left.add(Box.createVerticalStrut(6));

        if (active == null) {
            JLabel empty = Theme.label(Lang.t("dash.welcome"), Theme.display(30f), Theme.TEXT);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            left.add(empty);
            left.add(Box.createVerticalStrut(8));
            left.add(subLine(Lang.t("dash.create")));
        } else {
            JLabel name = Theme.label(active.name.toUpperCase(), Theme.display(38f), Theme.TEXT);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            left.add(name);
            left.add(Box.createVerticalStrut(8));

            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            chips.setOpaque(false);
            chips.setAlignmentX(Component.LEFT_ALIGNMENT);
            chips.add(Chip.muted("Minecraft " + active.mcVersion));
            chips.add(Chip.muted(active.loaderLabel()));
            chips.add(Chip.muted(active.modCount() + " " + Lang.t("dash.mods")));
            chips.add(Chip.muted(active.ramMb + " MB"));
            left.add(chips);
            left.add(Box.createVerticalStrut(10));
            left.add(subLine(active.quickServer.isBlank()
                    ? "Offline · " + state.username()
                    : "Hızlı katılma: " + active.quickServer));
        }

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        FlatButton play = new FlatButton(Lang.t("dash.play"), FlatButton.Kind.PRIMARY).icon("play");
        play.setAlignmentX(Component.RIGHT_ALIGNMENT);
        play.addActionListener(event -> {
            if (state.active() == null) {
                shell.newProfile();
            } else {
                shell.launch(state.active());
            }
        });
        right.add(play);
        right.add(Box.createVerticalStrut(10));
        if (active != null) {
            String last = active.lastPlayed == 0 ? "—"
                    : new java.text.SimpleDateFormat("dd MMM HH:mm").format(new java.util.Date(active.lastPlayed));
            JLabel meta = Theme.label("Son oynama: " + last, Theme.mono(10f), Theme.OUTLINE);
            meta.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(meta);
        }

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JLabel subLine(String text) {
        JLabel label = Theme.label(text, Theme.body(12.5f), Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent banner() {
        Profile active = state.active();
        if (active == null) {
            return null;
        }
        CrashReport.Finding crash = CrashReport.latest(active.gameDir());
        if (crash == null) {
            return null;
        }
        RoundPanel card = Theme.card(new BorderLayout(14, 0), 16);
        card.setBackground(Theme.SURFACE_LOW);

        JPanel iconBox = new JPanel(new BorderLayout());
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(34, 34));
        iconBox.add(new Glyph("warning", Theme.DANGER), BorderLayout.CENTER);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.add(Theme.label(Lang.t("crash.title"), Theme.display(15f), Theme.TEXT));
        titleRow.add(Chip.danger("crash"));
        text.add(titleRow);
        text.add(Theme.label(crash.summary(), Theme.body(12f), Theme.TEXT_MUTED));
        if (!crash.frames().isEmpty()) {
            text.add(Theme.label(crash.frames().get(0), Theme.mono(10f), Theme.OUTLINE));
        }

        FlatButton open = new FlatButton(Lang.t("crash.detail"), FlatButton.Kind.GHOST);
        open.addActionListener(event -> {
            shell.toggleConsole();
            shell.log("Çökme raporu: " + crash.file());
            for (String frame : crash.frames()) {
                shell.log("  " + frame);
            }
        });

        card.add(iconBox, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(open, BorderLayout.EAST);
        return card;
    }

    private JComponent recentCard() {
        RoundPanel card = Theme.card(new BorderLayout(0, 10), 16);
        card.add(Theme.heading(Lang.t("dash.recent")), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<Profile> profiles = state.profiles();
        if (profiles.isEmpty()) {
            list.add(Theme.label(Lang.t("dash.welcome"), Theme.body(12f), Theme.OUTLINE));
        }
        int shown = 0;
        for (Profile profile : profiles) {
            if (shown++ >= 6) {
                break;
            }
            list.add(recentRow(profile));
            list.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SURFACE_LOW);
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        FlatButton create = new FlatButton(Lang.t("dash.new"), FlatButton.Kind.GHOST).icon("plus");
        create.addActionListener(event -> shell.newProfile());
        actions.add(create);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JComponent recentRow(Profile profile) {
        RoundPanel row = new RoundPanel(Theme.SURFACE, Theme.OUTLINE_SOFT, 8);
        row.setLayout(new BorderLayout(10, 0));
        row.setBorder(new EmptyBorder(9, 12, 9, 12));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.label(profile.name, Theme.display(16f), Theme.TEXT));
        text.add(Theme.label(profile.describe() + " · " + profile.modCount() + " " + Lang.t("dash.mods"),
                Theme.mono(10f), Theme.OUTLINE));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        boolean isActive = state.active() != null && state.active().id.equals(profile.id);
        if (!isActive) {
            FlatButton use = new FlatButton("Aktif", FlatButton.Kind.GHOST);
            use.addActionListener(event -> {
                state.setActive(profile);
                shell.refreshAll();
            });
            actions.add(use);
        } else {
            actions.add(Chip.mint("aktif"));
        }
        FlatButton play = new FlatButton("", FlatButton.Kind.ICON).icon("play");
        play.addActionListener(event -> shell.launch(profile));
        actions.add(play);

        row.add(text, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JComponent statsCard() {
        RoundPanel card = Theme.card(new BorderLayout(0, 12), 16);
        card.setPreferredSize(new Dimension(300, 10));
        card.add(Theme.heading(Lang.t("dash.stats")), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 1, 0, 10));
        grid.setOpaque(false);
        grid.add(stat(String.valueOf(state.profiles().size()), Lang.t("dash.profiles")));
        grid.add(stat(String.format("%.1f", state.totalPlaySeconds() / 3600.0), Lang.t("prof.hours")));
        grid.add(stat(String.valueOf(state.totalMods()), Lang.t("prof.mods")));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent stat(String value, String label) {
        RoundPanel tile = new RoundPanel(Theme.SURFACE, null, 8);
        tile.setLayout(new BorderLayout());
        tile.setBorder(new EmptyBorder(8, 12, 8, 12));
        tile.add(Theme.label(value, Theme.display(24f), Theme.ACCENT), BorderLayout.WEST);
        JLabel caption = Theme.label(label, Theme.body(11f), Theme.OUTLINE);
        caption.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tile.add(caption, BorderLayout.EAST);
        return tile;
    }

    /** Paints a single icon inside a fixed box. */
    private static final class Glyph extends JComponent {
        private final String name;
        private final java.awt.Color color;

        Glyph(String name, java.awt.Color color) {
            this.name = name;
            this.color = color;
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            Icons.draw((java.awt.Graphics2D) graphics, name, Math.min(getWidth(), getHeight()), color);
        }
    }
}
