package graph.algorithms;

import graph.Edge;
import graph.Graph;

/**
 * Bonus B: the condensation of a directed graph - every strongly connected component becomes
 * one vertex, and every edge between two different components becomes an edge between their
 * vertices (with the same weight). Edges inside a component disappear. The result is always a
 * DAG: a cycle between components would make them one component.
 */
public final class Condensation {

    private final Graph dag;
    private final SccResult components;
    private final int[] originalEdgeOf;   // condensation edge id -> original edge id

    private Condensation(Graph dag, SccResult components, int[] originalEdgeOf) {
        this.dag = dag;
        this.components = components;
        this.originalEdgeOf = originalEdgeOf;
    }

    /** Builds the condensation of {@code graph} from its components. */
    public static Condensation build(Graph graph, SccResult scc) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("condensation needs a directed graph");
        }
        int crossing = 0;
        for (Edge e : graph.edges()) {
            if (!scc.sameComponent(e.from(), e.to())) {
                crossing++;
            }
        }
        Graph dag = new Graph(scc.count(), true, crossing);
        int[] originalEdgeOf = new int[crossing];
        for (Edge e : graph.edges()) {
            int cu = scc.componentOf(e.from());
            int cv = scc.componentOf(e.to());
            if (cu != cv) {
                Edge added = dag.addEdge(cu, cv, e.weight());
                originalEdgeOf[added.id()] = e.id();
            }
        }
        return new Condensation(dag, scc, originalEdgeOf);
    }

    /** @return the condensation DAG (vertex = component id) */
    public Graph dag() {
        return dag;
    }

    /** @return the components the condensation was built from */
    public SccResult components() {
        return components;
    }

    /** @return id of the original edge behind condensation edge {@code condensationEdgeId} */
    public int originalEdge(int condensationEdgeId) {
        return originalEdgeOf[condensationEdgeId];
    }
}
