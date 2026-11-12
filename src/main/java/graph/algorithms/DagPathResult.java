package graph.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Distances and shortest-path tree computed by {@link DagShortestPaths}.
 *
 * <p>{@code parent[v]} is the predecessor of {@code v} on its shortest path ({@code -1} for a
 * source or an unreachable vertex) and {@code parentEdge[v]} the id of the edge used, so the
 * path is well defined even with parallel edges.
 */
public final class DagPathResult {

    /** Distance value of a vertex that cannot be reached. */
    public static final long UNREACHABLE = Long.MAX_VALUE;

    private final long[] dist;
    private final int[] parent;
    private final int[] parentEdge;

    DagPathResult(long[] dist, int[] parent, int[] parentEdge) {
        this.dist = dist;
        this.parent = parent;
        this.parentEdge = parentEdge;
    }

    /** @return number of vertices */
    public int vertexCount() {
        return dist.length;
    }

    /** @return shortest distance to {@code v}, or {@link #UNREACHABLE} */
    public long distance(int v) {
        return dist[v];
    }

    /** @return copy of all distances */
    public long[] distances() {
        return dist.clone();
    }

    /** @return {@code true} if {@code v} is reachable from a source */
    public boolean isReachable(int v) {
        return dist[v] != UNREACHABLE;
    }

    /** @return predecessor of {@code v} on its shortest path, {@code -1} for a source/unreachable */
    public int parent(int v) {
        return parent[v];
    }

    /** @return id of the edge that last improved {@code v}, {@code -1} for a source/unreachable */
    public int parentEdge(int v) {
        return parentEdge[v];
    }

    /**
     * Restores the shortest path by following {@code parent[]} back to a source.
     *
     * @return the vertices from the source to {@code v}, or an empty list if {@code v} is unreachable
     */
    public List<Integer> pathTo(int v) {
        if (!isReachable(v)) {
            return Collections.emptyList();
        }
        List<Integer> path = new ArrayList<>();
        for (int x = v; x != -1; x = parent[x]) {
            path.add(x);
        }
        Collections.reverse(path);
        return path;
    }

    /** @return printable path, e.g. {@code "0 -> 3 -> 7 (distance 12)"} */
    public String formatPath(int v) {
        if (!isReachable(v)) {
            return v + " is unreachable";
        }
        StringBuilder sb = new StringBuilder();
        for (int x : pathTo(v)) {
            if (sb.length() > 0) {
                sb.append(" -> ");
            }
            sb.append(x);
        }
        return sb.append(" (distance ").append(dist[v]).append(')').toString();
    }

    /** @return sum of the distances of all reachable vertices (the benchmark's result value) */
    public long sumOfDistances() {
        long sum = 0;
        for (long d : dist) {
            if (d != UNREACHABLE) {
                sum += d;
            }
        }
        return sum;
    }

    /** @return number of reachable vertices */
    public int reachableCount() {
        int count = 0;
        for (long d : dist) {
            if (d != UNREACHABLE) {
                count++;
            }
        }
        return count;
    }
}
