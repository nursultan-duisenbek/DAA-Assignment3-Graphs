package graph.benchmark;

import graph.Edge;
import graph.Graph;
import graph.algorithms.CriticalPath;
import graph.algorithms.DagPathResult;
import graph.algorithms.DagShortestPaths;
import graph.algorithms.Kosaraju;
import graph.algorithms.Kruskal;
import graph.algorithms.MstResult;
import graph.algorithms.Prim;
import graph.algorithms.SccResult;
import graph.algorithms.TarjanScc;
import graph.algorithms.TopoResult;
import graph.algorithms.TopologicalSort;
import graph.metrics.Metrics;
import graph.metrics.Stats;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Self-checking benchmark. For every size and density it generates the three graph kinds with
 * the exact generator, runs all five algorithms {@code runs} times (median time, counters from
 * inside the algorithms) and checks, before anything is written:
 *
 * <ul>
 *   <li>Kruskal and Prim return the same total weight and a tree of {@code n - 1} edges;</li>
 *   <li>Kahn returns an order with {@code pos[u] < pos[v]} for every edge;</li>
 *   <li>the DAG distances satisfy the shortest-path optimality certificate;</li>
 *   <li>counter identities: Kosaraju {@code 2m} scans, Kahn and DAG relaxation {@code m},
 *       Prim {@code 2m}, Kruskal {@code dsuOps = 2 * scans + 3 * (n - 1)};</li>
 *   <li>counters are identical in every run (they are deterministic);</li>
 *   <li>the computed MST weight, SCC count and DAG distance sum equal the assignment's
 *       reference table.</li>
 * </ul>
 *
 * Any failed check aborts the run with exit code 1 instead of writing a misleading CSV.
 *
 * <p>Usage: {@code java -jar target/daa-assignment3-graphs-1.0.jar [--out DIR] [--runs K]
 * [--sizes 1000,3162,...] [--no-plots] [--no-bonus]}
 */
public final class BenchmarkRunner {

    /** Sizes in half-decade steps: 10^3, 10^3.5, 10^4, 10^4.5, 10^5. */
    public static final int[] DEFAULT_SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    /** Number of timed runs per algorithm and graph. */
    public static final int DEFAULT_RUNS = 5;

    /** Edge densities of the benchmark. */
    public enum Density {
        SPARSE("sparse", 4), DENSE("dense", 20);

        private final String label;
        private final int factor;

        Density(String label, int factor) {
            this.label = label;
            this.factor = factor;
        }

        public String label() {
            return label;
        }

        /** @return {@code k} in {@code m = k * n} */
        public int factor() {
            return factor;
        }
    }

    /** Benchmark settings. */
    public record Config(Path outputDir, int[] sizes, int runs, boolean plots, boolean bonus) {
        /** Full benchmark: all ten graphs, five runs, plots and bonus, output in {@code results/}. */
        public static Config defaults() {
            return new Config(Path.of("results"), DEFAULT_SIZES, DEFAULT_RUNS, true, true);
        }
    }

    /** Thrown when a self-check fails. */
    public static final class BenchmarkFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public BenchmarkFailure(String message) {
            super(message);
        }
    }

    /** One reference-table comparison: computed value next to the expected value. */
    public record ReferenceCheck(int n, int m, String quantity, long computed, long expected) {
        public boolean ok() {
            return computed == expected;
        }
    }

    /** Everything the run produced. */
    public record Summary(List<ResultRow> rows, List<BonusRow> bonusRows, List<ReferenceCheck> referenceChecks,
                          Map<String, Long> dsuPointerHops) {
    }

    private record Measured<R>(R result, Metrics metrics, double medianMs) {
    }

    private final Config config;
    private final PrintStream out;
    private final List<ResultRow> rows = new ArrayList<>();
    private final List<BonusRow> bonusRows = new ArrayList<>();
    private final List<ReferenceCheck> referenceChecks = new ArrayList<>();
    private final Map<String, Long> dsuPointerHops = new LinkedHashMap<>();

    public BenchmarkRunner(Config config, PrintStream out) {
        if (config.runs() < 1) {
            throw new IllegalArgumentException("runs must be >= 1");
        }
        this.config = config;
        this.out = out;
    }

    /** Entry point; exits with status 1 if any self-check fails. */
    public static void main(String[] args) {
        try {
            Config config = parseArgs(args);
            new BenchmarkRunner(config, System.out).run();
        } catch (BenchmarkFailure e) {
            System.err.println();
            System.err.println("BENCHMARK FAILED: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(2);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            System.exit(2);
        }
    }

    static Config parseArgs(String[] args) {
        Config d = Config.defaults();
        Path out = d.outputDir();
        int[] sizes = d.sizes();
        int runs = d.runs();
        boolean plots = true;
        boolean bonus = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--out" -> out = Path.of(requireValue(args, ++i, "--out"));
                case "--runs" -> runs = Integer.parseInt(requireValue(args, ++i, "--runs"));
                case "--sizes" -> sizes = Arrays.stream(requireValue(args, ++i, "--sizes").split(","))
                        .map(String::trim).mapToInt(Integer::parseInt).toArray();
                case "--no-plots" -> plots = false;
                case "--no-bonus" -> bonus = false;
                default -> throw new IllegalArgumentException("unknown option " + args[i]
                        + " (use --out DIR, --runs K, --sizes a,b,c, --no-plots, --no-bonus)");
            }
        }
        return new Config(out, sizes, runs, plots, bonus);
    }

    private static String requireValue(String[] args, int i, String option) {
        if (i >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[i];
    }

    /** Runs the whole benchmark and writes the CSV files, analysis and plots. */
    public Summary run() throws IOException {
        long started = System.nanoTime();
        out.println("DAA Assignment 3 - self-checking graph benchmark");
        out.printf(Locale.ROOT, "Java %s, %d timed runs per algorithm (median reported), generator seed %d%n%n",
                System.getProperty("java.version"), config.runs(), GraphGenerator.SEED);

        for (int n : config.sizes()) {
            for (Density density : Density.values()) {
                runConfiguration(n, density);
            }
        }

        Path dir = config.outputDir();
        ResultsCsv.write(dir.resolve("results.csv"), rows);
        Analysis.write(dir.resolve("analysis.md"), rows, dsuPointerHops, referenceChecks);
        if (config.bonus()) {
            BonusRow.write(dir.resolve("bonus.csv"), bonusRows);
        }
        if (config.plots()) {
            Plots.writeAll(rows, dir.resolve("plots"));
        }

        printReferenceTable();
        out.printf(Locale.ROOT, "%nAll self-checks passed. Wrote %d rows to %s%s%s (%.1f s total).%n",
                rows.size(), dir.resolve("results.csv"),
                config.bonus() ? ", " + dir.resolve("bonus.csv") : "",
                config.plots() ? ", plots in " + dir.resolve("plots") : "",
                (System.nanoTime() - started) / 1e9);
        return new Summary(List.copyOf(rows), List.copyOf(bonusRows), List.copyOf(referenceChecks),
                Map.copyOf(dsuPointerHops));
    }

    private void runConfiguration(int n, Density density) {
        int m = density.factor() * n;
        String tag = density.label() + "_" + n;
        out.printf(Locale.ROOT, "== n = %,d   m = %,d   (%s) ==%n", n, m, density.label());

        // ---- MST on the undirected graph -------------------------------------------------
        Graph undirected = GraphGenerator.undirected(n, m);
        String ug = "undirected_" + tag;
        Measured<MstResult> kruskal = measure("Kruskal", ug, mt -> Kruskal.run(undirected, mt));
        Measured<MstResult> prim = measure("Prim", ug, mt -> Prim.run(undirected, mt));
        long kw = kruskal.result().totalWeight();
        long pw = prim.result().totalWeight();
        check(kw == pw, "MST weights disagree on " + ug + ": Kruskal=" + kw + ", Prim=" + pw);
        check(kruskal.result().edgeCount() == n - 1 && prim.result().edgeCount() == n - 1,
                "MST on " + ug + " must have n-1 = " + (n - 1) + " edges, Kruskal has "
                        + kruskal.result().edgeCount() + ", Prim has " + prim.result().edgeCount());
        check(kruskal.result().components() == 1 && prim.result().components() == 1,
                "generated undirected graph " + ug + " must be connected");
        checkCounter("Prim edgeScans", ug, prim.metrics().edgeScans(), 2L * m);
        checkCounter("Kruskal dsuOps", ug, kruskal.metrics().dsuOps(),
                2L * kruskal.metrics().edgeScans() + 3L * (n - 1));
        dsuPointerHops.put(ug, kruskal.metrics().dsuPointerHops());
        addRow("Kruskal", ug, density, n, m, kruskal, kw);
        addRow("Prim", ug, density, n, m, prim, pw);
        report("Kruskal", kruskal, "weight " + kw);
        report("Prim", prim, "weight " + pw + " (agrees with Kruskal)");

        // ---- SCC on the directed graph ---------------------------------------------------
        Graph directed = GraphGenerator.directed(n, m);
        String dg = "directed_" + tag;
        Measured<SccResult> kosaraju = measure("Kosaraju", dg, mt -> Kosaraju.run(directed, mt));
        checkCounter("Kosaraju edgeScans", dg, kosaraju.metrics().edgeScans(), 2L * m);
        check(kosaraju.metrics().maxDepth() <= n, "Kosaraju stack deeper than n on " + dg);
        int sccCount = kosaraju.result().count();
        addRow("Kosaraju", dg, density, n, m, kosaraju, sccCount);
        report("Kosaraju", kosaraju, sccCount + " components, max stack depth " + kosaraju.metrics().maxDepth());

        // ---- Topological order and shortest paths on the DAG -----------------------------
        Graph dag = GraphGenerator.dag(n, m);
        String dagName = "dag_" + tag;
        Measured<TopoResult> kahn = measure("Kahn", dagName, mt -> TopologicalSort.run(dag, mt));
        check(kahn.result().isAcyclic(), "Kahn reports a cycle on the generated DAG " + dagName + ": "
                + kahn.result().cycle());
        int[] order = kahn.result().order();
        check(TopologicalSort.isValidOrder(dag, order), "Kahn order violates pos[u] < pos[v] on " + dagName);
        checkCounter("Kahn edgeScans", dagName, kahn.metrics().edgeScans(), m);
        addRow("Kahn", dagName, density, n, m, kahn, order.length);
        report("Kahn", kahn, order.length + " vertices in a valid order (pos[u] < pos[v] checked)");

        Measured<DagPathResult> paths = measure("DagShortestPath", dagName,
                mt -> DagShortestPaths.run(dag, order, mt));
        checkCounter("DagShortestPath edgeScans", dagName, paths.metrics().edgeScans(), m);
        checkShortestPathCertificate(dag, paths.result(), dagName);
        long distSum = paths.result().sumOfDistances();
        addRow("DagShortestPath", dagName, density, n, m, paths, distSum);
        report("DagShortestPath", paths, "sum of distances " + distSum + " (optimality certificate checked)");

        // ---- reference table ---------------------------------------------------------------
        Optional<ReferenceValues.Expected> expected = ReferenceValues.lookup(n, m);
        if (expected.isPresent()) {
            ReferenceValues.Expected e = expected.get();
            List<ReferenceCheck> checks = List.of(
                    new ReferenceCheck(n, m, "MST total weight", kw, e.mstWeight()),
                    new ReferenceCheck(n, m, "SCC count", sccCount, e.sccCount()),
                    new ReferenceCheck(n, m, "sum of DAG distances", distSum, e.dagDistSum()));
            referenceChecks.addAll(checks);
            for (ReferenceCheck c : checks) {
                check(c.ok(), "reference value mismatch for n=" + n + ", m=" + m + ": " + c.quantity()
                        + " computed " + c.computed() + " but the assignment table says " + c.expected());
            }
            out.println("  reference values: MST, SCC and DAG distance sum match the assignment table");
        } else {
            out.println("  (no reference row for this size)");
        }

        if (config.bonus()) {
            runBonus(n, m, density, directed, dg, kosaraju, dag, dagName);
        }
        out.println();
    }

    /** Bonus A (Tarjan vs Kosaraju) and bonus B (condensation and critical path). */
    private void runBonus(int n, int m, Density density, Graph directed, String dg, Measured<SccResult> kosaraju,
                          Graph dag, String dagName) {
        Measured<SccResult> tarjan = measure("Tarjan", dg, mt -> TarjanScc.run(directed, mt));
        check(tarjan.result().samePartitionAs(kosaraju.result()),
                "Tarjan and Kosaraju disagree on the components of " + dg);
        checkCounter("Tarjan edgeScans", dg, tarjan.metrics().edgeScans(), m);
        CriticalPath.Result cpDirected = CriticalPath.run(directed);
        CriticalPath.Result cpDag = CriticalPath.run(dag);
        bonusRows.add(new BonusRow(dg, density.label(), n, m, kosaraju.metrics().edgeScans(),
                tarjan.metrics().edgeScans(), kosaraju.result().count(), tarjan.result().count(),
                kosaraju.metrics().maxDepth(), tarjan.metrics().maxDepth(), kosaraju.medianMs(), tarjan.medianMs(),
                kosaraju.result().largestComponentSize(), cpDirected.length(), cpDirected.components().size()));
        bonusRows.add(new BonusRow(dagName, density.label(), n, m, -1, -1, cpDag.componentCount(), -1, -1, -1,
                Double.NaN, Double.NaN, -1, cpDag.length(), cpDag.components().size()));
        out.printf(Locale.ROOT, "  bonus: Tarjan %d scans vs Kosaraju %d (same %d components); "
                        + "critical path %d (directed) / %d (DAG, %d vertices)%n",
                tarjan.metrics().edgeScans(), kosaraju.metrics().edgeScans(), tarjan.result().count(),
                cpDirected.length(), cpDag.length(), cpDag.components().size());
    }

    private <R> Measured<R> measure(String algorithm, String graph, Function<Metrics, R> task) {
        double[] times = new double[config.runs()];
        Metrics first = null;
        R result = null;
        System.gc();
        for (int run = 0; run < config.runs(); run++) {
            Metrics metrics = new Metrics();
            long t0 = System.nanoTime();
            result = task.apply(metrics);
            long t1 = System.nanoTime();
            times[run] = (t1 - t0) / 1e6;
            if (first == null) {
                first = metrics;
            } else {
                check(first.sameCountsAs(metrics), algorithm + " counters differ between runs on " + graph
                        + ": " + first + " vs " + metrics);
            }
        }
        return new Measured<>(result, first, Stats.median(times));
    }

    /**
     * Optimality certificate for super-source shortest paths: sources are at 0, no edge can
     * still be relaxed, and every other vertex has a tight parent edge.
     */
    private static void checkShortestPathCertificate(Graph dag, DagPathResult r, String name) {
        for (int v = 0; v < dag.vertexCount(); v++) {
            check(r.isReachable(v), "vertex " + v + " unreachable from the super-source on " + name);
            if (dag.inDegree(v) == 0) {
                check(r.distance(v) == 0 && r.parent(v) == -1, "source " + v + " must have distance 0 on " + name);
            } else {
                int id = r.parentEdge(v);
                check(id >= 0, "vertex " + v + " has no parent edge on " + name);
                Edge e = dag.edges().get(id);
                check(e.to() == v && e.from() == r.parent(v) && r.distance(e.from()) + e.weight() == r.distance(v),
                        "parent edge of " + v + " is not tight on " + name);
            }
        }
        for (Edge e : dag.edges()) {
            check(r.distance(e.to()) <= r.distance(e.from()) + e.weight(),
                    "edge " + e + " can still be relaxed on " + name);
        }
    }

    private void addRow(String algorithm, String graph, Density density, int n, int m, Measured<?> measured,
                        long result) {
        Metrics mt = measured.metrics();
        rows.add(new ResultRow(algorithm, graph, density.label(), n, m, measured.medianMs(), mt.dsuOps(),
                mt.edgeScans(), mt.comparisons(), mt.maxDepth(), result));
    }

    private void report(String algorithm, Measured<?> measured, String what) {
        Metrics mt = measured.metrics();
        out.printf(Locale.ROOT, "  %-16s %9.3f ms  dsuOps=%-9d edgeScans=%-9d comparisons=%-10d maxDepth=%-6d %s%n",
                algorithm, measured.medianMs(), mt.dsuOps(), mt.edgeScans(), mt.comparisons(), mt.maxDepth(), what);
    }

    private void printReferenceTable() {
        if (referenceChecks.isEmpty()) {
            return;
        }
        out.println("Reference values: computed (left) vs assignment table (right)");
        out.printf(Locale.ROOT, "%-18s %-26s %-16s %-28s%n", "graph (n / m)", "MST total weight", "SCC count",
                "sum of DAG distances");
        for (int i = 0; i + 2 < referenceChecks.size(); i += 3) {
            ReferenceCheck mst = referenceChecks.get(i);
            ReferenceCheck scc = referenceChecks.get(i + 1);
            ReferenceCheck dist = referenceChecks.get(i + 2);
            out.printf(Locale.ROOT, "%-18s %-26s %-16s %-28s%n", mst.n() + " / " + mst.m(), cell(mst), cell(scc),
                    cell(dist));
        }
    }

    private static String cell(ReferenceCheck c) {
        return c.computed() + (c.ok() ? " = " : " != ") + c.expected() + (c.ok() ? " OK" : " FAIL");
    }

    private static void checkCounter(String what, String graph, long actual, long expected) {
        check(actual == expected, what + " on " + graph + " is " + actual + ", expected exactly " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new BenchmarkFailure(message);
        }
    }
}
