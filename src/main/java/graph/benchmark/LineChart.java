package graph.benchmark;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.DoubleFunction;

/**
 * Minimal Java2D line-chart renderer (no external libraries), so that the benchmark alone
 * reproduces the plots. Supports one or two panels side by side, log or linear axes, a legend
 * row, selective end-of-line labels (grouped when lines coincide) and muted reference lines.
 * Rendered at 2x resolution for sharp text.
 */
final class LineChart {

    // Chart chrome and ink (light surface).
    static final Color SURFACE = new Color(0xfcfcfb);
    static final Color INK_PRIMARY = new Color(0x0b0b0b);
    static final Color INK_SECONDARY = new Color(0x52514e);
    static final Color INK_MUTED = new Color(0x898781);
    static final Color GRID = new Color(0xe1e0d9);
    static final Color AXIS = new Color(0xc3c2b7);

    private static final int SCALE = 2;
    private static final String FONT_FAMILY = pickFont();

    enum Marker { CIRCLE, SQUARE, TRIANGLE, DIAMOND, TRIANGLE_DOWN }

    /** A data series; {@code endLabel} is the direct label at the last point (null = none). */
    record Series(String name, Color color, Marker marker, double[] x, double[] y, String endLabel) {
    }

    /** A muted, thin reference line with a short label at its right end. */
    record Reference(double[] x, double[] y, String label) {
    }

    /** One plot area. */
    static final class Panel {
        final String title;
        final List<Series> series = new ArrayList<>();
        final List<Reference> references = new ArrayList<>();
        boolean logY;
        double yMin;
        double yMax;
        double[] yTicks;
        DoubleFunction<String> yFormat = v -> String.format(Locale.ROOT, "%.1f", v);
        String yCaption = "";

        Panel(String title) {
            this.title = title;
        }
    }

    /** A whole image. */
    static final class Figure {
        String title = "";
        String subtitle = "";
        String xCaption = "";
        String footnote = "";
        int width = 1200;
        int height = 640;
        int endLabelWidth = 150;
        double xMin;
        double xMax;
        double[] xTicks;
        DoubleFunction<String> xFormat = LineChart::compact;
        final List<Panel> panels = new ArrayList<>();
        final List<Series> legend = new ArrayList<>();
    }

    private LineChart() {
    }

    /** Renders {@code figure} to a PNG file. */
    static void render(Figure fig, Path file) throws IOException {
        BufferedImage image = new BufferedImage(fig.width * SCALE, fig.height * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.scale(SCALE, SCALE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(SURFACE);
            g.fillRect(0, 0, fig.width, fig.height);

            int left = 32;
            int right = fig.width - 32;
            g.setColor(INK_PRIMARY);
            g.setFont(font(Font.BOLD, 20));
            g.drawString(fig.title, left, 44);
            g.setColor(INK_SECONDARY);
            g.setFont(font(Font.PLAIN, 13));
            g.drawString(fig.subtitle, left, 68);

            int legendY = drawLegend(g, fig.legend, left, 100, right);

            int top = legendY + 22;
            int bottom = fig.height - 52;
            int gap = 40;
            int panelCount = fig.panels.size();
            double panelWidth = (right - left - gap * (panelCount - 1)) / (double) panelCount;
            for (int i = 0; i < panelCount; i++) {
                double x0 = left + i * (panelWidth + gap);
                drawPanel(g, fig, fig.panels.get(i), x0, top, x0 + panelWidth, bottom);
            }

            g.setColor(INK_MUTED);
            g.setFont(font(Font.PLAIN, 11));
            g.drawString(fig.footnote, left, fig.height - 16);
        } finally {
            g.dispose();
        }
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        ImageIO.write(image, "png", file.toFile());
    }

    private static int drawLegend(Graphics2D g, List<Series> legend, int x, int y, int maxX) {
        g.setFont(font(Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        double cx = x;
        int cy = y;
        for (Series s : legend) {
            double w = 26 + fm.stringWidth(s.name()) + 22;
            if (cx + w > maxX) {
                cx = x;
                cy += 22;
            }
            g.setColor(s.color());
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx, cy - 4, cx + 20, cy - 4));
            g.setColor(s.color());
            g.fill(markerShape(s.marker(), cx + 10, cy - 4, 4.0));   // no surface ring: keeps the key solid
            g.setColor(INK_SECONDARY);
            g.drawString(s.name(), (float) (cx + 26), cy);
            cx += w;
        }
        return cy;
    }

    private static void drawPanel(Graphics2D g, Figure fig, Panel p, double x0, double y0, double x1, double y1) {
        g.setColor(INK_PRIMARY);
        g.setFont(font(Font.BOLD, 13));
        g.drawString(p.title, (float) x0, (float) (y0 + 12));
        g.setColor(INK_MUTED);
        g.setFont(font(Font.PLAIN, 11));
        g.drawString(p.yCaption, (float) x0, (float) (y0 + 30));

        g.setFont(font(Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int tickLabelWidth = 0;
        for (double t : p.yTicks) {
            tickLabelWidth = Math.max(tickLabelWidth, fm.stringWidth(p.yFormat.apply(t)));
        }
        boolean hasEndLabels = p.series.stream().anyMatch(s -> s.endLabel() != null) || !p.references.isEmpty();
        double plotLeft = x0 + tickLabelWidth + 10;
        double plotRight = x1 - (hasEndLabels ? fig.endLabelWidth : 8);
        double plotTop = y0 + 44;
        double plotBottom = y1 - 30;
        Axis xAxis = new Axis(true, fig.xMin, fig.xMax, plotLeft, plotRight);
        Axis yAxis = new Axis(p.logY, p.yMin, p.yMax, plotBottom, plotTop);

        // Gridlines (horizontal hairlines) and y tick labels.
        g.setStroke(new BasicStroke(1f));
        for (double t : p.yTicks) {
            double py = yAxis.map(t);
            g.setColor(GRID);
            g.draw(new Line2D.Double(plotLeft, py, plotRight, py));
            g.setColor(INK_MUTED);
            String label = p.yFormat.apply(t);
            g.drawString(label, (float) (plotLeft - 8 - fm.stringWidth(label)), (float) (py + 4));
        }
        // Baseline and x ticks.
        g.setColor(AXIS);
        g.draw(new Line2D.Double(plotLeft, plotBottom, plotRight, plotBottom));
        for (double t : fig.xTicks) {
            double px = xAxis.map(t);
            g.setColor(AXIS);
            g.draw(new Line2D.Double(px, plotBottom, px, plotBottom + 4));
            g.setColor(INK_MUTED);
            String label = fig.xFormat.apply(t);
            g.drawString(label, (float) (px - fm.stringWidth(label) / 2.0), (float) (plotBottom + 18));
        }
        String xc = fig.xCaption;
        g.drawString(xc, (float) (plotRight - fm.stringWidth(xc)), (float) (plotBottom + 34));

        // Reference lines (muted, thin, solid).
        List<double[]> labelAnchors = new ArrayList<>();
        List<List<String>> labelTexts = new ArrayList<>();
        for (Reference r : p.references) {
            g.setColor(INK_MUTED);
            g.setStroke(new BasicStroke(1f));
            Path2D path = new Path2D.Double();
            for (int i = 0; i < r.x().length; i++) {
                double px = xAxis.map(r.x()[i]);
                double py = yAxis.map(r.y()[i]);
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            g.draw(path);
            int last = r.x().length - 1;
            labelAnchors.add(new double[] {xAxis.map(r.x()[last]), yAxis.map(r.y()[last])});
            labelTexts.add(List.of(r.label()));
        }

        // Series: 2px lines, then markers with a 2px surface ring.
        for (Series s : p.series) {
            g.setColor(s.color());
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Double();
            for (int i = 0; i < s.x().length; i++) {
                double px = xAxis.map(s.x()[i]);
                double py = yAxis.map(s.y()[i]);
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            g.draw(path);
        }
        for (Series s : p.series) {
            for (int i = 0; i < s.x().length; i++) {
                drawMarker(g, s.marker(), s.color(), xAxis.map(s.x()[i]), yAxis.map(s.y()[i]));
            }
        }

        // End labels: group series whose ends are within 14px so coinciding lines share a label.
        List<Series> labelled = new ArrayList<>();
        for (Series s : p.series) {
            if (s.endLabel() != null) {
                labelled.add(s);
            }
        }
        labelled.sort((a, b) -> Double.compare(yAxis.map(a.y()[a.y().length - 1]),
                yAxis.map(b.y()[b.y().length - 1])));
        int i = 0;
        while (i < labelled.size()) {
            int j = i;
            double first = yAxis.map(lastY(labelled.get(i)));
            List<String> lines = new ArrayList<>();
            double sum = 0;
            while (j < labelled.size() && yAxis.map(lastY(labelled.get(j))) - first < 14) {
                lines.add(labelled.get(j).endLabel());
                sum += yAxis.map(lastY(labelled.get(j)));
                j++;
            }
            labelAnchors.add(new double[] {plotRight, sum / (j - i)});
            labelTexts.add(lines);
            i = j;
        }
        drawEndLabels(g, labelAnchors, labelTexts, plotRight, plotTop, plotBottom);
    }

    private static double lastY(Series s) {
        return s.y()[s.y().length - 1];
    }

    /** Draws label blocks right of the plot, pushing blocks apart so they never overlap. */
    private static void drawEndLabels(Graphics2D g, List<double[]> anchors, List<List<String>> texts,
                                      double plotRight, double plotTop, double plotBottom) {
        g.setFont(font(Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        Integer[] order = new Integer[anchors.size()];
        for (int k = 0; k < order.length; k++) {
            order[k] = k;
        }
        Arrays.sort(order, (a, b) -> Double.compare(anchors.get(a)[1], anchors.get(b)[1]));
        double[] blockTop = new double[order.length];
        double cursor = plotTop - 20;
        for (int k = 0; k < order.length; k++) {
            int idx = order[k];
            double h = texts.get(idx).size() * lineHeight;
            double desired = anchors.get(idx)[1] - h / 2.0;
            blockTop[k] = Math.max(desired, cursor);
            cursor = blockTop[k] + h + 2;
        }
        // If the stack runs past the bottom, shift it up.
        double overflow = cursor - (plotBottom + 20);
        if (overflow > 0) {
            for (int k = 0; k < blockTop.length; k++) {
                blockTop[k] -= overflow;
            }
        }
        for (int k = 0; k < order.length; k++) {
            int idx = order[k];
            double[] anchor = anchors.get(idx);
            List<String> lines = texts.get(idx);
            double h = lines.size() * lineHeight;
            double centre = blockTop[k] + h / 2.0;
            double textX = plotRight + 14;
            if (Math.abs(centre - anchor[1]) > 2 || anchor[0] < plotRight - 1) {
                g.setColor(AXIS);                       // leader line to a displaced label
                g.setStroke(new BasicStroke(1f));
                g.draw(new Line2D.Double(anchor[0] + 6, anchor[1], textX - 4, centre));
            }
            g.setColor(INK_SECONDARY);
            for (int l = 0; l < lines.size(); l++) {
                g.drawString(lines.get(l), (float) textX, (float) (blockTop[k] + (l + 1) * lineHeight - fm.getDescent()));
            }
        }
    }

    private static void drawMarker(Graphics2D g, Marker marker, Color color, double cx, double cy) {
        g.setColor(SURFACE);
        g.fill(markerShape(marker, cx, cy, 6.0));      // 2px surface ring around the r=4 marker
        g.setColor(color);
        g.fill(markerShape(marker, cx, cy, 4.0));
    }

    private static Shape markerShape(Marker marker, double cx, double cy, double r) {
        switch (marker) {
            case SQUARE:
                double s = r * 0.9;
                return new Rectangle2D.Double(cx - s, cy - s, 2 * s, 2 * s);
            case TRIANGLE:
            case TRIANGLE_DOWN: {
                double k = marker == Marker.TRIANGLE ? 1 : -1;
                double rr = r * 1.2;
                Path2D t = new Path2D.Double();
                t.moveTo(cx, cy - k * rr);
                t.lineTo(cx + rr * 0.95, cy + k * rr * 0.65);
                t.lineTo(cx - rr * 0.95, cy + k * rr * 0.65);
                t.closePath();
                return t;
            }
            case DIAMOND: {
                double rr = r * 1.2;
                Path2D d = new Path2D.Double();
                d.moveTo(cx, cy - rr);
                d.lineTo(cx + rr, cy);
                d.lineTo(cx, cy + rr);
                d.lineTo(cx - rr, cy);
                d.closePath();
                return d;
            }
            case CIRCLE:
            default:
                return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        }
    }

    /** Linear or logarithmic mapping from data to pixels. */
    private record Axis(boolean log, double min, double max, double p0, double p1) {
        double map(double v) {
            double t = log ? (Math.log(v) - Math.log(min)) / (Math.log(max) - Math.log(min)) : (v - min) / (max - min);
            return p0 + t * (p1 - p0);
        }
    }

    /** Compact number: 1K, 3.2K, 10K, 31.6K, 100K, 1M, 2.5M ... */
    static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) {
            return trim(v / 1e9) + "B";
        }
        if (a >= 1e6) {
            return trim(v / 1e6) + "M";
        }
        if (a >= 1e3) {
            return trim(v / 1e3) + "K";
        }
        return trim(v);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    private static String pickFont() {
        try {
            Set<String> families = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(Locale.ROOT));
            for (String candidate : List.of("Inter", "Segoe UI", "Helvetica Neue", "DejaVu Sans", "Arial")) {
                if (families.contains(candidate)) {
                    return candidate;
                }
            }
        } catch (RuntimeException | Error e) {
            // headless environment without font configuration: fall back to the logical font
        }
        return Font.SANS_SERIF;
    }
}
