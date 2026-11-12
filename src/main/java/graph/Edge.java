package graph;

/**
 * An immutable weighted edge.
 *
 * <p>In a directed graph the edge goes from {@code from} to {@code to}. In an undirected
 * graph the very same object is stored in the adjacency lists of both endpoints, so
 * algorithms always walk an adjacency list with {@link #other(int)}, which works for both
 * kinds of graph.
 *
 * @param id     position of the edge in {@link Graph#edges()} (unique inside one graph)
 * @param from   tail vertex (first endpoint for undirected graphs)
 * @param to     head vertex (second endpoint for undirected graphs)
 * @param weight integer weight, may be negative or zero
 */
public record Edge(int id, int from, int to, int weight) {

    /**
     * Returns the endpoint opposite to {@code vertex}.
     *
     * @param vertex one endpoint of this edge
     * @return the other endpoint ({@code vertex} itself for a self-loop)
     * @throws IllegalArgumentException if {@code vertex} is not an endpoint of this edge
     */
    public int other(int vertex) {
        if (vertex == from) {
            return to;
        }
        if (vertex == to) {
            return from;
        }
        throw new IllegalArgumentException("vertex " + vertex + " is not an endpoint of " + this);
    }

    /** @return {@code true} if both endpoints are the same vertex */
    public boolean isSelfLoop() {
        return from == to;
    }

    @Override
    public String toString() {
        return from + " -" + weight + "-> " + to;
    }
}
