package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Kahn's topological sort: in-degrees plus a FIFO queue.
 *
 * <p>All vertices with in-degree 0 enter the queue; when a vertex leaves the queue it is
 * appended to the order and every outgoing edge decrements the in-degree of its head; a head
 * that reaches 0 enters the queue. If fewer than {@code n} vertices leave the queue, the rest
 * all keep a positive in-degree, which is only possible if they contain a <b>cycle</b> - this
 * is reported instead of returning a wrong order.
 *
 * <p>The in-degrees come from the {@link Graph}, which maintains them while edges are inserted;
 * Kahn works on its own copy (O(V)). Time O(V + E), extra space O(V).
 * Counter: {@code edgeScans} - one per outgoing edge examined, exactly {@code m} on a DAG.
 */
public final class TopologicalSort {

    private TopologicalSort() {
    }

    /** Runs Kahn without collecting metrics. */
    public static TopoResult run(Graph graph) {
        return run(graph, new Metrics());
    }

    /**
     * Computes a topological order or detects a cycle.
     *
     * @throws IllegalArgumentException if the graph is undirected
     */
    public static TopoResult run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("topological order needs a directed graph");
        }
        int n = graph.vertexCount();
        int[] inDegree = graph.inDegrees();
        int[] queue = new int[n];                  // every vertex enters at most once
        int head = 0;
        int tail = 0;
        for (int v = 0; v < n; v++) {
            if (inDegree[v] == 0) {
                queue[tail++] = v;
            }
        }
        while (head < tail) {
            int u = queue[head++];
            for (Edge e : graph.adjacent(u)) {
                metrics.incEdgeScans();
                if (--inDegree[e.to()] == 0) {
                    queue[tail++] = e.to();
                }
            }
        }
        int[] processed = new int[tail];
        System.arraycopy(queue, 0, processed, 0, tail);
        List<Integer> cycle = tail == n ? Collections.emptyList() : findCycle(graph, inDegree);
        return new TopoResult(n, processed, cycle);
    }

    /**
     * Extracts one cycle for the error report. Every vertex left with a positive in-degree
     * has a predecessor that is also left over, so walking backwards through left-over
     * predecessors must eventually repeat a vertex. Diagnostic only, not counted in metrics.
     */
    private static List<Integer> findCycle(Graph graph, int[] remainingInDegree) {
        int n = graph.vertexCount();
        int[] leftOverPredecessor = new int[n];
        Arrays.fill(leftOverPredecessor, -1);
        for (Edge e : graph.edges()) {
            if (remainingInDegree[e.from()] > 0 && remainingInDegree[e.to()] > 0) {
                leftOverPredecessor[e.to()] = e.from();
            }
        }
        int start = -1;
        for (int v = 0; v < n; v++) {
            if (remainingInDegree[v] > 0) {
                start = v;
                break;
            }
        }
        int[] seenAt = new int[n];
        Arrays.fill(seenAt, -1);
        List<Integer> walk = new ArrayList<>();
        int v = start;
        while (seenAt[v] == -1) {
            seenAt[v] = walk.size();
            walk.add(v);
            v = leftOverPredecessor[v];
        }
        List<Integer> cycle = new ArrayList<>(walk.subList(seenAt[v], walk.size()));
        Collections.reverse(cycle);                // backwards walk -> forward edge direction
        return cycle;
    }

    /**
     * Checks that {@code order} is a permutation of the vertices with {@code pos[u] < pos[v]}
     * for every edge {@code u -> v}. A self-loop can never satisfy this.
     */
    public static boolean isValidOrder(Graph graph, int[] order) {
        int n = graph.vertexCount();
        if (order == null || order.length != n) {
            return false;
        }
        int[] pos = new int[n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            int v = order[i];
            if (v < 0 || v >= n || pos[v] != -1) {
                return false;
            }
            pos[v] = i;
        }
        for (Edge e : graph.edges()) {
            if (pos[e.from()] >= pos[e.to()]) {
                return false;
            }
        }
        return true;
    }
}
