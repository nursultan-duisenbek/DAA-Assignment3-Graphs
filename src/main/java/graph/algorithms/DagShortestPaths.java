package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.Arrays;
import java.util.Objects;

/**
 * Shortest paths in a DAG in one pass: take a topological order and relax the outgoing edges
 * of every vertex in that order. Time O(V + E), extra space O(V).
 *
 * <p><b>Sources.</b> {@link #run(Graph, int[], Metrics)} uses a virtual super-source: every
 * vertex with in-degree 0 starts at distance 0, so the whole DAG is reachable and the result
 * does not depend on an arbitrarily chosen start vertex. {@link #fromSource} computes ordinary
 * single-source distances.
 *
 * <p><b>Negative weights are allowed.</b> When a vertex is processed, all of its predecessors
 * come earlier in the topological order and have already been relaxed into it, so its distance
 * is final before it is used; no later relaxation can lower it again (no cycles).
 *
 * <p>Counters: {@code edgeScans} and {@code comparisons} - one per relaxed edge, exactly
 * {@code m} each when every vertex is reachable. The topological order is an input (it is
 * produced and counted by {@link TopologicalSort}), so these counters cover the relaxation
 * pass only.
 */
public final class DagShortestPaths {

    private DagShortestPaths() {
    }

    /**
     * Convenience: computes the topological order with Kahn first, then runs the
     * super-source relaxation.
     *
     * @throws IllegalArgumentException if the graph has a cycle
     */
    public static DagPathResult run(Graph graph) {
        TopoResult topo = TopologicalSort.run(graph);
        if (topo.hasCycle()) {
            throw new IllegalArgumentException("shortest paths in a DAG need an acyclic graph; cycle: "
                    + topo.cycle());
        }
        return run(graph, topo.order(), new Metrics());
    }

    /**
     * Super-source shortest paths: every vertex of in-degree 0 starts at distance 0.
     *
     * @param topologicalOrder a valid topological order of {@code graph} (e.g. from Kahn)
     */
    public static DagPathResult run(Graph graph, int[] topologicalOrder, Metrics metrics) {
        int n = graph.vertexCount();
        long[] dist = initialDistances(graph, topologicalOrder);
        for (int v = 0; v < n; v++) {
            if (graph.inDegree(v) == 0) {
                dist[v] = 0;                                 // edge of weight 0 from the super-source
            }
        }
        return relax(graph, topologicalOrder, dist, metrics);
    }

    /**
     * Single-source shortest paths from {@code source}; vertices not reachable from it get
     * {@link DagPathResult#UNREACHABLE}.
     */
    public static DagPathResult fromSource(Graph graph, int[] topologicalOrder, int source, Metrics metrics) {
        Objects.checkIndex(source, graph.vertexCount());
        long[] dist = initialDistances(graph, topologicalOrder);
        dist[source] = 0;
        return relax(graph, topologicalOrder, dist, metrics);
    }

    private static long[] initialDistances(Graph graph, int[] order) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("shortest paths in a DAG need a directed graph");
        }
        if (order == null || order.length != graph.vertexCount()) {
            throw new IllegalArgumentException("topological order must contain all " + graph.vertexCount()
                    + " vertices");
        }
        long[] dist = new long[graph.vertexCount()];
        Arrays.fill(dist, DagPathResult.UNREACHABLE);
        return dist;
    }

    private static DagPathResult relax(Graph graph, int[] order, long[] dist, Metrics metrics) {
        int n = graph.vertexCount();
        int[] parent = new int[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parent, -1);
        Arrays.fill(parentEdge, -1);
        for (int u : order) {
            if (dist[u] == DagPathResult.UNREACHABLE) {
                continue;                                    // nothing to propagate from u
            }
            for (Edge e : graph.adjacent(u)) {
                metrics.incEdgeScans();
                int v = e.to();
                long candidate = dist[u] + e.weight();
                metrics.incComparisons();
                if (candidate < dist[v]) {
                    dist[v] = candidate;
                    parent[v] = u;
                    parentEdge[v] = e.id();
                }
            }
        }
        return new DagPathResult(dist, parent, parentEdge);
    }
}
