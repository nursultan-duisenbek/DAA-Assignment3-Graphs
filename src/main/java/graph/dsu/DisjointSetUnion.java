package graph.dsu;

import java.util.Objects;

/**
 * Disjoint Set Union (union-find) with <b>both</b> heuristics:
 * <ul>
 *   <li><b>union by rank</b> - the root of the lower-rank tree is attached under the root of
 *       the higher-rank tree, so a tree of rank r has at least 2^r nodes and height &lt;= log2 n;</li>
 *   <li><b>path compression</b> - every node visited by {@link #find(int)} is re-linked
 *       directly to the root.</li>
 * </ul>
 * Together they give O(alpha(n)) amortised time per operation, where alpha is the inverse
 * Ackermann function (alpha(n) &lt;= 4 for any realistic n).
 *
 * <p>Counters are kept inside the operations: every {@link #find(int)} call (including the two
 * made by {@link #union(int, int)}) and every <em>successful</em> union. {@link #find(int)} is
 * iterative, so it can never overflow the call stack.
 */
public final class DisjointSetUnion {

    private int[] parent;
    private int[] rank;
    private int components;

    private long findCalls;
    private long unionCalls;
    private long pointerHops;

    /** Creates {@code n} singleton sets {0}, {1}, ..., {n-1}. */
    public DisjointSetUnion(int n) {
        makeSet(n);
    }

    /**
     * (Re)initialises the structure with {@code n} singleton sets and resets the counters.
     *
     * @param n number of elements, {@code n >= 0}
     */
    public void makeSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + n);
        }
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        components = n;
        findCalls = 0;
        unionCalls = 0;
        pointerHops = 0;
    }

    /**
     * Returns the representative (root) of the set containing {@code x} and compresses the
     * path from {@code x} to the root.
     */
    public int find(int x) {
        Objects.checkIndex(x, parent.length);
        findCalls++;
        int root = x;
        while (parent[root] != root) {        // first pass: locate the root
            root = parent[root];
            pointerHops++;
        }
        while (parent[x] != root) {           // second pass: path compression
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /**
     * Merges the sets containing {@code x} and {@code y} using union by rank.
     *
     * @return {@code true} if two different sets were merged, {@code false} if {@code x} and
     *         {@code y} were already in the same set
     */
    public boolean union(int x, int y) {
        int rx = find(x);
        int ry = find(y);
        if (rx == ry) {
            return false;
        }
        if (rank[rx] < rank[ry]) {
            int t = rx;
            rx = ry;
            ry = t;
        }
        parent[ry] = rx;                       // lower rank goes under higher rank
        if (rank[rx] == rank[ry]) {
            rank[rx]++;
        }
        components--;
        unionCalls++;
        return true;
    }

    /** @return {@code true} if {@code x} and {@code y} are in the same set (costs two finds) */
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    /** @return current number of disjoint sets */
    public int components() {
        return components;
    }

    /** @return number of elements */
    public int size() {
        return parent.length;
    }

    /** @return number of {@link #find(int)} calls since the last {@link #makeSet(int)} */
    public long findCalls() {
        return findCalls;
    }

    /** @return number of successful {@link #union(int, int)} calls */
    public long unionCalls() {
        return unionCalls;
    }

    /** @return {@code findCalls() + unionCalls()} - the {@code dsuOps} metric */
    public long operations() {
        return findCalls + unionCalls;
    }

    /** @return parent links followed while locating roots (diagnostic for the alpha(n) cost) */
    public long pointerHops() {
        return pointerHops;
    }

    /** Package-private for tests: the current parent link of {@code x} (no compression). */
    int parentOf(int x) {
        return parent[x];
    }

    /** Package-private for tests: the rank of {@code x}. */
    int rankOf(int x) {
        return rank[x];
    }
}
