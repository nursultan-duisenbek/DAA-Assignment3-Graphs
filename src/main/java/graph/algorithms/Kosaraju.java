package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.Arrays;

/**
 * Kosaraju's strongly connected components with <b>iterative</b> DFS.
 *
 * <ol>
 *   <li>DFS over the graph records the vertices in order of finishing time.</li>
 *   <li>The graph is transposed.</li>
 *   <li>DFS over the transpose, starting roots in <em>decreasing</em> finishing time; every
 *       DFS tree is exactly one component.</li>
 * </ol>
 *
 * <p>No recursion is used: each pass keeps its own explicit stack of vertices plus, per
 * vertex, the index of the next adjacency entry to examine. The stack therefore lives on the
 * heap and a path of 100 000 vertices (depth 100 000) cannot overflow the JVM call stack.
 *
 * <p>Time O(V + E), extra space O(V + E) (transpose) . Counters: {@code edgeScans} - one per
 * adjacency entry examined in either pass, exactly {@code 2m} for a directed graph;
 * {@code maxDepth} - largest size of the explicit stack in either pass. Building the
 * transpose is a representation step and is not counted as a scan.
 */
public final class Kosaraju {

    private Kosaraju() {
    }

    /** Runs Kosaraju without collecting metrics. */
    public static SccResult run(Graph graph) {
        return run(graph, new Metrics());
    }

    /**
     * Computes the strongly connected components. For an undirected graph these are its
     * connected components.
     */
    public static SccResult run(Graph graph, Metrics metrics) {
        int n = graph.vertexCount();
        int[] stack = new int[n];
        int[] nextEdge = new int[n];

        // Pass 1: finishing order on G.
        boolean[] visited = new boolean[n];
        int[] finishOrder = new int[n];
        int finished = 0;
        for (int root = 0; root < n; root++) {
            if (visited[root]) {
                continue;
            }
            int top = 0;
            stack[top++] = root;
            visited[root] = true;
            metrics.recordDepth(top);
            while (top > 0) {
                int u = stack[top - 1];
                if (nextEdge[u] < graph.degree(u)) {
                    Edge e = graph.edgeAt(u, nextEdge[u]++);
                    metrics.incEdgeScans();
                    int v = e.other(u);
                    if (!visited[v]) {
                        visited[v] = true;
                        stack[top++] = v;
                        metrics.recordDepth(top);
                    }
                } else {
                    top--;                                   // all edges of u done: u finishes
                    finishOrder[finished++] = u;
                }
            }
        }

        // Pass 2: DFS on the transpose in decreasing finishing order.
        Graph transpose = graph.reversed();
        int[] componentOf = new int[n];
        Arrays.fill(componentOf, -1);
        Arrays.fill(nextEdge, 0);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = finishOrder[i];
            if (componentOf[root] != -1) {
                continue;
            }
            int top = 0;
            stack[top++] = root;
            componentOf[root] = count;
            metrics.recordDepth(top);
            while (top > 0) {
                int u = stack[top - 1];
                if (nextEdge[u] < transpose.degree(u)) {
                    Edge e = transpose.edgeAt(u, nextEdge[u]++);
                    metrics.incEdgeScans();
                    int v = e.other(u);
                    if (componentOf[v] == -1) {
                        componentOf[v] = count;
                        stack[top++] = v;
                        metrics.recordDepth(top);
                    }
                } else {
                    top--;
                }
            }
            count++;                                         // one DFS tree = one component
        }
        return new SccResult(count, componentOf);
    }
}
