package graph.metrics;

/**
 * Operation counters filled in <em>inside</em> the algorithms while they run (never computed
 * from a formula afterwards). With the deterministic generator they are identical on every
 * machine, which makes them the graded evidence instead of wall-clock time.
 *
 * <ul>
 *   <li>{@code dsuOps} - DSU {@code find} calls plus successful {@code union} calls;</li>
 *   <li>{@code edgeScans} - adjacency-list entries / edges looked at;</li>
 *   <li>{@code comparisons} - weight comparisons (sorting, priority queue, relaxation);</li>
 *   <li>{@code maxDepth} - deepest size the explicit DFS stack ever reached.</li>
 * </ul>
 *
 * {@code dsuPointerHops} is an extra diagnostic (parent links followed inside {@code find})
 * used in the report to illustrate the almost-constant alpha(n) cost; it is not a CSV column.
 */
public final class Metrics {

    private long dsuOps;
    private long edgeScans;
    private long comparisons;
    private int maxDepth;
    private long dsuPointerHops;

    /** Adds DSU operations (find calls + successful unions). */
    public void addDsuOps(long count) {
        dsuOps += count;
    }

    /** Adds parent links followed by DSU finds (diagnostic only). */
    public void addDsuPointerHops(long count) {
        dsuPointerHops += count;
    }

    /** Counts one edge (adjacency entry) looked at. */
    public void incEdgeScans() {
        edgeScans++;
    }

    /** Counts one weight comparison. */
    public void incComparisons() {
        comparisons++;
    }

    /** Records the current size of an explicit stack; keeps the maximum. */
    public void recordDepth(int depth) {
        if (depth > maxDepth) {
            maxDepth = depth;
        }
    }

    public long dsuOps() {
        return dsuOps;
    }

    public long edgeScans() {
        return edgeScans;
    }

    public long comparisons() {
        return comparisons;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public long dsuPointerHops() {
        return dsuPointerHops;
    }

    /** Resets every counter to zero. */
    public void reset() {
        dsuOps = 0;
        edgeScans = 0;
        comparisons = 0;
        maxDepth = 0;
        dsuPointerHops = 0;
    }

    /** @return {@code true} if all counters equal those of {@code other} */
    public boolean sameCountsAs(Metrics other) {
        return dsuOps == other.dsuOps
                && edgeScans == other.edgeScans
                && comparisons == other.comparisons
                && maxDepth == other.maxDepth
                && dsuPointerHops == other.dsuPointerHops;
    }

    @Override
    public String toString() {
        return "Metrics{dsuOps=" + dsuOps + ", edgeScans=" + edgeScans + ", comparisons=" + comparisons
                + ", maxDepth=" + maxDepth + "}";
    }
}
