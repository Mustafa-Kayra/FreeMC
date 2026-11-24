package dev.freemc.ui;

import dev.freemc.Config;
import dev.freemc.Dirs;
import dev.freemc.auth.OfflineAuth;
import dev.freemc.game.GameLauncher;
import dev.freemc.game.Instance;
import dev.freemc.game.Progress;
import dev.freemc.game.VersionManifest;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import javax.swing.Box;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.List;

/** Version and account selection, the play button and its progress. */
final class PlayPanel extends JPanel {

    private final Config config;
    private final Selection selection;
    private final Console console;

    private final JComboBox<VersionManifest.Entry> versionBox = Theme.combo();
    private final JComboBox<String> loaderBox = Theme.combo();
    private final JTextField usernameField = Theme.field(16);
    private final JSpinner ramSpinner = new JSpinner(new SpinnerNumberModel(2048, 512, 32768, 512));
    private final FlatButton playButton = new FlatButton("OYNA", FlatButton.Kind.PRIMARY);
    private final FlatButton folderButton = new FlatButton("Oyun klasörü", FlatButton.Kind.GHOST);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel status = Theme.label("Hazır", Theme.SMALL, Theme.MUTED);

    PlayPanel(Config config, Selection selection, Console console) {
        this.config = config;
        this.selection = selection;
        this.console = console;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);

        Theme.styleSpinner(ramSpinner);
        Theme.styleProgress(progress);
        usernameField.setText(config.username());
        ramSpinner.setValue(config.ramMb());

        loaderBox.setModel(new DefaultComboBoxModel<>(new String[]{"Vanilla", "Fabric"}));
        loaderBox.setSelectedIndex(Instance.FABRIC.equals(config.loader()) ? 1 : 0);

        add(settingsCard(), BorderLayout.NORTH);
        add(actionsCard(), BorderLayout.CENTER);
        add(Box.createVerticalGlue(), BorderLayout.SOUTH);

        versionBox.addActionListener(event -> publishSelection());
        loaderBox.addActionListener(event -> {
            publishSelection();
            updateSelections();
        });
        playButton.addActionListener(event -> onPlay());
        folderButton.addActionListener(event -> openFolder());

        versionBox.setPreferredSize(new Dimension(280, 34));
        loaderBox.setPreferredSize(new Dimension(140, 34));
        usernameField.setPreferredSize(new Dimension(280, 34));
        ramSpinner.setPreferredSize(new Dimension(140, 34));
    }

    private JComponent settingsCard() {
        RoundPanel card = Theme.card(new GridBagLayout(), 20);

        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.anchor = GridBagConstraints.LINE_START;
        label.insets = new Insets(7, 0, 7, 18);

        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.anchor = GridBagConstraints.LINE_START;
        field.insets = new Insets(7, 0, 7, 32);

        GridBagConstraints label2 = new GridBagConstraints();
        label2.gridx = 2;
        label2.anchor = GridBagConstraints.LINE_START;
        label2.insets = new Insets(7, 0, 7, 18);

        GridBagConstraints field2 = new GridBagConstraints();
        field2.gridx = 3;
        field2.weightx = 1;
        field2.anchor = GridBagConstraints.LINE_START;
        field2.insets = new Insets(7, 0, 7, 0);

        card.add(Theme.label("Minecraft sürümü", Theme.BODY, Theme.MUTED), label);
        card.add(versionBox, field);
        card.add(Theme.label("Yükleyici", Theme.BODY, Theme.MUTED), label2);
        card.add(loaderBox, field2);

        label.gridy = 1;
        field.gridy = 1;
        label2.gridy = 1;
        field2.gridy = 1;
        card.add(Theme.label("Kullanıcı adı", Theme.BODY, Theme.MUTED), label);
        card.add(usernameField, field);
        card.add(Theme.label("Bellek (MB)", Theme.BODY, Theme.MUTED), label2);
        card.add(ramSpinner, field2);

        GridBagConstraints hint = new GridBagConstraints();
        hint.gridx = 0;
        hint.gridy = 2;
        hint.gridwidth = 4;
        hint.anchor = GridBagConstraints.LINE_START;
        hint.insets = new Insets(6, 0, 0, 0);
        card.add(Theme.label("Offline mod: hesap gerekmez, ad yeterli. Fabric seçilirse modlar yüklenir.",
                Theme.SMALL, Theme.MUTED), hint);
        return card;
    }

    private JComponent actionsCard() {
        RoundPanel card = Theme.card(new BorderLayout(14, 10), 16);

        JPanel buttons = new JPanel(new BorderLayout(10, 0));
        buttons.setOpaque(false);
        buttons.add(folderButton, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(status);
        right.add(Box.createHorizontalStrut(14));
        right.add(playButton);
        buttons.add(right, BorderLayout.EAST);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        stack.add(buttons);
        stack.add(Box.createVerticalStrut(10));
        stack.add(progress);

        card.add(stack, BorderLayout.CENTER);
        return card;
    }

    void setVersions(VersionManifest manifest) {
        DefaultComboBoxModel<VersionManifest.Entry> model = new DefaultComboBoxModel<>();
        for (VersionManifest.Entry entry : manifest.releases()) {
            model.addElement(entry);
        }
        versionBox.setModel(model);
        versionBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof VersionManifest.Entry entry) {
                    label.setText(entry.id());
                }
                label.setBackground(selected ? Theme.SURFACE_ALT : Theme.FIELD);
                label.setForeground(Theme.TEXT);
                label.setBorder(new javax.swing.border.EmptyBorder(4, 8, 4, 8));
                return label;
            }
        });
        String wanted = config.version();
        List<VersionManifest.Entry> entries = manifest.releases();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(wanted)) {
                versionBox.setSelectedIndex(i);
                break;
            }
        }
        publishSelection();
        updateSelections();
        status.setText(entries.size() + " sürüm hazır");
    }

    private void publishSelection() {
        Object selected = versionBox.getSelectedItem();
        String version = selected instanceof VersionManifest.Entry entry ? entry.id() : "";
        String loader = loaderBox.getSelectedIndex() == 1 ? Instance.FABRIC : Instance.VANILLA;
        selection.set(version, loader);
    }

    private void updateSelections() {
        status.setText(loaderBox.getSelectedIndex() == 1 ? "Fabric: modlar etkin" : "Vanilla: modlar kapalı");
    }

    private void onPlay() {
        if (!selection.ready()) {
            status.setText("Önce bir sürüm seç");
            return;
        }
        String username = OfflineAuth.sanitize(usernameField.getText());
        usernameField.setText(username);
        int ramMb = (Integer) ramSpinner.getValue();
        config.username(username).ramMb(ramMb).version(selection.mcVersion()).loader(selection.loader()).save();

        Instance instance = selection.instance();
        setBusy(true);
        console.log("=== " + instance.describe() + " başlatılıyor ===");

        Thread worker = new Thread(() -> {
            try {
                Process process = GameLauncher.start(instance, username, ramMb, new Progress() {
                    @Override
                    public void log(String message) {
                        console.log(message);
                    }

                    @Override
                    public void files(int done, int total) {
                        SwingUtilities.invokeLater(() -> {
                            if (total > 0) {
                                progress.setMaximum(total);
                                progress.setValue(done);
                                progress.setString(done + " / " + total);
                            }
                        });
                    }
                });
                SwingUtilities.invokeLater(() -> status.setText("Oyun çalışıyor · pid " + process.pid()));
                int code = process.waitFor();
                console.log("Oyun kapandı (çıkış kodu " + code + ")");
                SwingUtilities.invokeLater(() -> status.setText("Oyun kapandı"));
            } catch (Exception e) {
                console.log("HATA: " + e.getMessage());
                SwingUtilities.invokeLater(() -> status.setText("Hata: " + e.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> setBusy(false));
            }
        }, "freemc-play");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        playButton.setEnabled(!busy);
        versionBox.setEnabled(!busy);
        loaderBox.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        ramSpinner.setEnabled(!busy);
        if (busy) {
            status.setText("Çalışıyor…");
        } else {
            progress.setValue(0);
            progress.setString("");
        }
    }

    private void openFolder() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(selection.ready() ? selection.instance().gameDir().toFile()
                        : Dirs.root().toFile());
                return;
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // fall through to the console
        }
        console.log("Klasör: " + (selection.ready() ? selection.instance().gameDir() : Dirs.root()));
    }
}
