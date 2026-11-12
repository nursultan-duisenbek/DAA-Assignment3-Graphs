package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Weighted graph stored as adjacency lists. One class serves both directed and undirected
 * graphs, so all algorithms (and the benchmark loop) work on a single type.
 *
 * <ul>
 *   <li>Vertices are numbered {@code 0 .. n-1}; weights are {@code int} and may be negative.</li>
 *   <li>Directed: edge {@code u -> v} is stored once, in the list of {@code u}.</li>
 *   <li>Undirected: the same {@link Edge} object is stored in the lists of {@code u} and
 *       {@code v}; a self-loop is stored once (it is a single edge).</li>
 *   <li>Self-loops, parallel edges, isolated vertices and disconnected graphs are allowed.</li>
 *   <li>In-degrees are maintained incrementally while edges are inserted (O(1) per edge),
 *       the same way the adjacency lists are maintained, so Kahn's algorithm can start from a
 *       copy in O(V) instead of re-reading every edge.</li>
 * </ul>
 *
 * <p>The class is not thread-safe. Adjacency lists are exposed as read-only views.
 */
public final class Graph {

    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<List<Edge>> adjacencyViews;
    private final List<Edge> edges;
    private final List<Edge> edgesView;
    private final int[] inDegree;

    /**
     * Creates a graph with {@code n} isolated vertices.
     *
     * @param n        number of vertices, {@code n >= 0}
     * @param directed {@code true} for a directed graph
     */
    public Graph(int n, boolean directed) {
        this(n, directed, 16);
    }

    /**
     * Creates a graph with {@code n} isolated vertices and room for {@code expectedEdges}
     * edges (only a capacity hint).
     *
     * @param n             number of vertices, {@code n >= 0}
     * @param directed      {@code true} for a directed graph
     * @param expectedEdges capacity hint for the edge list
     */
    public Graph(int n, boolean directed, int expectedEdges) {
        if (n < 0) {
            throw new IllegalArgumentException("vertex count must be >= 0, got " + n);
        }
        this.n = n;
        this.directed = directed;
        this.adjacency = new ArrayList<>(n);
        this.adjacencyViews = new ArrayList<>(n);
        for (int v = 0; v < n; v++) {
            List<Edge> list = new ArrayList<>(4);
            adjacency.add(list);
            adjacencyViews.add(Collections.unmodifiableList(list));
        }
        this.edges = new ArrayList<>(Math.max(0, expectedEdges));
        this.edgesView = Collections.unmodifiableList(edges);
        this.inDegree = new int[n];
    }

    /** @return a new directed graph with {@code n} vertices */
    public static Graph directed(int n) {
        return new Graph(n, true);
    }

    /** @return a new undirected graph with {@code n} vertices */
    public static Graph undirected(int n) {
        return new Graph(n, false);
    }

    /**
     * Adds the edge {@code u -> v} (or {@code u - v} for an undirected graph).
     *
     * @return the created edge; its id is its index in {@link #edges()}
     * @throws IndexOutOfBoundsException if an endpoint is not in {@code 0 .. n-1}
     */
    public Edge addEdge(int u, int v, int weight) {
        Objects.checkIndex(u, n);
        Objects.checkIndex(v, n);
        Edge e = new Edge(edges.size(), u, v, weight);
        edges.add(e);
        adjacency.get(u).add(e);
        if (directed) {
            inDegree[v]++;
        } else if (u != v) {
            adjacency.get(v).add(e);
        }
        return e;
    }

    /** @return number of vertices {@code n} */
    public int vertexCount() {
        return n;
    }

    /** @return number of edges {@code m} (an undirected edge counts once) */
    public int edgeCount() {
        return edges.size();
    }

    /** @return {@code true} if the graph is directed */
    public boolean isDirected() {
        return directed;
    }

    /**
     * Read-only adjacency list of {@code u}: outgoing edges for a directed graph, incident
     * edges for an undirected graph. Use {@link Edge#other(int)} to get the neighbour.
     */
    public List<Edge> adjacent(int u) {
        return adjacencyViews.get(u);
    }

    /** @return length of the adjacency list of {@code u} (out-degree when directed) */
    public int degree(int u) {
        return adjacency.get(u).size();
    }

    /** @return the {@code i}-th entry of the adjacency list of {@code u} */
    public Edge edgeAt(int u, int i) {
        return adjacency.get(u).get(i);
    }

    /**
     * In-degree of {@code v}. For an undirected graph every incident edge counts as incoming,
     * so the value equals {@link #degree(int)}.
     */
    public int inDegree(int v) {
        Objects.checkIndex(v, n);
        return directed ? inDegree[v] : adjacency.get(v).size();
    }

    /** @return a fresh copy of all in-degrees (see {@link #inDegree(int)}) */
    public int[] inDegrees() {
        if (directed) {
            return inDegree.clone();
        }
        int[] result = new int[n];
        for (int v = 0; v < n; v++) {
            result[v] = adjacency.get(v).size();
        }
        return result;
    }

    /** @return read-only list of all edges in insertion order */
    public List<Edge> edges() {
        return edgesView;
    }

    /**
     * Returns the transpose graph: every directed edge {@code u -> v} becomes {@code v -> u}
     * (same id and weight). For an undirected graph an equal copy is returned.
     */
    public Graph reversed() {
        Graph r = new Graph(n, directed, edges.size());
        for (Edge e : edges) {
            if (directed) {
                r.addEdge(e.to(), e.from(), e.weight());
            } else {
                r.addEdge(e.from(), e.to(), e.weight());
            }
        }
        return r;
    }

    @Override
    public String toString() {
        return (directed ? "directed" : "undirected") + " graph, n=" + n + ", m=" + edges.size();
    }
}
