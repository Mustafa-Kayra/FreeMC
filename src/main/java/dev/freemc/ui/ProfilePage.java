package dev.freemc.ui;

import dev.freemc.Lang;
import dev.freemc.auth.OfflineAuth;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

/** Offline identity and play statistics. */
public final class ProfilePage implements Page {

    private final AppState state;
    private final Shell shell;
    private final JPanel view = new JPanel(new BorderLayout(14, 0));
    private final JPanel left = new JPanel(new BorderLayout());
    private final JPanel right = new JPanel(new BorderLayout(0, 14));

    ProfilePage(AppState state, Shell shell) {
        this.state = state;
        this.shell = shell;
        view.setBackground(Theme.BG);
        view.setBorder(new EmptyBorder(16, 22, 16, 22));
        view.add(left, BorderLayout.CENTER);
        view.add(right, BorderLayout.EAST);
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
        left.removeAll();
        left.add(identityCard(), BorderLayout.NORTH);
        left.add(Box.createVerticalStrut(0), BorderLayout.CENTER);

        right.removeAll();
        right.add(statsCard(), BorderLayout.NORTH);
        right.setPreferredSize(new Dimension(330, 10));
        view.revalidate();
        view.repaint();
    }

    private JComponent identityCard() {
        RoundPanel card = Theme.card(new BorderLayout(18, 0), 22);
        String username = state.username();
        OfflineAuth auth = OfflineAuth.of(username);

        JPanel avatarBox = new JPanel(new BorderLayout());
        avatarBox.setOpaque(false);
        avatarBox.setPreferredSize(new Dimension(110, 110));
        java.awt.image.BufferedImage head = dev.freemc.game.Skin.head(
                dev.freemc.game.Skin.image(state.active()), 8);
        avatarBox.add(new Avatar(username, head), BorderLayout.CENTER);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel caption = Theme.heading(Lang.t("prof.title"));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(caption);
        text.add(Box.createVerticalStrut(4));
        JLabel name = Theme.label(username.toUpperCase(), Theme.display(32f), Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(name);
        text.add(Box.createVerticalStrut(6));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.add(Chip.mint("offline"));
        chips.add(Chip.muted("UUID " + auth.uuid().substring(0, 8) + "…"));
        text.add(chips);
        text.add(Box.createVerticalStrut(14));

        JPanel form = new JPanel(new BorderLayout(8, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField field = Theme.field(18);
        field.setText(username);
        JLabel fieldLabel = Theme.label(Lang.t("prof.name"), Theme.body(11f), Theme.OUTLINE);
        JPanel labelBox = new JPanel();
        labelBox.setOpaque(false);
        labelBox.setLayout(new BoxLayout(labelBox, BoxLayout.Y_AXIS));
        labelBox.add(fieldLabel);
        labelBox.add(Box.createVerticalStrut(4));
        labelBox.add(field);
        form.add(labelBox, BorderLayout.CENTER);

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

        text.add(Box.createVerticalStrut(12));
        JLabel uuid = Theme.label("UUID: " + auth.uuid(), Theme.mono(10f), Theme.OUTLINE);
        uuid.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(uuid);

        card.add(avatarBox, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
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

    /** Draws a blocky avatar from the first letter, so no skin fetch is needed offline. */
    private static final class Avatar extends JComponent {
        private final String name;
        private final java.awt.image.BufferedImage head;

        Avatar(String name, java.awt.image.BufferedImage head) {
            this.name = name;
            this.head = head;
            setPreferredSize(new Dimension(110, 110));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            g.setColor(Theme.SURFACE_TOP);
            g.fillRoundRect(0, 0, size, size, 16, 16);
            if (head != null) {
                g.drawImage(head, size / 10, size / 10, size - size / 10, size - size / 10, null);
            } else {
                g.setColor(Theme.ACCENT);
                g.fillRoundRect(size / 8, size / 8, size * 3 / 4, size * 3 / 4, 12, 12);
                g.setColor(Theme.ACCENT_INK);
                g.setFont(Theme.display(36f));
                String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                int width = g.getFontMetrics().stringWidth(initial);
                g.drawString(initial, (size - width) / 2, size / 2 + 14);
            }
            g.dispose();
        }
    }
}
