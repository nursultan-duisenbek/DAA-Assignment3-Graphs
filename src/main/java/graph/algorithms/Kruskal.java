package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.dsu.DisjointSetUnion;
import graph.metrics.Metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Kruskal's minimum spanning forest: sort the edges by weight, then take an edge whenever
 * {@code find(u) != find(v)} and {@code union(u, v)}.
 *
 * <p>Time O(E log E) for the sort plus O(E alpha(V)) for the DSU; extra space O(E + V).
 *
 * <p>Counters:
 * <ul>
 *   <li>{@code comparisons} - weight comparisons of the merge sort;</li>
 *   <li>{@code edgeScans} - sorted edges examined; the loop stops as soon as the tree has
 *       {@code n - 1} edges, so on dense graphs only a prefix of the sorted list is examined;</li>
 *   <li>{@code dsuOps} - find calls + successful unions, counted by the DSU itself. Each
 *       examined edge costs two finds, each accepted edge additionally one union (which itself
 *       performs two finds), so {@code dsuOps = 2 * edgeScans + 3 * acceptedEdges}.</li>
 * </ul>
 */
public final class Kruskal {

    private Kruskal() {
    }

    /** Runs Kruskal without collecting metrics. */
    public static MstResult run(Graph graph) {
        return run(graph, new Metrics());
    }

    /**
     * Computes a minimum spanning forest of an undirected graph. Never throws on disconnected
     * graphs: it returns a forest and reports the number of components.
     *
     * @throws IllegalArgumentException if the graph is directed
     */
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal needs an undirected graph");
        }
        int n = graph.vertexCount();
        Edge[] sorted = graph.edges().toArray(new Edge[0]);
        EdgeSorter.sortByWeight(sorted, metrics);

        DisjointSetUnion dsu = new DisjointSetUnion(n);
        List<Edge> forest = new ArrayList<>(Math.max(0, n - 1));
        long total = 0;
        for (Edge e : sorted) {
            if (forest.size() == n - 1) {
                break;                              // spanning tree complete, no edge can be added
            }
            metrics.incEdgeScans();
            int ru = dsu.find(e.from());
            int rv = dsu.find(e.to());
            if (ru != rv) {                         // different trees: the edge is safe (cut property)
                dsu.union(ru, rv);
                forest.add(e);
                total += e.weight();
            }
        }
        metrics.addDsuOps(dsu.operations());
        metrics.addDsuPointerHops(dsu.pointerHops());
        return new MstResult(total, forest, dsu.components());
    }
}
