package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.Arrays;

/**
 * Bonus A: Tarjan's strongly connected components in a <b>single</b> iterative DFS pass,
 * using {@code index[]}, {@code lowlink[]} and a stack of active vertices.
 *
 * <p>{@code lowlink[u]} is the smallest DFS index reachable from the subtree of {@code u}
 * through at most one back/cross edge to a vertex that is still on the active stack. A vertex
 * with {@code lowlink[u] == index[u]} is the root of a component; that component is popped
 * from the active stack when {@code u} finishes.
 *
 * <p>Compared with Kosaraju it scans every edge once instead of twice ({@code m} instead of
 * {@code 2m} edge scans) and needs no transpose graph. Time O(V + E), extra space O(V).
 * {@code maxDepth} records the DFS (call) stack.
 */
public final class TarjanScc {

    private TarjanScc() {
    }

    /** Runs Tarjan without collecting metrics. */
    public static SccResult run(Graph graph) {
        return run(graph, new Metrics());
    }

    /** Computes the strongly connected components in one DFS pass. */
    public static SccResult run(Graph graph, Metrics metrics) {
        int n = graph.vertexCount();
        int[] index = new int[n];
        int[] low = new int[n];
        Arrays.fill(index, -1);
        boolean[] onStack = new boolean[n];
        int[] active = new int[n];          // Tarjan's stack of active vertices
        int activeTop = 0;
        int[] callStack = new int[n];       // explicit replacement of the recursion
        int[] nextEdge = new int[n];
        int[] componentOf = new int[n];
        int nextIndex = 0;
        int count = 0;

        for (int root = 0; root < n; root++) {
            if (index[root] != -1) {
                continue;
            }
            int top = 0;
            callStack[top++] = root;
            index[root] = low[root] = nextIndex++;
            active[activeTop++] = root;
            onStack[root] = true;
            metrics.recordDepth(top);

            while (top > 0) {
                int u = callStack[top - 1];
                if (nextEdge[u] < graph.degree(u)) {
                    Edge e = graph.edgeAt(u, nextEdge[u]++);
                    metrics.incEdgeScans();
                    int v = e.other(u);
                    if (index[v] == -1) {                      // tree edge: "recurse" into v
                        index[v] = low[v] = nextIndex++;
                        active[activeTop++] = v;
                        onStack[v] = true;
                        callStack[top++] = v;
                        metrics.recordDepth(top);
                    } else if (onStack[v]) {                   // edge into the current component
                        low[u] = Math.min(low[u], index[v]);
                    }
                } else {                                       // u finishes
                    top--;
                    if (low[u] == index[u]) {                  // u is the root of a component
                        int w;
                        do {
                            w = active[--activeTop];
                            onStack[w] = false;
                            componentOf[w] = count;
                        } while (w != u);
                        count++;
                    }
                    if (top > 0) {
                        int parent = callStack[top - 1];
                        low[parent] = Math.min(low[parent], low[u]);
                    }
                }
            }
        }
        return new SccResult(count, componentOf);
    }
}
