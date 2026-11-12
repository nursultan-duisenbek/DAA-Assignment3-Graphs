package graph.benchmark;

import graph.Graph;

import java.util.Random;

/**
 * The exact benchmark generator from the assignment. Every graph is built from its own
 * {@code new Random(42)}, and the random numbers are drawn in exactly this order (the order
 * matters: drawing the weights later, or redrawing both endpoints of a self-loop, gives other
 * graphs and other reference values):
 *
 * <ol>
 *   <li><b>undirected</b>: Fisher-Yates shuffle of {@code perm = [0..n-1]}
 *       ({@code for i = n-1..1: swap(perm[i], perm[nextInt(i+1)])}); then for
 *       {@code i = 1..n-1} the spanning-tree edge {@code (perm[nextInt(i)], perm[i])}; then the
 *       remaining {@code m - (n-1)} random edges {@code a = nextInt(n)}, {@code b = nextInt(n)}
 *       with {@code b} redrawn while {@code b == a};</li>
 *   <li><b>directed</b>: only the {@code m} random edges (no shuffle, no spanning tree);</li>
 *   <li><b>DAG</b>: Fisher-Yates shuffle of {@code rank[0..n-1]}, then {@code m} random edges,
 *       swapping the ends when {@code rank[a] > rank[b]} so every edge goes up in rank;</li>
 *   <li>every edge draws its weight {@code 1 + nextInt(1000)} immediately after its endpoints.</li>
 * </ol>
 */
public final class GraphGenerator {

    /** Seed prescribed by the assignment. */
    public static final long SEED = 42L;
    /** Weights are uniform in {@code 1 .. MAX_WEIGHT}. */
    public static final int MAX_WEIGHT = 1000;

    private GraphGenerator() {
    }

    /** The three graph kinds of the benchmark. */
    public enum Kind {
        UNDIRECTED("undirected"), DIRECTED("directed"), DAG("dag");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Generates a graph of the given kind with {@code new Random(42)}. */
    public static Graph generate(Kind kind, int n, int m) {
        return switch (kind) {
            case UNDIRECTED -> undirected(n, m);
            case DIRECTED -> directed(n, m);
            case DAG -> dag(n, m);
        };
    }

    /** Connected undirected graph: random spanning tree plus {@code m - (n-1)} random edges. */
    public static Graph undirected(int n, int m) {
        return undirected(n, m, new Random(SEED));
    }

    /** Directed graph with {@code m} random edges. */
    public static Graph directed(int n, int m) {
        return directed(n, m, new Random(SEED));
    }

    /** DAG with {@code m} random edges oriented by a random rank. */
    public static Graph dag(int n, int m) {
        return dag(n, m, new Random(SEED));
    }

    /** Same as {@link #undirected(int, int)} with a caller-supplied random source. */
    public static Graph undirected(int n, int m, Random rnd) {
        if (n < 1 || m < n - 1) {
            throw new IllegalArgumentException("need n >= 1 and m >= n-1 for a connected graph, got n="
                    + n + ", m=" + m);
        }
        checkRandomEdges(n, m - (n - 1));
        Graph g = new Graph(n, false, m);
        int[] perm = shuffledIdentity(n, rnd);
        for (int i = 1; i < n; i++) {                       // random spanning tree
            int a = perm[rnd.nextInt(i)];
            int b = perm[i];
            g.addEdge(a, b, weight(rnd));
        }
        for (int k = n - 1; k < m; k++) {                    // remaining random edges
            int a = rnd.nextInt(n);
            int b = otherEnd(a, n, rnd);
            g.addEdge(a, b, weight(rnd));
        }
        return g;
    }

    /** Same as {@link #directed(int, int)} with a caller-supplied random source. */
    public static Graph directed(int n, int m, Random rnd) {
        checkRandomEdges(n, m);
        Graph g = new Graph(n, true, m);
        for (int k = 0; k < m; k++) {
            int a = rnd.nextInt(n);
            int b = otherEnd(a, n, rnd);
            g.addEdge(a, b, weight(rnd));
        }
        return g;
    }

    /** Same as {@link #dag(int, int)} with a caller-supplied random source. */
    public static Graph dag(int n, int m, Random rnd) {
        checkRandomEdges(n, m);
        Graph g = new Graph(n, true, m);
        int[] rank = shuffledIdentity(n, rnd);
        for (int k = 0; k < m; k++) {
            int a = rnd.nextInt(n);
            int b = otherEnd(a, n, rnd);
            if (rank[a] > rank[b]) {                         // orient from lower to higher rank
                int t = a;
                a = b;
                b = t;
            }
            g.addEdge(a, b, weight(rnd));
        }
        return g;
    }

    /** Fisher-Yates: {@code for (i = n-1; i > 0; i--) swap(p[i], p[rnd.nextInt(i+1)])}. */
    static int[] shuffledIdentity(int n, Random rnd) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return p;
    }

    /** Draws {@code b = nextInt(n)} until {@code b != a}. */
    private static int otherEnd(int a, int n, Random rnd) {
        int b;
        do {
            b = rnd.nextInt(n);
        } while (b == a);
        return b;
    }

    private static int weight(Random rnd) {
        return 1 + rnd.nextInt(MAX_WEIGHT);
    }

    private static void checkRandomEdges(int n, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("edge count must be >= 0");
        }
        if (count > 0 && n < 2) {
            throw new IllegalArgumentException("random edges without self-loops need n >= 2");
        }
    }
}
