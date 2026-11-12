package graph.benchmark;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.ToDoubleFunction;

/**
 * Builds the three required PNG plots from the result rows:
 * <ol>
 *   <li>{@code operations_vs_n.png} - total counted operations vs n, log-log, one line per
 *       algorithm and density (density = panel);</li>
 *   <li>{@code scans_per_edge.png} - edgeScans / m vs n; flat lines confirm linear work;</li>
 *   <li>{@code kruskal_edge_share.png} - share of the sorted edges Kruskal examines before the
 *       tree is complete, sparse vs dense.</li>
 * </ol>
 * Can also be run on its own: {@code PlotsMain results/results.csv results/plots}.
 */
public final class Plots {

    /** Fixed categorical order: colour follows the algorithm in every chart. */
    private static final Color[] ALGORITHM_COLORS = {
        new Color(0x2a78d6), new Color(0xeb6834), new Color(0x1baf7a), new Color(0xeda100), new Color(0xe87ba4)};
    private static final LineChart.Marker[] ALGORITHM_MARKERS = {
        LineChart.Marker.CIRCLE, LineChart.Marker.SQUARE, LineChart.Marker.TRIANGLE, LineChart.Marker.DIAMOND,
        LineChart.Marker.TRIANGLE_DOWN};
    private static final List<String> DISPLAY_NAMES = List.of("Kruskal", "Prim", "Kosaraju", "Kahn",
            "DAG shortest path");
    /** Ordinal blue steps for the density of one algorithm (sparse lighter, dense darker). */
    private static final Color SPARSE_BLUE = new Color(0x6da7ec);
    private static final Color DENSE_BLUE = new Color(0x184f95);

    private static final String FOOTNOTE = "Source: results/results.csv. Counters are counted inside the algorithms on "
            + "graphs from new Random(42), so they are identical on every machine.";

    private Plots() {
    }

    /** Regenerates the plots from an existing CSV: {@code Plots <results.csv> <plot dir>}. */
    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "results/results.csv");
        Path dir = Path.of(args.length > 1 ? args[1] : "results/plots");
        writeAll(ResultsCsv.read(csv), dir);
        System.out.println("plots written to " + dir);
    }

    /** Writes all three plots into {@code dir}. */
    public static void writeAll(List<ResultRow> rows, Path dir) throws IOException {
        operationsVsN(rows, dir.resolve("operations_vs_n.png"));
        scansPerEdge(rows, dir.resolve("scans_per_edge.png"));
        kruskalEdgeShare(rows, dir.resolve("kruskal_edge_share.png"));
    }

    static void operationsVsN(List<ResultRow> rows, Path file) throws IOException {
        LineChart.Figure fig = baseFigure(rows);
        fig.title = "Operations vs n";
        fig.subtitle = "Total counted operations (dsuOps + edgeScans + comparisons), log-log axes. "
                + "End labels give the fitted slope: 1.00 means linear work.";
        double lo = Double.MAX_VALUE;
        double hi = 0;
        for (ResultRow r : rows) {
            double v = Analysis.totalOperations(r);
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        double yMin = Math.pow(10, Math.floor(Math.log10(lo)));
        double yMax = Math.pow(10, Math.ceil(Math.log10(hi)));
        List<Double> ticks = new ArrayList<>();
        for (double t = yMin; t <= yMax * 1.0001; t *= 10) {
            ticks.add(t);
        }
        for (String density : Analysis.DENSITIES) {
            LineChart.Panel panel = panel(density);
            panel.logY = true;
            panel.yMin = yMin;
            panel.yMax = yMax;
            panel.yTicks = ticks.stream().mapToDouble(Double::doubleValue).toArray();
            panel.yFormat = LineChart::compact;
            panel.yCaption = "total operations (log scale)";
            addAlgorithmSeries(panel, rows, density, r -> (double) Analysis.totalOperations(r), series -> {
                double slope = Analysis.slope(series, r -> (double) Analysis.totalOperations(r));
                return String.format(Locale.ROOT, "slope %.2f", slope);
            });
            fig.panels.add(panel);
        }
        LineChart.render(fig, file);
    }

    static void scansPerEdge(List<ResultRow> rows, Path file) throws IOException {
        LineChart.Figure fig = baseFigure(rows);
        fig.title = "Scans per edge (edgeScans / m) vs n";
        fig.subtitle = "A flat line means the edges looked at grow exactly like m (linear work). Exact counter ratios; "
                + "coinciding lines share one label.";
        double max = 0;
        for (ResultRow r : rows) {
            max = Math.max(max, (double) r.edgeScans() / r.m());
        }
        double yMax = Math.ceil(max * 2 + 0.5) / 2.0;
        List<Double> ticks = new ArrayList<>();
        for (double t = 0; t <= yMax + 1e-9; t += 0.5) {
            ticks.add(t);
        }
        for (String density : Analysis.DENSITIES) {
            LineChart.Panel panel = panel(density);
            panel.yMin = 0;
            panel.yMax = yMax;
            panel.yTicks = ticks.stream().mapToDouble(Double::doubleValue).toArray();
            panel.yFormat = v -> String.format(Locale.ROOT, "%.1f", v);
            panel.yCaption = "edgeScans / m";
            addAlgorithmSeries(panel, rows, density, r -> (double) r.edgeScans() / r.m(), series -> {
                ResultRow last = series.get(series.size() - 1);
                return String.format(Locale.ROOT, "%.2f", (double) last.edgeScans() / last.m());
            });
            fig.panels.add(panel);
        }
        LineChart.render(fig, file);
    }

    static void kruskalEdgeShare(List<ResultRow> rows, Path file) throws IOException {
        LineChart.Figure fig = baseFigure(rows);
        fig.legend.clear();                       // this chart's series are the two densities
        fig.width = 1000;
        fig.endLabelWidth = 190;
        fig.title = "Share of edges Kruskal examines";
        fig.subtitle = "edgeScans / m for Kruskal. After sorting, the scan stops once n - 1 edges are accepted; "
                + "Prim always looks at every edge.";
        LineChart.Panel panel = new LineChart.Panel("Kruskal on sparse (m = 4n) and dense (m = 20n) graphs");
        panel.yMin = 0;
        panel.yMax = 1.05;
        panel.yTicks = new double[] {0, 0.25, 0.5, 0.75, 1.0};
        panel.yFormat = v -> String.format(Locale.ROOT, "%.0f%%", v * 100);
        panel.yCaption = "share of the m edges examined";
        Color[] colors = {SPARSE_BLUE, DENSE_BLUE};
        LineChart.Marker[] markers = {LineChart.Marker.CIRCLE, LineChart.Marker.SQUARE};
        double[] xs = null;
        for (int d = 0; d < Analysis.DENSITIES.size(); d++) {
            String density = Analysis.DENSITIES.get(d);
            List<ResultRow> series = Analysis.select(rows, "Kruskal", density);
            if (series.isEmpty()) {
                continue;
            }
            xs = xs(series);
            double[] ys = values(series, r -> (double) r.edgeScans() / r.m());
            String name = "Kruskal, " + density + " (m = " + (density.equals("sparse") ? 4 : 20) + "n)";
            String label = String.format(Locale.ROOT, "%s %.1f%%", density, ys[ys.length - 1] * 100);
            LineChart.Series s = new LineChart.Series(name, colors[d], markers[d], xs, ys, label);
            panel.series.add(s);
            fig.legend.add(s);
        }
        if (xs != null) {
            panel.references.add(new LineChart.Reference(new double[] {xs[0], xs[xs.length - 1]},
                    new double[] {1.0, 1.0}, "Prim: every edge (100%)"));
        }
        fig.panels.add(panel);
        LineChart.render(fig, file);
    }

    private static LineChart.Figure baseFigure(List<ResultRow> rows) {
        LineChart.Figure fig = new LineChart.Figure();
        double[] sizes = new TreeSet<>(rows.stream().map(r -> (double) r.n()).toList()).stream()
                .mapToDouble(Double::doubleValue).toArray();
        fig.xTicks = sizes;
        fig.xMin = sizes[0] / 1.3;
        fig.xMax = sizes[sizes.length - 1] * 1.3;
        fig.xCaption = "n (vertices, log scale)";
        fig.footnote = FOOTNOTE;
        for (int a = 0; a < DISPLAY_NAMES.size(); a++) {
            fig.legend.add(new LineChart.Series(DISPLAY_NAMES.get(a), ALGORITHM_COLORS[a], ALGORITHM_MARKERS[a],
                    new double[0], new double[0], null));
        }
        return fig;
    }

    private static LineChart.Panel panel(String density) {
        return new LineChart.Panel(density.equals("sparse") ? "Sparse graphs (m = 4n)" : "Dense graphs (m = 20n)");
    }

    private interface LabelMaker {
        String make(List<ResultRow> series);
    }

    private static void addAlgorithmSeries(LineChart.Panel panel, List<ResultRow> rows, String density,
                                           ToDoubleFunction<ResultRow> metric, LabelMaker label) {
        for (int a = 0; a < Analysis.ALGORITHMS.size(); a++) {
            List<ResultRow> series = Analysis.select(rows, Analysis.ALGORITHMS.get(a), density);
            if (series.isEmpty()) {
                continue;
            }
            panel.series.add(new LineChart.Series(DISPLAY_NAMES.get(a), ALGORITHM_COLORS[a], ALGORITHM_MARKERS[a],
                    xs(series), values(series, metric), DISPLAY_NAMES.get(a) + "  " + label.make(series)));
        }
    }

    private static double[] xs(List<ResultRow> series) {
        return values(series, r -> (double) r.n());
    }

    private static double[] values(List<ResultRow> series, ToDoubleFunction<ResultRow> metric) {
        double[] v = new double[series.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = metric.applyAsDouble(series.get(i));
        }
        return v;
    }
}
