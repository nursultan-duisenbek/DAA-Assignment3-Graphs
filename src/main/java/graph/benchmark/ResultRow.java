package graph.benchmark;

import java.util.Locale;

/**
 * One row of {@code results.csv}: one algorithm on one generated graph.
 *
 * @param algorithm      Kruskal, Prim, Kosaraju, Kahn or DagShortestPath
 * @param graph          graph name, e.g. {@code undirected_sparse_1000}
 * @param density        {@code sparse} or {@code dense} (part of the graph name, not a CSV column)
 * @param n              vertices
 * @param m              edges
 * @param timeMs         median wall-clock time of the runs, in milliseconds
 * @param dsuOps         find calls + successful unions
 * @param edgeScans      edges / adjacency entries looked at
 * @param comparisons    weight comparisons
 * @param maxDepth       deepest explicit DFS stack (0 if the algorithm has none)
 * @param result         MST weight, SCC count, order length or sum of distances
 */
public record ResultRow(String algorithm, String graph, String density, int n, int m, double timeMs,
                        long dsuOps, long edgeScans, long comparisons, long maxDepth, long result) {

    /** CSV header, exactly as required by the assignment. */
    public static final String CSV_HEADER =
            "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";

    /** @return this row as a CSV line (no trailing newline) */
    public String toCsv() {
        return String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d",
                algorithm, graph, n, m, timeMs, dsuOps, edgeScans, comparisons, maxDepth, result);
    }

    /** Parses a line written by {@link #toCsv()}. */
    public static ResultRow fromCsv(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 10) {
            throw new IllegalArgumentException("expected 10 CSV fields, got " + f.length + ": " + line);
        }
        String graph = f[1];
        String density = graph.contains("dense") ? "dense" : graph.contains("sparse") ? "sparse" : "";
        return new ResultRow(f[0], graph, density, Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                Double.parseDouble(f[4]), Long.parseLong(f[5]), Long.parseLong(f[6]), Long.parseLong(f[7]),
                Long.parseLong(f[8]), Long.parseLong(f[9]));
    }
}
