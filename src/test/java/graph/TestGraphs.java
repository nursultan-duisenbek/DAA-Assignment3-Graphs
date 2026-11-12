package graph;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Random graph builders and slow but obviously-correct reference algorithms ("oracles") used
 * to cross-check the real implementations.
 */
public final class TestGraphs {

    /** Directory with the sample graphs. */
    public static final Path DATA = Path.of("data");

    private TestGraphs() {
    }

    /** Random connected undirected graph: random tree plus {@code extra} random edges (loops and parallels allowed). */
    public static Graph randomConnected(int n, int extra, Random rnd, int minW, int maxW) {
        Graph g = Graph.undirected(n);
        for (int v = 1; v < n; v++) {
            g.addEdge(rnd.nextInt(v), v, weight(rnd, minW, maxW));
        }
        for (int k = 0; k < extra; k++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), weight(rnd, minW, maxW));
        }
        return g;
    }

    /** Random undirected graph with {@code m} edges, possibly disconnected. */
    public static Graph randomUndirected(int n, int m, Random rnd, int minW, int maxW) {
        Graph g = Graph.undirected(n);
        for (int k = 0; k < m; k++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), weight(rnd, minW, maxW));
        }
        return g;
    }

    /** Random directed graph with {@code m} edges (self-loops and parallel edges allowed). */
    public static Graph randomDirected(int n, int m, Random rnd) {
        Graph g = Graph.directed(n);
        for (int k = 0; k < m; k++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(10));
        }
        return g;
    }

    /** Random DAG: edges go from lower to higher position of a random permutation. */
    public static Graph randomDag(int n, int m, Random rnd, int minW, int maxW) {
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = rank[i];
            rank[i] = rank[j];
            rank[j] = t;
        }
        Graph g = Graph.directed(n);
        if (n < 2) {
            return g;
        }
        for (int k = 0; k < m; k++) {
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n);
            while (b == a) {
                b = rnd.nextInt(n);
            }
            if (rank[a] > rank[b]) {
                int t = a;
                a = b;
                b = t;
            }
            g.addEdge(a, b, weight(rnd, minW, maxW));
        }
        return g;
    }

    private static int weight(Random rnd, int minW, int maxW) {
        return minW + rnd.nextInt(maxW - minW + 1);
    }

    /** BFS reachability from {@code s} following adjacency lists. */
    public static boolean[] reachable(Graph g, int s) {
        boolean[] seen = new boolean[g.vertexCount()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[s] = true;
        queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (Edge e : g.adjacent(u)) {
                int v = e.other(u);
                if (!seen[v]) {
                    seen[v] = true;
                    queue.add(v);
                }
            }
        }
        return seen;
    }

    /**
     * Brute-force SCC oracle: BFS from every vertex on G and on the transpose; u and v share a
     * component iff each reaches the other. Returns canonical ids (smallest vertex of the component).
     */
    public static int[] bruteForceSccRepresentatives(Graph g) {
        int n = g.vertexCount();
        Graph rev = g.reversed();
        int[] rep = new int[n];
        Arrays.fill(rep, -1);
        for (int s = 0; s < n; s++) {
            if (rep[s] != -1) {
                continue;
            }
            boolean[] fwd = reachable(g, s);
            boolean[] bwd = reachable(rev, s);
            for (int v = 0; v < n; v++) {
                if (fwd[v] && bwd[v]) {
                    rep[v] = s;
                }
            }
        }
        return rep;
    }

    /** Number of distinct components in a representative array. */
    public static int countDistinct(int[] ids) {
        return (int) Arrays.stream(ids).distinct().count();
    }

    /**
     * MST oracle without DSU: Kruskal where the cycle test is a BFS over the edges chosen so
     * far - the slow O(V * E) variant the assignment warns about, fine for small graphs.
     */
    public static long naiveMinimumForestWeight(Graph g) {
        List<Edge> sorted = new ArrayList<>(g.edges());
        sorted.sort(Comparator.comparingInt(Edge::weight));
        Graph forest = Graph.undirected(g.vertexCount());
        long total = 0;
        for (Edge e : sorted) {
            if (!reachable(forest, e.from())[e.to()]) {
                forest.addEdge(e.from(), e.to(), e.weight());
                total += e.weight();
            }
        }
        return total;
    }

    /** Number of connected components of an undirected graph by repeated BFS. */
    public static int connectedComponents(Graph g) {
        boolean[] seen = new boolean[g.vertexCount()];
        int count = 0;
        for (int s = 0; s < g.vertexCount(); s++) {
            if (!seen[s]) {
                count++;
                boolean[] r = reachable(g, s);
                for (int v = 0; v < r.length; v++) {
                    seen[v] |= r[v];
                }
            }
        }
        return count;
    }

    /**
     * Bellman-Ford oracle: {@code n - 1} rounds of relaxing every edge, starting from the given
     * initial distances ({@code Long.MAX_VALUE} = unreachable).
     */
    public static long[] bellmanFord(Graph g, long[] initial) {
        long[] dist = initial.clone();
        for (int round = 1; round < g.vertexCount(); round++) {
            boolean changed = false;
            for (Edge e : g.edges()) {
                if (dist[e.from()] != Long.MAX_VALUE && dist[e.from()] + e.weight() < dist[e.to()]) {
                    dist[e.to()] = dist[e.from()] + e.weight();
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return dist;
    }

    /** Initial distances for the super-source: 0 at every in-degree-0 vertex. */
    public static long[] superSourceStart(Graph g) {
        long[] dist = new long[g.vertexCount()];
        Arrays.fill(dist, Long.MAX_VALUE);
        for (int v = 0; v < g.vertexCount(); v++) {
            if (g.inDegree(v) == 0) {
                dist[v] = 0;
            }
        }
        return dist;
    }

    /** Directed path 0 -> 1 -> ... -> n-1. */
    public static Graph directedPath(int n) {
        Graph g = new Graph(n, true, Math.max(0, n - 1));
        for (int v = 0; v + 1 < n; v++) {
            g.addEdge(v, v + 1, 1);
        }
        return g;
    }
}
