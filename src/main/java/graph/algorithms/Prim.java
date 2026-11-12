package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Prim's minimum spanning forest with a {@link PriorityQueue} (lazy version, as on the
 * lecture slide): start from vertex 0, repeatedly take the lightest queued edge, skip it if
 * its far vertex is already in the tree, otherwise add the vertex and queue its edges.
 * When the queue runs empty while vertices are left, the next unvisited vertex (in index
 * order) starts a new tree, so a disconnected graph yields a forest.
 *
 * <p>Time O(E log E) = O(E log V); extra space O(E) for the queue plus O(V).
 *
 * <p>Counters: {@code edgeScans} - adjacency entries examined (every vertex scans its list
 * once, so {@code 2m} for a graph without self-loops); {@code comparisons} - weight
 * comparisons made by the priority queue.
 */
public final class Prim {

    private Prim() {
    }

    /** Runs Prim without collecting metrics. */
    public static MstResult run(Graph graph) {
        return run(graph, new Metrics());
    }

    /**
     * Computes a minimum spanning forest of an undirected graph.
     *
     * @throws IllegalArgumentException if the graph is directed
     */
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim needs an undirected graph");
        }
        int n = graph.vertexCount();
        boolean[] inTree = new boolean[n];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.incComparisons();
            return Integer.compare(a.weight(), b.weight());
        });
        List<Edge> forest = new ArrayList<>(Math.max(0, n - 1));
        long total = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (inTree[start]) {
                continue;
            }
            components++;                           // start a new tree of the forest
            visit(graph, start, inTree, queue, metrics);
            while (!queue.isEmpty()) {
                Edge e = queue.poll();
                boolean fromIn = inTree[e.from()];
                boolean toIn = inTree[e.to()];
                if (fromIn && toIn) {
                    continue;                       // far vertex already in the tree: skip
                }
                forest.add(e);                      // lightest edge leaving the tree (cut property)
                total += e.weight();
                visit(graph, fromIn ? e.to() : e.from(), inTree, queue, metrics);
            }
        }
        return new MstResult(total, forest, components);
    }

    private static void visit(Graph graph, int vertex, boolean[] inTree, PriorityQueue<Edge> queue,
                              Metrics metrics) {
        inTree[vertex] = true;
        for (Edge e : graph.adjacent(vertex)) {
            metrics.incEdgeScans();
            if (!inTree[e.other(vertex)]) {
                queue.add(e);
            }
        }
    }
}
