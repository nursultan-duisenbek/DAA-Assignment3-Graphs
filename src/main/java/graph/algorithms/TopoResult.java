package graph.algorithms;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Result of Kahn's algorithm: either a topological order, or the report that the graph has a
 * cycle. A cyclic graph never yields an order - {@link #order()} throws instead of returning
 * a wrong one.
 */
public final class TopoResult {

    private final int vertexCount;
    private final int[] processed;      // vertices in the order they left the queue
    private final List<Integer> cycle;  // one directed cycle, empty for a DAG

    TopoResult(int vertexCount, int[] processed, List<Integer> cycle) {
        this.vertexCount = vertexCount;
        this.processed = processed;
        this.cycle = Collections.unmodifiableList(cycle);
    }

    /** @return {@code true} if every vertex left the queue, i.e. the graph is a DAG */
    public boolean isAcyclic() {
        return processed.length == vertexCount;
    }

    /** @return {@code true} if fewer than {@code n} vertices left the queue */
    public boolean hasCycle() {
        return !isAcyclic();
    }

    /**
     * @return a copy of the topological order (every edge {@code u -> v} has {@code u} first)
     * @throws IllegalStateException if the graph has a cycle
     */
    public int[] order() {
        if (hasCycle()) {
            throw new IllegalStateException("graph has a cycle, no topological order exists: " + cycle);
        }
        return processed.clone();
    }

    /** @return number of vertices that left the queue ({@code n} for a DAG) */
    public int processedCount() {
        return processed.length;
    }

    /**
     * @return the vertices of one directed cycle {@code c0 -> c1 -> ... -> c0} (the first vertex
     *         is not repeated), or an empty list for a DAG
     */
    public List<Integer> cycle() {
        return cycle;
    }

    @Override
    public String toString() {
        return isAcyclic()
                ? "TopoResult{order=" + Arrays.toString(processed) + "}"
                : "TopoResult{cycle=" + cycle + ", processed=" + processed.length + "/" + vertexCount + "}";
    }
}
