package dev.freemc.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Small geometric glyphs drawn in code, so the UI needs no icon assets. */
public final class Icons {

    private Icons() {
    }

    public static void draw(Graphics2D graphics, String name, int size, Color color) {
        if (name == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        double s = size;
        double stroke = Math.max(1.4, s / 11.0);
        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "play" -> {
                Path2D.Double triangle = new Path2D.Double();
                triangle.moveTo(s * 0.26, s * 0.16);
                triangle.lineTo(s * 0.84, s * 0.5);
                triangle.lineTo(s * 0.26, s * 0.84);
                triangle.closePath();
                g.fill(triangle);
            }
            case "search" -> {
                g.draw(new Ellipse2D.Double(s * 0.14, s * 0.14, s * 0.5, s * 0.5));
                g.draw(new java.awt.geom.Line2D.Double(s * 0.6, s * 0.6, s * 0.86, s * 0.86));
            }
            case "dashboard" -> {
                double w = s * 0.34;
                g.fill(new RoundRectangle2D.Double(s * 0.12, s * 0.12, w, w, 3, 3));
                g.fill(new RoundRectangle2D.Double(s * 0.54, s * 0.12, w, w, 3, 3));
                g.fill(new RoundRectangle2D.Double(s * 0.12, s * 0.54, w, w, 3, 3));
                g.draw(new RoundRectangle2D.Double(s * 0.54, s * 0.54, w, w, 3, 3));
            }
            case "library" -> {
                for (int i = 0; i < 3; i++) {
                    double y = s * (0.18 + i * 0.24);
                    double width = s * (0.74 - i * 0.1);
                    g.draw(new java.awt.geom.Line2D.Double(s * 0.12, y, s * 0.12 + width, y));
                }
            }
            case "user" -> {
                g.draw(new Ellipse2D.Double(s * 0.32, s * 0.14, s * 0.36, s * 0.36));
                g.draw(new java.awt.geom.Arc2D.Double(s * 0.14, s * 0.56, s * 0.72, s * 0.62, 0, 180,
                        java.awt.geom.Arc2D.OPEN));
            }
            case "settings" -> {
                g.draw(new Ellipse2D.Double(s * 0.3, s * 0.3, s * 0.4, s * 0.4));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    double x1 = 0.5 + Math.cos(angle) * 0.3;
                    double y1 = 0.5 + Math.sin(angle) * 0.3;
                    double x2 = 0.5 + Math.cos(angle) * 0.44;
                    double y2 = 0.5 + Math.sin(angle) * 0.44;
                    g.draw(new java.awt.geom.Line2D.Double(x1 * s, y1 * s, x2 * s, y2 * s));
                }
            }
            case "back" -> {
                Path2D.Double chevron = new Path2D.Double();
                chevron.moveTo(s * 0.6, s * 0.2);
                chevron.lineTo(s * 0.3, s * 0.5);
                chevron.lineTo(s * 0.6, s * 0.8);
                g.draw(chevron);
            }
            case "refresh" -> {
                g.draw(new java.awt.geom.Arc2D.Double(s * 0.16, s * 0.16, s * 0.68, s * 0.68, 40, 280,
                        java.awt.geom.Arc2D.OPEN));
                Path2D.Double head = new Path2D.Double();
                head.moveTo(s * 0.62, s * 0.1);
                head.lineTo(s * 0.86, s * 0.24);
                head.lineTo(s * 0.6, s * 0.34);
                g.fill(head);
            }
            case "download" -> {
                g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.14, s * 0.5, s * 0.6));
                Path2D.Double arrow = new Path2D.Double();
                arrow.moveTo(s * 0.3, s * 0.44);
                arrow.lineTo(s * 0.5, s * 0.66);
                arrow.lineTo(s * 0.7, s * 0.44);
                g.draw(arrow);
                g.draw(new java.awt.geom.Line2D.Double(s * 0.2, s * 0.82, s * 0.8, s * 0.82));
            }
            case "check" -> {
                Path2D.Double check = new Path2D.Double();
                check.moveTo(s * 0.2, s * 0.52);
                check.lineTo(s * 0.42, s * 0.74);
                check.lineTo(s * 0.8, s * 0.28);
                g.draw(check);
            }
            case "warning" -> {
                Path2D.Double triangle = new Path2D.Double();
                triangle.moveTo(s * 0.5, s * 0.14);
                triangle.lineTo(s * 0.88, s * 0.82);
                triangle.lineTo(s * 0.12, s * 0.82);
                triangle.closePath();
                g.draw(triangle);
                g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.4, s * 0.5, s * 0.6));
                g.fill(new Ellipse2D.Double(s * 0.47, s * 0.66, s * 0.06, s * 0.06));
            }
            case "close" -> {
                g.draw(new java.awt.geom.Line2D.Double(s * 0.26, s * 0.26, s * 0.74, s * 0.74));
                g.draw(new java.awt.geom.Line2D.Double(s * 0.74, s * 0.26, s * 0.26, s * 0.74));
            }
            case "plus" -> {
                g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.2, s * 0.5, s * 0.8));
                g.draw(new java.awt.geom.Line2D.Double(s * 0.2, s * 0.5, s * 0.8, s * 0.5));
            }
            case "trash" -> {
                g.draw(new java.awt.geom.Line2D.Double(s * 0.2, s * 0.28, s * 0.8, s * 0.28));
                g.draw(new java.awt.geom.Rectangle2D.Double(s * 0.28, s * 0.28, s * 0.44, s * 0.56));
                g.draw(new java.awt.geom.Line2D.Double(s * 0.4, s * 0.16, s * 0.6, s * 0.16));
            }
            case "folder" -> {
                Path2D.Double folder = new Path2D.Double();
                folder.moveTo(s * 0.12, s * 0.3);
                folder.lineTo(s * 0.44, s * 0.3);
                folder.lineTo(s * 0.52, s * 0.4);
                folder.lineTo(s * 0.12, s * 0.4);
                folder.closePath();
                g.draw(folder);
                g.draw(new RoundRectangle2D.Double(s * 0.12, s * 0.3, s * 0.76, s * 0.44, 4, 4));
            }
            case "layers" -> {
                Path2D.Double top = new Path2D.Double();
                top.moveTo(s * 0.5, s * 0.16);
                top.lineTo(s * 0.86, s * 0.36);
                top.lineTo(s * 0.5, s * 0.56);
                top.lineTo(s * 0.14, s * 0.36);
                top.closePath();
                g.draw(top);
                g.draw(new java.awt.geom.Line2D.Double(s * 0.14, s * 0.52, s * 0.5, s * 0.72));
                g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.72, s * 0.86, s * 0.52));
            }
            default -> g.draw(new Ellipse2D.Double(s * 0.2, s * 0.2, s * 0.6, s * 0.6));
        }
        g.dispose();
    }
}
