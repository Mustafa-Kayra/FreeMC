package dev.freemc.ui;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** The shared console strip both tabs write into. */
public final class Console extends RoundPanel {

    private static final int MAX_LINES = 2000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea area = new JTextArea();

    public Console() {
        super(Theme.SURFACE, Theme.BORDER, 14);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        Theme.styleLog(area);
        area.setEditable(false);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.FIELD);
        add(scroll, BorderLayout.CENTER);
    }

    public void log(String line) {
        SwingUtilities.invokeLater(() -> {
            area.append("[" + LocalTime.now().format(TIME) + "] " + line + System.lineSeparator());
            int lines = area.getLineCount();
            if (lines > MAX_LINES) {
                try {
                    area.replaceRange("", 0, area.getLineStartOffset(lines - MAX_LINES));
                } catch (javax.swing.text.BadLocationException ignored) {
                    // trimming is cosmetic
                }
            }
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> area.setText(""));
    }
}
