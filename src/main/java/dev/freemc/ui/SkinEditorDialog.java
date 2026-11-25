package dev.freemc.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/** Draws a 64x64 skin without leaving the launcher. */
public final class SkinEditorDialog extends JDialog {

    private enum Tool { BRUSH, FILL, ERASER, PICKER }

    private final PixelCanvas canvas;
    private final JLabel colorChip = new JLabel("      ");
    private final java.util.List<FlatButton> toolButtons = new java.util.ArrayList<>();
    private Color color = new Color(0xC6, 0x9C, 0x6D);
    private Tool tool = Tool.BRUSH;
    private BufferedImage result;

    public SkinEditorDialog(Window owner, BufferedImage initial, Consumer<BufferedImage> onSave) {
        super(owner, "Kostüm tasarla", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(0, 10));
        getContentPane().setBackground(Theme.BG);

        canvas = new PixelCanvas(initial == null ? SkinTemplates.steve() : initial, this::currentColor,
                this::activeTool);

        add(toolbar(), BorderLayout.NORTH);
        add(canvasArea(), BorderLayout.CENTER);
        add(sidebar(), BorderLayout.EAST);
        add(actions(onSave), BorderLayout.SOUTH);

        setSize(1000, 760);
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog and returns the painted skin, or null when cancelled. */
    public BufferedImage open() {
        setVisible(true);
        return result;
    }

    private Color currentColor() {
        return color;
    }

    private Tool activeTool() {
        return tool;
    }

    private JComponent toolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(Theme.SURFACE_LOW);
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(Theme.heading("Kostüm tasarla"));

        JComboBox<String> templates = Theme.combo();
        for (String name : SkinTemplates.NAMES) {
            templates.addItem(name);
        }
        templates.setSelectedItem("Steve");
        templates.addActionListener(event -> {
            String name = (String) templates.getSelectedItem();
            if (name != null) {
                canvas.pushUndo();
                canvas.replace(SkinTemplates.create(name));
            }
        });
        left.add(templates);
        bar.add(left, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        tools.setOpaque(false);
        addTool(tools, "Fırça", Tool.BRUSH);
        addTool(tools, "Doldur", Tool.FILL);
        addTool(tools, "Silgi", Tool.ERASER);
        addTool(tools, "Damlalık", Tool.PICKER);
        FlatButton undo = new FlatButton("Geri al", FlatButton.Kind.GHOST);
        undo.addActionListener(event -> canvas.undo());
        tools.add(undo);
        FlatButton clear = new FlatButton("Temizle", FlatButton.Kind.GHOST);
        clear.addActionListener(event -> {
            canvas.pushUndo();
            canvas.replace(SkinTemplates.empty());
        });
        tools.add(clear);
        bar.add(tools, BorderLayout.EAST);
        return bar;
    }

    private void addTool(JPanel parent, String label, Tool value) {
        FlatButton button = new FlatButton(label, FlatButton.Kind.TAB).active(value == tool);
        button.addActionListener(event -> {
            tool = value;
            for (int i = 0; i < toolButtons.size(); i++) {
                toolButtons.get(i).active(i == value.ordinal());
            }
        });
        toolButtons.add(button);
        parent.add(button);
    }

    private JComponent canvasArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.setBorder(new EmptyBorder(0, 14, 0, 0));
        JScrollPane scroll = new JScrollPane(canvas);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.OUTLINE_SOFT));
        scroll.getViewport().setBackground(Theme.SURFACE_LOW);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent sidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.BG);
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(0, 12, 0, 14));
        side.setPreferredSize(new Dimension(300, 10));

        JLabel previewLabel = Theme.heading("Önizleme");
        previewLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(previewLabel);
        side.add(javax.swing.Box.createVerticalStrut(6));

        Preview preview = new Preview(canvas);
        preview.setAlignmentX(LEFT_ALIGNMENT);
        side.add(preview);
        side.add(javax.swing.Box.createVerticalStrut(12));

        JLabel paletteLabel = Theme.heading("Renkler");
        paletteLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(paletteLabel);
        side.add(javax.swing.Box.createVerticalStrut(6));

        Color[] palette = {
                new Color(0xC6, 0x9C, 0x6D), new Color(0xA8, 0x7E, 0x53), new Color(0x39, 0x23, 0x1A),
                new Color(0x8C, 0x5A, 0x3B), Color.WHITE, new Color(0x3B, 0x3B, 0x8C),
                new Color(0x00, 0xA8, 0xA8), new Color(0x3B, 0x3B, 0xA8), new Color(0x4E, 0x9A, 0x4B),
                new Color(0xB0, 0x3A, 0x3A), new Color(0xD7, 0x7A, 0x33), new Color(0xF2, 0xD2, 0xB3),
                new Color(0x4A, 0x4A, 0x4A), new Color(0x8F, 0x8F, 0x8F), new Color(0x2A, 0x2A, 0x2A),
                new Color(0xE8, 0xE8, 0xE8)};
        JPanel swatches = new JPanel(new GridLayout(4, 4, 4, 4));
        swatches.setOpaque(false);
        swatches.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        for (Color entry : palette) {
            JButton swatch = new JButton();
            swatch.setBackground(entry);
            swatch.setPreferredSize(new Dimension(30, 30));
            swatch.setOpaque(true);
            swatch.setBorder(BorderFactory.createLineBorder(Theme.OUTLINE_SOFT));
            swatch.addActionListener(event -> setColor(entry));
            swatches.add(swatch);
        }
        side.add(swatches);
        side.add(javax.swing.Box.createVerticalStrut(10));

        JPanel custom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        custom.setOpaque(false);
        custom.setAlignmentX(LEFT_ALIGNMENT);
        colorChip.setOpaque(true);
        colorChip.setBackground(color);
        colorChip.setBorder(BorderFactory.createLineBorder(Theme.OUTLINE_SOFT));
        custom.add(colorChip);
        FlatButton pick = new FlatButton("Renk seç", FlatButton.Kind.GHOST);
        pick.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(this, "Renk seç", color);
            if (chosen != null) {
                setColor(chosen);
            }
        });
        custom.add(pick);
        side.add(custom);
        side.add(javax.swing.Box.createVerticalStrut(10));

        JLabel hint = Theme.label("<html>Sol tuş boyar, sağ tuş siler. Shift ile damlalık gibi davranır. "
                + "Yüz, gövde, kollar ve bacaklar 8×8'lik bloklar hâlinde serilidir.</html>",
                Theme.body(10.5f), Theme.OUTLINE);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        side.add(hint);
        side.add(javax.swing.Box.createVerticalGlue());
        return side;
    }

    private JComponent actions(Consumer<BufferedImage> onSave) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        bar.setBackground(Theme.SURFACE_LOW);
        FlatButton cancel = new FlatButton("İptal", FlatButton.Kind.GHOST);
        cancel.addActionListener(event -> dispose());
        FlatButton save = new FlatButton("Kaydet", FlatButton.Kind.PRIMARY);
        save.addActionListener(event -> {
            result = canvas.image();
            if (onSave != null) {
                onSave.accept(result);
            }
            dispose();
        });
        bar.add(cancel);
        bar.add(save);
        return bar;
    }

    private void setColor(Color value) {
        color = value;
        colorChip.setBackground(value);
        if (tool == Tool.ERASER || tool == Tool.PICKER) {
            tool = Tool.BRUSH;
            for (int i = 0; i < toolButtons.size(); i++) {
                toolButtons.get(i).active(i == Tool.BRUSH.ordinal());
            }
        }
    }

    private void onPicked(Color value) {
        setColor(value);
    }

    // ------------------------------------------------------------- canvas

    /** The 64x64 texture on a zoomed grid. */
    private static final class PixelCanvas extends JComponent {

        private static final int SIZE = 64;

        private BufferedImage image;
        private final int zoom = 8;
        private final java.util.function.Supplier<Color> colorSource;
        private final java.util.function.Supplier<Tool> toolSource;
        private final Deque<BufferedImage> undo = new ArrayDeque<>();
        private final java.util.List<Runnable> listeners = new java.util.ArrayList<>();

        PixelCanvas(BufferedImage image, java.util.function.Supplier<Color> colorSource,
                    java.util.function.Supplier<Tool> toolSource) {
            this.image = image;
            this.colorSource = colorSource;
            this.toolSource = toolSource;
            setPreferredSize(new Dimension(SIZE * zoom, SIZE * zoom));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    pushUndo();
                    paintAt(event, true);
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    paintAt(event, false);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        BufferedImage image() {
            return image;
        }

        void replace(BufferedImage next) {
            this.image = next;
            notifyListeners();
            repaint();
        }

        void pushUndo() {
            BufferedImage copy = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            undo.push(copy);
            if (undo.size() > 40) {
                undo.removeLast();
            }
        }

        void undo() {
            BufferedImage previous = undo.poll();
            if (previous != null) {
                image = previous;
                notifyListeners();
                repaint();
            }
        }

        void addListener(Runnable listener) {
            listeners.add(listener);
        }

        private void notifyListeners() {
            for (Runnable listener : listeners) {
                listener.run();
            }
        }

        private void paintAt(MouseEvent event, boolean pressed) {
            Point point = event.getPoint();
            int x = point.x / zoom;
            int y = point.y / zoom;
            if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
                return;
            }
            Tool tool = toolSource.get();
            boolean erasing = tool == Tool.ERASER || SwingUtilities.isRightMouseButton(event)
                    || event.isShiftDown();
            if (tool == Tool.PICKER || (event.isShiftDown() && pressed && tool == Tool.BRUSH)) {
                Color picked = new Color(image.getRGB(x, y), true);
                if (picked.getAlpha() > 0) {
                    onPick(picked);
                }
                return;
            }
            if (tool == Tool.FILL && pressed) {
                floodFill(x, y, SwingUtilities.isRightMouseButton(event) ? new Color(0, true) : colorSource.get());
                notifyListeners();
                repaint();
                return;
            }
            if (tool == Tool.ERASER) {
                erasing = true;
            }
            image.setRGB(x, y, erasing ? 0x00000000 : colorSource.get().getRGB());
            setDragColor(erasing);
            notifyListeners();
            repaint();
        }

        private boolean erasingDrag;

        private void setDragColor(boolean erasing) {
            this.erasingDrag = erasing;
        }

        private void onPick(Color color) {
            if (color != null) {
                picked = color;
            }
        }

        private Color picked;

        private void floodFill(int x, int y, Color replacement) {
            int target = image.getRGB(x, y);
            int fill = replacement.getRGB();
            if (target == fill) {
                return;
            }
            java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
            queue.add(new int[]{x, y});
            while (!queue.isEmpty()) {
                int[] cell = queue.poll();
                int cx = cell[0];
                int cy = cell[1];
                if (cx < 0 || cy < 0 || cx >= SIZE || cy >= SIZE || image.getRGB(cx, cy) != target) {
                    continue;
                }
                image.setRGB(cx, cy, fill);
                queue.add(new int[]{cx + 1, cy});
                queue.add(new int[]{cx - 1, cy});
                queue.add(new int[]{cx, cy + 1});
                queue.add(new int[]{cx, cy - 1});
            }
        }

        Color pickedColor() {
            return picked;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(Theme.SURFACE_LOW);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(image, 0, 0, SIZE * zoom, SIZE * zoom, null);

            g.setColor(new Color(1f, 1f, 1f, 0.06f));
            for (int i = 1; i < SIZE; i++) {
                g.drawLine(i * zoom, 0, i * zoom, SIZE * zoom);
                g.drawLine(0, i * zoom, SIZE * zoom, i * zoom);
            }
            g.setColor(new Color(1f, 1f, 1f, 0.16f));
            for (int i = 0; i <= SIZE; i += 8) {
                g.drawLine(i * zoom, 0, i * zoom, SIZE * zoom);
                g.drawLine(0, i * zoom, SIZE * zoom, i * zoom);
            }
            g.setColor(Theme.ACCENT);
            g.drawRect(0, 0, SIZE * zoom - 1, SIZE * zoom - 1);
            g.dispose();
        }
    }

    /** Live body preview that follows every brush stroke. */
    private static final class Preview extends JComponent {
        private final PixelCanvas canvas;

        Preview(PixelCanvas canvas) {
            this.canvas = canvas;
            setPreferredSize(new Dimension(260, 260));
            setAlignmentX(LEFT_ALIGNMENT);
            canvas.addListener(this::repaint);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(Theme.SURFACE_LOW);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            BufferedImage body = dev.freemc.game.Skin.body(canvas.image(), 7);
            if (body != null) {
                int x = (getWidth() - body.getWidth()) / 2;
                int y = (getHeight() - body.getHeight()) / 2;
                g.drawImage(body, x, y, null);
            }
            g.dispose();
        }
    }
}
