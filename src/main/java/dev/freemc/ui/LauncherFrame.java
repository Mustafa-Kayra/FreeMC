package dev.freemc.ui;

import dev.freemc.Config;
import dev.freemc.Dirs;
import dev.freemc.Jvm;
import dev.freemc.auth.OfflineAuth;
import dev.freemc.game.CommandBuilder;
import dev.freemc.game.GameInstall;
import dev.freemc.game.GameProcess;
import dev.freemc.game.Installer;
import dev.freemc.game.Progress;
import dev.freemc.game.VersionManifest;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.List;

/** The whole user interface: pick a version, type a name, press play. */
public final class LauncherFrame extends JFrame {

    private static final Color BG = new Color(0x16161c);
    private static final Color PANEL = new Color(0x1f1f28);
    private static final Color TEXT = new Color(0xe8e8ef);
    private static final Color MUTED = new Color(0x9a9aab);
    private static final Color ACCENT = new Color(0x3ddc84);
    private static final int MAX_LOG_LINES = 4000;

    private final Config config = Config.load();

    private final JComboBox<VersionManifest.Entry> versionBox = new JComboBox<>();
    private final JTextField usernameField = new JTextField(16);
    private final JSpinner ramSpinner = new JSpinner(new SpinnerNumberModel(2048, 512, 65536, 512));
    private final JButton playButton = new JButton("OYNA");
    private final JButton refreshButton = new JButton("Sürümleri Yenile");
    private final JButton folderButton = new JButton("Klasörü Aç");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("Hazır");
    private final JTextArea logArea = new JTextArea(10, 60);

    public LauncherFrame() {
        super("FreeMC — Minecraft Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 560));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        usernameField.setText(config.username());
        ramSpinner.setValue(config.ramMb());

        refreshButton.addActionListener(event -> loadVersions(true));
        playButton.addActionListener(event -> onPlay());
        folderButton.addActionListener(event -> openDataFolder());

        pack();
        setLocationRelativeTo(null);
        loadVersions(false);
    }

    public void open() {
        setVisible(true);
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(18, 22, 10, 22));

        JLabel title = new JLabel("FreeMC");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(ACCENT);

        JLabel subtitle = new JLabel("crack / offline Minecraft launcher — Windows + Linux");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(0, 12, 0, 0));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(title, BorderLayout.WEST);
        left.add(subtitle, BorderLayout.CENTER);
        panel.add(left, BorderLayout.WEST);

        JLabel dataRoot = new JLabel(Dirs.root().toString());
        dataRoot.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        dataRoot.setForeground(MUTED);
        panel.add(dataRoot, BorderLayout.EAST);
        return panel;
    }

    private JComponent center() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(4, 22, 0, 22));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2e2e3a)),
                new EmptyBorder(14, 16, 14, 16)));

        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.anchor = GridBagConstraints.LINE_START;
        label.insets = new Insets(6, 0, 6, 14);

        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.weightx = 1;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.insets = new Insets(6, 0, 6, 0);

        styleCombo(versionBox);
        versionBox.setPreferredSize(new Dimension(320, 30));
        styleField(usernameField);
        styleSpinner(ramSpinner);
        styleButton(playButton, true);
        styleButton(refreshButton, false);
        styleButton(folderButton, false);

        JPanel versionRow = new JPanel(new BorderLayout(8, 0));
        versionRow.setOpaque(false);
        versionRow.add(versionBox, BorderLayout.CENTER);
        versionRow.add(refreshButton, BorderLayout.EAST);

        form.add(labelFor("Sürüm"), label);
        form.add(versionRow, field);

        label.gridy = 1;
        field.gridy = 1;
        form.add(labelFor("Kullanıcı adı"), label);
        form.add(usernameField, field);

        label.gridy = 2;
        field.gridy = 2;
        form.add(labelFor("Bellek (MB)"), label);
        form.add(ramSpinner, field);

        label.gridy = 3;
        field.gridy = 3;
        JLabel hint = new JLabel("Offline mod: hiçbir hesap gerekmez, ad yeterli.");
        hint.setForeground(MUTED);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        form.add(labelFor(""), label);
        form.add(hint, field);

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        actions.add(folderButton, BorderLayout.WEST);
        actions.add(playButton, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(8, 6));
        bottom.setOpaque(false);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setValue(0);
        progressBar.setBackground(PANEL);
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(progressBar, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(form, BorderLayout.NORTH);
        top.add(bottom, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setBackground(new Color(0x101016));
        logArea.setForeground(new Color(0xc9c9d6));
        logArea.setCaretColor(ACCENT);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x2e2e3a)));
        scroll.getViewport().setBackground(new Color(0x101016));

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent footer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(8, 22, 14, 22));
        JLabel note = new JLabel("Oyun dosyaları Mojang'ın resmi sunucularından indirilir.");
        note.setForeground(MUTED);
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        panel.add(note, BorderLayout.WEST);
        return panel;
    }

    private JLabel labelFor(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        return label;
    }

    private void styleField(JTextField field) {
        field.setBackground(new Color(0x12121a));
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x33333f)),
                new EmptyBorder(5, 8, 5, 8)));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(new Color(0x12121a));
        combo.setForeground(TEXT);
        combo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(0x12121a));
        spinner.setForeground(TEXT);
        spinner.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(new Color(0x12121a));
            defaultEditor.getTextField().setForeground(TEXT);
            defaultEditor.getTextField().setCaretColor(ACCENT);
            defaultEditor.getTextField().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        }
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, primary ? 14 : 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(primary ? 10 : 6, primary ? 28 : 14, primary ? 10 : 6, primary ? 28 : 14));
        if (primary) {
            button.setBackground(ACCENT);
            button.setForeground(new Color(0x0b2a19));
        } else {
            button.setBackground(new Color(0x2a2a35));
            button.setForeground(TEXT);
        }
    }

    private void loadVersions(boolean force) {
        refreshButton.setEnabled(false);
        status("Sürüm listesi alınıyor…");
        Thread worker = new Thread(() -> {
            try {
                VersionManifest manifest = Installer.manifest(force, new Progress() {
                    @Override
                    public void log(String message) {
                        appendLog(message);
                    }
                });
                List<VersionManifest.Entry> entries = manifest.releases();
                SwingUtilities.invokeLater(() -> {
                    DefaultComboBoxModel<VersionManifest.Entry> model = new DefaultComboBoxModel<>();
                    for (VersionManifest.Entry entry : entries) {
                        model.addElement(entry);
                    }
                    versionBox.setModel(model);
                    versionBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
                        @Override
                        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                                              int index, boolean selected,
                                                                              boolean focused) {
                            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                            if (value instanceof VersionManifest.Entry entry) {
                                label.setText(entry.id() + "   (" + entry.type() + ")");
                            }
                            label.setBackground(selected ? new Color(0x2f4f3c) : new Color(0x12121a));
                            label.setForeground(TEXT);
                            return label;
                        }
                    });
                    selectSaved(entries);
                    status(entries.size() + " sürüm listelendi");
                    refreshButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    status("Sürüm listesi alınamadı: " + e.getMessage());
                    appendLog("HATA: " + e.getMessage());
                    refreshButton.setEnabled(true);
                });
            }
        }, "freemc-manifest");
        worker.setDaemon(true);
        worker.start();
    }

    private void selectSaved(List<VersionManifest.Entry> entries) {
        String wanted = config.version();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(wanted)) {
                versionBox.setSelectedIndex(i);
                return;
            }
        }
        if (!entries.isEmpty()) {
            versionBox.setSelectedIndex(0);
        }
    }

    private void onPlay() {
        Object selected = versionBox.getSelectedItem();
        if (!(selected instanceof VersionManifest.Entry entry)) {
            status("Önce bir sürüm seç.");
            return;
        }
        String username = OfflineAuth.sanitize(usernameField.getText());
        usernameField.setText(username);
        int ramMb = (Integer) ramSpinner.getValue();
        config.username(username).ramMb(ramMb).version(entry.id()).save();

        setBusy(true);
        appendLog("=== " + entry.id() + " kuruluyor ===");
        Thread worker = new Thread(() -> {
            try {
                Progress progress = new Progress() {
                    @Override
                    public void log(String message) {
                        appendLog(message);
                    }

                    @Override
                    public void files(int done, int total) {
                        SwingUtilities.invokeLater(() -> {
                            if (total > 0) {
                                progressBar.setMaximum(total);
                                progressBar.setValue(done);
                                progressBar.setString(done + " / " + total);
                            }
                        });
                    }
                };
                GameInstall install = Installer.install(entry.id(), progress);
                OfflineAuth auth = OfflineAuth.of(username);
                String java = Jvm.forVersion(install.version.javaMajor, this::appendLog);
                List<String> command = CommandBuilder.build(install, java, auth.name(), auth.uuid(),
                        auth.accessToken(), ramMb);
                appendLog("Başlatılıyor: " + auth.name() + "  (uuid " + auth.uuid() + ")");
                Process process = GameProcess.launch(command, install.gameDir, this::appendLog);
                SwingUtilities.invokeLater(() -> status("Oyun çalışıyor (pid " + process.pid() + ")"));
                int code = process.waitFor();
                SwingUtilities.invokeLater(() -> status("Oyun kapandı (çıkış kodu " + code + ")"));
            } catch (Exception e) {
                appendLog("HATA: " + e.getMessage());
                SwingUtilities.invokeLater(() -> status("Hata: " + e.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> setBusy(false));
            }
        }, "freemc-launch");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        playButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        versionBox.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        ramSpinner.setEnabled(!busy);
        if (!busy) {
            progressBar.setValue(0);
            progressBar.setString("");
        }
        status(busy ? "Çalışıyor…" : "Hazır");
    }

    private void openDataFolder() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(Dirs.root().toFile());
                return;
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // fall through to the log line
        }
        status("Klasör: " + Dirs.root());
        appendLog("Klasör: " + Dirs.root());
    }

    private void status(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + System.lineSeparator());
            int lines = logArea.getLineCount();
            if (lines > MAX_LOG_LINES) {
                try {
                    logArea.replaceRange("", 0, logArea.getLineStartOffset(lines - MAX_LOG_LINES));
                } catch (javax.swing.text.BadLocationException ignored) {
                    // trimming is cosmetic
                }
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // the platform default is fine
        }
    }
}
