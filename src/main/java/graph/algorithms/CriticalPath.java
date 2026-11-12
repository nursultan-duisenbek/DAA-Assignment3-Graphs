package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.metrics.Metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bonus B: the critical (longest) path. The graph is condensed into a DAG of strongly connected
 * components, then the <b>longest</b> path is found by relaxing edges in topological order with
 * the sign of every weight flipped: the shortest path with weights {@code -w} is the longest
 * path with weights {@code w}. In a DAG this is O(V + E); in a general graph longest simple
 * path is NP-hard (a positive cycle could be walked forever, and forbidding repeated vertices
 * turns it into Hamiltonian path).
 *
 * <p>All vertices with in-degree 0 in the condensation start at 0 (virtual super-source), so
 * the result is the heaviest chain of dependencies anywhere in the graph - the critical path
 * of a job schedule.
 */
public final class CriticalPath {

    /**
     * @param length     total weight of the critical path
     * @param components component ids along the path, from its start to its end
     * @param edges      the original edges that connect consecutive components of the path
     * @param componentCount number of strongly connected components (vertices of the condensation)
     */
    public record Result(long length, List<Integer> components, List<Edge> edges, int componentCount) {
    }

    private CriticalPath() {
    }

    /** Computes the critical path of {@code graph} (any directed graph, cycles allowed). */
    public static Result run(Graph graph, Metrics metrics) {
        SccResult scc = Kosaraju.run(graph, new Metrics());
        Condensation condensation = Condensation.build(graph, scc);
        Graph dag = condensation.dag();

        Graph negated = new Graph(dag.vertexCount(), true, dag.edgeCount());
        for (Edge e : dag.edges()) {
            if (e.weight() == Integer.MIN_VALUE) {
                throw new ArithmeticException("weight " + e.weight() + " cannot be negated");
            }
            negated.addEdge(e.from(), e.to(), -e.weight());
        }
        TopoResult topo = TopologicalSort.run(negated, new Metrics());
        DagPathResult shortest = DagShortestPaths.run(negated, topo.order(), metrics);

        if (dag.vertexCount() == 0) {
            return new Result(0, Collections.emptyList(), Collections.emptyList(), 0);
        }
        int end = 0;
        for (int c = 1; c < dag.vertexCount(); c++) {
            if (shortest.distance(c) < shortest.distance(end)) {
                end = c;                                       // most negative = longest
            }
        }
        List<Integer> path = shortest.pathTo(end);
        List<Edge> edges = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            int condensationEdge = shortest.parentEdge(path.get(i));
            edges.add(graph.edges().get(condensation.originalEdge(condensationEdge)));
        }
        return new Result(-shortest.distance(end), path, edges, dag.vertexCount());
    }

    /** Runs without collecting metrics. */
    public static Result run(Graph graph) {
        return run(graph, new Metrics());
    }
}
