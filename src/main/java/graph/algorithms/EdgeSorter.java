package graph.algorithms;

import graph.Edge;
import graph.metrics.Metrics;

/**
 * Stable bottom-up merge sort of edges by weight that counts every weight comparison.
 *
 * <p>Written by hand (instead of {@code Arrays.sort}) for two reasons: the assignment asks for
 * self-written algorithms, and the comparison count must not depend on the JDK's internal
 * sorting strategy. It is iterative (no recursion) and makes at most
 * {@code E * ceil(log2 E)} comparisons, using O(E) extra space.
 */
public final class EdgeSorter {

    private EdgeSorter() {
    }

    /**
     * Sorts {@code edges} in non-decreasing order of weight; equal weights keep their order.
     *
     * @param edges   array to sort in place
     * @param metrics receives one {@code comparison} per weight comparison
     */
    public static void sortByWeight(Edge[] edges, Metrics metrics) {
        int n = edges.length;
        if (n < 2) {
            return;
        }
        Edge[] src = edges;
        Edge[] dst = new Edge[n];
        for (long width = 1; width < n; width <<= 1) {
            for (long lo = 0; lo < n; lo += width << 1) {
                int mid = (int) Math.min(lo + width, n);
                int hi = (int) Math.min(lo + (width << 1), n);
                merge(src, dst, (int) lo, mid, hi, metrics);
            }
            Edge[] t = src;
            src = dst;
            dst = t;
        }
        if (src != edges) {
            System.arraycopy(src, 0, edges, 0, n);
        }
    }

    private static void merge(Edge[] src, Edge[] dst, int lo, int mid, int hi, Metrics metrics) {
        int i = lo;
        int j = mid;
        for (int k = lo; k < hi; k++) {
            if (i < mid && j < hi) {
                metrics.incComparisons();
                if (src[j].weight() < src[i].weight()) {
                    dst[k] = src[j++];
                } else {
                    dst[k] = src[i++];         // ties take the left run: stable
                }
            } else if (i < mid) {
                dst[k] = src[i++];
            } else {
                dst[k] = src[j++];
            }
        }
    }
}
