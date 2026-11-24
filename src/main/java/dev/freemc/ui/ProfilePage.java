package dev.freemc.ui;

import dev.freemc.Lang;
import dev.freemc.auth.ElyBy;
import dev.freemc.auth.OfflineAuth;
import dev.freemc.game.Profile;
import dev.freemc.game.Skin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Identity, Ely.by account and the skin that comes with it. */
public final class ProfilePage implements Page {

    private final AppState state;
    private final Shell shell;
    private final JPanel view = new JPanel(new BorderLayout(0, 14));
    private final JPanel identitySlot = new JPanel(new BorderLayout());
    private final JPanel elySlot = new JPanel(new BorderLayout());
    private final JPanel sideSlot = new JPanel(new BorderLayout());
    private final JPanel recentSlot = new JPanel(new BorderLayout(0, 8));

    ProfilePage(AppState state, Shell shell) {
        this.state = state;
        this.shell = shell;
        view.setBackground(Theme.BG);
        view.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel middle = new JPanel(new BorderLayout(14, 0));
        middle.setOpaque(false);
        middle.add(elySlot, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 10));
        side.add(sideSlot, BorderLayout.CENTER);
        middle.add(side, BorderLayout.EAST);

        view.add(identitySlot, BorderLayout.NORTH);
        view.add(middle, BorderLayout.CENTER);
        view.add(recentSlot, BorderLayout.SOUTH);
        refresh();
    }

    @Override
    public String key() {
        return "profile";
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
        identitySlot.removeAll();
        identitySlot.add(identityCard(), BorderLayout.CENTER);
        elySlot.removeAll();
        elySlot.add(elyCard(), BorderLayout.CENTER);
        sideSlot.removeAll();
        sideSlot.add(statsCard(), BorderLayout.CENTER);
        recentSlot.removeAll();
        recentSlot.add(recentCard(), BorderLayout.CENTER);
        view.revalidate();
        view.repaint();
    }

    private JComponent identityCard() {
        TexturePanel card = new TexturePanel(12, "block/deepslate_tiles.png", "block/stone.png");
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        Profile active = state.active();
        BufferedImage skinImage = Skin.image(active);
        BufferedImage capeImage = Skin.cape(active);
        JPanel preview = new JPanel(new BorderLayout());
        preview.setOpaque(false);
        preview.setPreferredSize(new Dimension(120, 168));
        preview.add(new BodyPreview(skinImage, capeImage, 5), BorderLayout.CENTER);
        card.add(preview, BorderLayout.WEST);

        String username = state.username();
        OfflineAuth auth = OfflineAuth.of(username);
        ElyBy.Account ely = ElyBy.stored(active);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel caption = Theme.heading(Lang.t("prof.title"));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(caption);
        text.add(Box.createVerticalStrut(4));

        JLabel name = Theme.label((ely != null ? ely.name() : username).toUpperCase(), Theme.display(32f), Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(name);
        text.add(Box.createVerticalStrut(6));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.add(ely != null ? Chip.mint("Ely.by") : Chip.muted("offline"));
        if (ely != null) {
            chips.add(Chip.muted("UUID " + ely.uuid().substring(0, 8) + "…"));
        } else {
            chips.add(Chip.muted("UUID " + auth.uuid().substring(0, 8) + "…"));
        }
        if (Skin.exists(active)) {
            chips.add(Chip.muted("skin"));
        }
        if (Skin.hasCape(active)) {
            chips.add(Chip.muted("pelerin"));
        }
        text.add(chips);
        text.add(Box.createVerticalStrut(14));

        JPanel form = new JPanel(new BorderLayout(8, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField field = Theme.field(16);
        field.setText(username);
        JPanel labelBox = new JPanel();
        labelBox.setOpaque(false);
        labelBox.setLayout(new BoxLayout(labelBox, BoxLayout.Y_AXIS));
        labelBox.add(Theme.label(ely == null ? Lang.t("prof.name") : Lang.t("prof.name") + " (offline)",
                Theme.body(11f), Theme.OUTLINE));
        labelBox.add(Box.createVerticalStrut(4));
        labelBox.add(field);
        labelBox.setMaximumSize(new Dimension(320, 60));
        form.add(labelBox, BorderLayout.WEST);

        FlatButton save = new FlatButton(Lang.t("prof.save"), FlatButton.Kind.PRIMARY);
        save.addActionListener(event -> {
            state.username(OfflineAuth.sanitize(field.getText()));
            shell.log("Kullanıcı adı: " + state.username());
            refresh();
        });
        JPanel saveBox = new JPanel(new BorderLayout());
        saveBox.setOpaque(false);
        saveBox.add(save, BorderLayout.SOUTH);
        form.add(saveBox, BorderLayout.EAST);
        text.add(form);

        text.add(Box.createVerticalStrut(10));
        JLabel uuid = Theme.label("UUID: " + (ely != null ? ely.uuid() : auth.uuid()), Theme.mono(10f),
                Theme.OUTLINE);
        uuid.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(uuid);

        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JComponent elyCard() {
        Profile active = state.active();
        ElyBy.Account account = ElyBy.stored(active);

        RoundPanel card = Theme.card(new BorderLayout(0, 10), 18);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.heading("ELY.BY"), BorderLayout.WEST);
        header.add(account != null ? Chip.mint(Lang.t("prof.ely.logged")) : Chip.muted("ely.by"), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel hint = Theme.label(account == null ? Lang.t("prof.ely.hint") : Lang.t("prof.ely.logged.as")
                + " " + account.name(), Theme.body(11.5f), Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalStrut(10));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nick = Theme.field(12);
        nick.setToolTipText(Lang.t("prof.ely.nick"));
        if (account != null) {
            nick.setText(account.name());
        }
        JPasswordField pass = new JPasswordField(12);
        Theme.styleInput(pass);
        pass.setEchoChar('•');

        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 0));
        fields.setOpaque(false);
        fields.add(nick);
        if (account == null) {
            fields.add(pass);
        }
        row.add(fields, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (account == null) {
            FlatButton login = new FlatButton(Lang.t("prof.ely.login"), FlatButton.Kind.PRIMARY);
            login.addActionListener(event -> {
                String user = nick.getText().trim();
                String password = new String(pass.getPassword());
                if (user.isEmpty() || password.isEmpty()) {
                    shell.log("Ely.by: takma ad ve şifre gerekli");
                    return;
                }
                loginEly(user, password);
            });
            buttons.add(login);
        } else {
            FlatButton logout = new FlatButton(Lang.t("prof.ely.logout"), FlatButton.Kind.GHOST);
            logout.addActionListener(event -> {
                try {
                    ElyBy.clear(active);
                    shell.log("Ely.by oturumu kapatıldı");
                    refresh();
                } catch (IOException e) {
                    shell.log("Oturum kapatılamadı: " + e.getMessage());
                }
            });
            buttons.add(logout);
        }

        FlatButton fetch = new FlatButton(Lang.t("prof.ely.fetch"), FlatButton.Kind.GHOST).icon("download");
        fetch.addActionListener(event -> fetchSkin(nick.getText().trim(), false));
        buttons.add(fetch);

        FlatButton apply = new FlatButton(Lang.t("prof.ely.apply"), FlatButton.Kind.GHOST).icon("check");
        apply.setEnabled(active != null && active.modded() && Skin.exists(active));
        apply.addActionListener(event -> applySkin());
        buttons.add(apply);

        body.add(row);
        body.add(buttons);
        body.add(Box.createVerticalStrut(6));

        JLabel note = Theme.label(active != null && active.modded() ? Lang.t("set.skin.note")
                : Lang.t("set.skin.vanilla"), Theme.body(10.5f), Theme.OUTLINE);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(note);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void loginEly(String user, String password) {
        shell.status(Lang.t("prof.ely.login") + "…", true);
        Thread worker = new Thread(() -> {
            try {
                ElyBy.Account account = ElyBy.login(user, password);
                ElyBy.store(state.active(), account);
                shell.log("Ely.by girişi başarılı: " + account.name() + " (" + account.uuid() + ")");
                state.username(account.name());
                shell.status(Lang.t("header.ready"), false);
                javax.swing.SwingUtilities.invokeLater(this::refresh);
                fetchSkin(account.name(), true);
            } catch (IOException e) {
                shell.log("Ely.by girişi başarısız: " + e.getMessage());
                shell.status(Lang.t("status.error"), false);
            }
        }, "freemc-elyby-login");
        worker.setDaemon(true);
        worker.start();
    }

    private void fetchSkin(String nickname, boolean quiet) {
        Profile active = state.active();
        if (active == null || nickname.isBlank()) {
            if (!quiet) {
                shell.log("Ely.by: takma ad gir");
            }
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                ElyBy.Textures textures = ElyBy.textures(nickname);
                if (textures.empty()) {
                    shell.log("Ely.by: " + nickname + " için kostüm bulunamadı");
                    return;
                }
                Path dir = Files.createTempDirectory("freemc-elyby");
                if (textures.skinUrl() != null) {
                    Path skin = ElyBy.download(textures.skinUrl(), dir.resolve("skin.png"));
                    Skin.save(active, skin);
                    shell.log("Kostüm alındı: " + nickname);
                }
                if (textures.capeUrl() != null) {
                    Path cape = ElyBy.download(textures.capeUrl(), dir.resolve("cape.png"));
                    Skin.saveCape(active, cape);
                    shell.log("Pelerin alındı: " + nickname);
                }
                javax.swing.SwingUtilities.invokeLater(this::refresh);
            } catch (IOException e) {
                shell.log("Ely.by kostümü alınamadı: " + e.getMessage());
            }
        }, "freemc-elyby-skin");
        worker.setDaemon(true);
        worker.start();
    }

    private void applySkin() {
        Profile active = state.active();
        if (active == null) {
            return;
        }
        shell.status(Lang.t("set.skin.apply"), true);
        Thread worker = new Thread(() -> {
            try {
                Skin.apply(active, state.username(), new dev.freemc.game.Progress() {
                    @Override
                    public void log(String message) {
                        shell.log(message);
                    }
                });
                shell.status(Lang.t("header.ready"), false);
            } catch (IOException e) {
                shell.log("Kostüm uygulanamadı: " + e.getMessage());
                shell.status(Lang.t("status.error"), false);
            }
        }, "freemc-skin-apply");
        worker.setDaemon(true);
        worker.start();
    }

    private JComponent statsCard() {
        RoundPanel card = Theme.card(new BorderLayout(0, 10), 16);
        card.add(Theme.heading(Lang.t("prof.stats")), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(3, 1, 0, 8));
        grid.setOpaque(false);
        grid.add(stat(String.format("%.1f", state.totalPlaySeconds() / 3600.0), Lang.t("prof.hours")));
        grid.add(stat(String.valueOf(state.profiles().size()), Lang.t("dash.profiles")));
        grid.add(stat(String.valueOf(state.totalMods()), Lang.t("prof.mods")));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent stat(String value, String label) {
        RoundPanel tile = new RoundPanel(Theme.SURFACE, null, 8);
        tile.setLayout(new BorderLayout());
        tile.setBorder(new EmptyBorder(10, 14, 10, 14));
        tile.add(Theme.label(value, Theme.display(26f), Theme.ACCENT), BorderLayout.WEST);
        JLabel caption = Theme.label(label, Theme.body(11f), Theme.OUTLINE);
        caption.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tile.add(caption, BorderLayout.EAST);
        return tile;
    }

    private JComponent recentCard() {
        RoundPanel card = Theme.card(new BorderLayout(0, 8), 16);
        card.add(Theme.heading(Lang.t("prof.recent")), BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        int shown = 0;
        for (Profile profile : state.profiles()) {
            if (shown++ >= 3) {
                break;
            }
            RoundPanel row = new RoundPanel(Theme.SURFACE, Theme.OUTLINE_SOFT, 8);
            row.setLayout(new BorderLayout(8, 0));
            row.setBorder(new EmptyBorder(8, 12, 8, 12));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(Theme.label(profile.name, Theme.display(15f), Theme.TEXT));
            text.add(Theme.label(profile.describe() + " · " + profile.modCount() + " " + Lang.t("dash.mods"),
                    Theme.mono(10f), Theme.OUTLINE));
            row.add(text, BorderLayout.CENTER);
            FlatButton play = new FlatButton("", FlatButton.Kind.ICON).icon("play");
            play.addActionListener(event -> shell.launch(profile));
            row.add(play, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            list.add(row);
            list.add(Box.createVerticalStrut(6));
        }
        if (list.getComponentCount() == 0) {
            list.add(Theme.label(Lang.t("dash.welcome"), Theme.body(12f), Theme.OUTLINE));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    /** Front view of the skin with the cape behind it, or the offline initial block. */
    private static final class BodyPreview extends JComponent {
        private final BufferedImage skin;
        private final BufferedImage cape;
        private final int scale;

        BodyPreview(BufferedImage skin, BufferedImage cape, int scale) {
            this.skin = skin;
            this.cape = cape;
            this.scale = scale;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int width = getWidth();
            int height = getHeight();
            g.setColor(Theme.SURFACE_TOP);
            g.fillRoundRect(0, 0, width, height, 10, 10);

            BufferedImage body = Skin.body(skin, scale);
            int drawWidth = body == null ? 0 : body.getWidth();
            int drawHeight = body == null ? 0 : body.getHeight();
            if (body != null) {
                int originX = (width - drawWidth) / 2;
                int originY = (height - drawHeight) / 2;
                if (cape != null && cape.getWidth() >= 10 && cape.getHeight() >= 16) {
                    g.drawImage(cape, originX + drawWidth / 8, originY + drawHeight / 5,
                            originX + drawWidth * 7 / 8, originY + drawHeight, 1, 1, 9, 16, null);
                }
                g.drawImage(body, originX, originY, null);
            } else {
                g.setColor(Theme.OUTLINE_SOFT);
                g.setFont(Theme.display(12f));
                String text = Lang.t("set.skin.empty");
                int textWidth = g.getFontMetrics().stringWidth(text);
                g.drawString(text, Math.max(6, (width - textWidth) / 2), height / 2);
            }
            g.dispose();
        }
    }
}
