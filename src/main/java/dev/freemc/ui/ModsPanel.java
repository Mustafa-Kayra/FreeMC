package dev.freemc.ui;

import dev.freemc.game.Instance;
import dev.freemc.game.Progress;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Modrinth browser: search, one-click install, plus manual jar handling. */
final class ModsPanel extends JPanel {

    private final Selection selection;
    private final Console console;

    private final JTextField searchField = Theme.field(26);
    private final FlatButton searchButton = new FlatButton("Ara", FlatButton.Kind.GHOST);
    private final FlatButton addButton = new FlatButton("Dosyadan ekle", FlatButton.Kind.GHOST);
    private final FlatButton refreshButton = new FlatButton("Yenile", FlatButton.Kind.GHOST);
    private final JLabel context = Theme.label("", Theme.SMALL, Theme.MUTED);
    private final JLabel notice = Theme.label("", Theme.SMALL, Theme.ACCENT);
    private final JPanel resultList = new JPanel();
    private final JPanel installedList = new JPanel();
    private boolean busy;

    ModsPanel(Selection selection, Console console) {
        this.selection = selection;
        this.console = console;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);

        add(searchCard(), BorderLayout.NORTH);
        add(lists(), BorderLayout.CENTER);

        searchButton.addActionListener(this::onSearch);
        searchField.addActionListener(this::onSearch);
        addButton.addActionListener(event -> onAddLocal());
        refreshButton.addActionListener(event -> refreshInstalled());

        selection.addListener(updated -> {
            updateContext();
            clearResults("Sürüm değişti, yeniden ara.");
            refreshInstalled();
        });
        updateContext();
        refreshInstalled();
    }

    private JComponent searchCard() {
        RoundPanel card = Theme.card(new BorderLayout(10, 8), 16);

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(searchField, BorderLayout.CENTER);
        row.add(searchButton, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(context, BorderLayout.WEST);
        footer.add(notice, BorderLayout.EAST);

        card.add(Theme.label("Modrinth'te mod ara", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        card.add(row, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JComponent lists() {
        JPanel wrapper = new JPanel(new BorderLayout(14, 0));
        wrapper.setOpaque(false);

        RoundPanel results = Theme.card(new BorderLayout(0, 8), 12);
        results.add(Theme.label("Sonuçlar", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        resultList.setLayout(new BoxLayout(resultList, BoxLayout.Y_AXIS));
        resultList.setBackground(Theme.SURFACE);
        resultList.setOpaque(false);
        resultList.add(emptyLabel("Aramak için bir mod adı yaz — örnek: jei, sodium, iris."));
        JScrollPane resultScroll = scroll(resultList);
        results.add(resultScroll, BorderLayout.CENTER);

        RoundPanel installed = Theme.card(new BorderLayout(0, 8), 12);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Kurulu modlar", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        JPanel headerButtons = new JPanel();
        headerButtons.setOpaque(false);
        headerButtons.setLayout(new BoxLayout(headerButtons, BoxLayout.X_AXIS));
        headerButtons.add(refreshButton);
        headerButtons.add(Box.createHorizontalStrut(8));
        headerButtons.add(addButton);
        header.add(headerButtons, BorderLayout.EAST);
        installed.add(header, BorderLayout.NORTH);

        installedList.setLayout(new BoxLayout(installedList, BoxLayout.Y_AXIS));
        installedList.setOpaque(false);
        JScrollPane installedScroll = scroll(installedList);
        installed.add(installedScroll, BorderLayout.CENTER);

        wrapper.add(results, BorderLayout.CENTER);
        wrapper.add(installed, BorderLayout.EAST);
        installed.setPreferredSize(new Dimension(340, 10));
        return wrapper;
    }

    private JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent emptyLabel(String text) {
        JLabel label = Theme.label(text, Theme.SMALL, Theme.MUTED);
        label.setBorder(new javax.swing.border.EmptyBorder(6, 4, 6, 4));
        return label;
    }

    private void updateContext() {
        if (!selection.ready()) {
            context.setText("Önce Oyna sekmesinden sürüm seç.");
            return;
        }
        String loader = Instance.FABRIC.equals(selection.loader()) ? "Fabric" : "Vanilla";
        context.setText(selection.mcVersion() + " · " + loader + " için sonuçlar");
        notice.setText(Instance.FABRIC.equals(selection.loader()) ? ""
                : "Modlar yalnızca Fabric ile yüklenir — Oyna sekmesinden Fabric'i seç.");
    }

    private void clearResults(String message) {
        resultList.removeAll();
        resultList.add(emptyLabel(message));
        resultList.revalidate();
        resultList.repaint();
    }

    private void onSearch(ActionEvent event) {
        if (busy || !selection.ready()) {
            return;
        }
        String query = searchField.getText().trim();
        String mcVersion = selection.mcVersion();
        String loader = selection.loader();
        setBusy(true);
        clearResults("Aranıyor…");
        console.log("Modrinth araması: \"" + query + "\" (" + mcVersion + " · " + loader + ")");

        Thread worker = new Thread(() -> {
            try {
                List<Modrinth.Hit> hits = Modrinth.search(query, mcVersion, loader, 20);
                SwingUtilities.invokeLater(() -> renderResults(hits));
            } catch (IOException e) {
                console.log("Modrinth hatası: " + e.getMessage());
                SwingUtilities.invokeLater(() -> clearResults("Arama başarısız: " + e.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> setBusy(false));
            }
        }, "freemc-modrinth-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderResults(List<Modrinth.Hit> hits) {
        resultList.removeAll();
        if (hits.isEmpty()) {
            resultList.add(emptyLabel("Sonuç yok. Farklı bir isim dene."));
        }
        for (Modrinth.Hit hit : hits) {
            resultList.add(hitRow(hit));
            resultList.add(Box.createVerticalStrut(8));
        }
        resultList.revalidate();
        resultList.repaint();
        console.log(hits.size() + " sonuç listelendi");
    }

    private JComponent hitRow(Modrinth.Hit hit) {
        RoundPanel row = new RoundPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        row.setLayout(new BorderLayout(12, 4));
        row.setBorder(new javax.swing.border.EmptyBorder(10, 12, 10, 12));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(Theme.label(hit.title(), Theme.HEADING, Theme.TEXT));
        text.add(Box.createVerticalStrut(3));
        text.add(Theme.label("<html><div style='width:420px'>" + escape(hit.description()) + "</div></html>",
                Theme.SMALL, Theme.MUTED));
        text.add(Box.createVerticalStrut(4));
        text.add(Theme.label(hit.author() + " · " + downloads(hit.downloads()) + " indirme",
                Theme.SMALL, Theme.MUTED));

        FlatButton install = new FlatButton("Kur", FlatButton.Kind.GHOST);
        install.setEnabled(Instance.FABRIC.equals(selection.loader()));
        install.addActionListener(event -> install(hit, install));

        row.add(text, BorderLayout.CENTER);
        row.add(install, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void install(Modrinth.Hit hit, FlatButton button) {
        if (!Instance.FABRIC.equals(selection.loader())) {
            console.log("Modlar için Oyna sekmesinden Fabric yükleyiciyi seç.");
            return;
        }
        String mcVersion = selection.mcVersion();
        Path modsDir = selection.instance().modsDir();
        button.setEnabled(false);
        button.setText("…");
        console.log("Kuruluyor: " + hit.title());

        Thread worker = new Thread(() -> {
            try {
                List<String> written = Modrinth.install(hit.slug(), modsDir, mcVersion, Instance.FABRIC,
                        logToConsole());
                if (written.isEmpty()) {
                    console.log(hit.title() + ": yeni dosya yok (zaten kurulu olabilir)");
                } else {
                    console.log(hit.title() + " kuruldu: " + String.join(", ", written));
                }
                SwingUtilities.invokeLater(() -> {
                    button.setText("Kuruldu");
                    refreshInstalled();
                });
            } catch (IOException e) {
                console.log("Kurulum hatası: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    button.setText("Kur");
                    button.setEnabled(true);
                });
            }
        }, "freemc-modrinth-install");
        worker.setDaemon(true);
        worker.start();
    }

    private Progress logToConsole() {
        return console::log;
    }

    private void onAddLocal() {
        if (!selection.ready()) {
            console.log("Önce bir sürüm seç.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Mod jar dosyası seç");
        chooser.setFileFilter(new FileNameExtensionFilter("Mod jar (*.jar)", "jar"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path jar = chooser.getSelectedFile().toPath();
        try {
            Path target = Modrinth.addLocal(jar, selection.instance().modsDir());
            console.log("Eklendi: " + target.getFileName());
            refreshInstalled();
        } catch (IOException e) {
            console.log("Eklenemedi: " + e.getMessage());
        }
    }

    void refreshInstalled() {
        installedList.removeAll();
        if (!selection.ready()) {
            installedList.add(emptyLabel("Sürüm seçilmedi."));
            installedList.revalidate();
            return;
        }
        List<Path> jars = Modrinth.installed(selection.instance().modsDir());
        if (jars.isEmpty()) {
            installedList.add(emptyLabel("Henüz mod yok."));
        }
        for (Path jar : jars) {
            installedList.add(installedRow(jar));
            installedList.add(Box.createVerticalStrut(6));
        }
        installedList.revalidate();
        installedList.repaint();
    }

    private JComponent installedRow(Path jar) {
        RoundPanel row = new RoundPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(new javax.swing.border.EmptyBorder(8, 10, 8, 10));

        String name = jar.getFileName().toString();
        JLabel label = Theme.label("<html><div style='width:190px'>" + escape(name) + "</div></html>",
                Theme.SMALL, Theme.TEXT);
        FlatButton remove = new FlatButton("Sil", FlatButton.Kind.DANGER);
        remove.addActionListener(event -> {
            try {
                Modrinth.remove(jar);
                console.log("Silindi: " + name);
                refreshInstalled();
            } catch (IOException e) {
                console.log("Silinemedi: " + e.getMessage());
            }
        });

        row.add(label, BorderLayout.CENTER);
        row.add(remove, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void setBusy(boolean value) {
        this.busy = value;
        searchButton.setEnabled(!value);
        searchField.setEnabled(!value);
        searchButton.setText(value ? "…" : "Ara");
    }

    private static String downloads(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format("%.1fk", count / 1_000.0);
        }
        return Long.toString(count);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
