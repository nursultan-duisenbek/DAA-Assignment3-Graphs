package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.GraphReader;
import graph.TestGraphs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bonus B: condensation and critical (longest) path. */
class CriticalPathTest {

    /** Longest path ending anywhere, by brute force over all paths (exponential, tiny DAGs only). */
    private static long bruteForceLongest(Graph dag) {
        long best = 0;
        for (int s = 0; s < dag.vertexCount(); s++) {
            best = Math.max(best, longestFrom(dag, s));
        }
        return best;
    }

    private static long longestFrom(Graph dag, int u) {
        long best = 0;
        for (Edge e : dag.adjacent(u)) {
            best = Math.max(best, e.weight() + longestFrom(dag, e.to()));
        }
        return best;
    }

    @Test
    void sampleDagCriticalPath() throws IOException {
        Graph dag = GraphReader.read(TestGraphs.DATA.resolve("pipeline_dag.txt"));
        CriticalPath.Result r = CriticalPath.run(dag);
        assertEquals(11, r.length());
        assertEquals(7, r.componentCount(), "every vertex of a DAG is its own component");
        assertEquals(2, r.edges().size());
        assertEquals(0, r.edges().get(0).from());
        assertEquals(3, r.edges().get(0).to());
        assertEquals(6, r.edges().get(1).to());
    }

    @Test
    void matchesBruteForceOnRandomPositiveDags() {
        Random rnd = new Random(61);
        for (int t = 0; t < 60; t++) {
            int n = 1 + rnd.nextInt(12);
            Graph dag = TestGraphs.randomDag(n, rnd.nextInt(2 * n + 1), rnd, 1, 20);
            CriticalPath.Result r = CriticalPath.run(dag);
            assertEquals(bruteForceLongest(dag), r.length(), "graph " + t);
            assertEquals(r.length(), r.edges().stream().mapToLong(Edge::weight).sum(),
                    "the reported edges add up to the length");
        }
    }

    @Test
    void condensationIsAlwaysADag() {
        Random rnd = new Random(62);
        for (int t = 0; t < 50; t++) {
            Graph g = TestGraphs.randomDirected(1 + rnd.nextInt(80), rnd.nextInt(200), rnd);
            SccResult scc = Kosaraju.run(g);
            Condensation c = Condensation.build(g, scc);
            assertEquals(scc.count(), c.dag().vertexCount());
            assertTrue(TopologicalSort.run(c.dag()).isAcyclic());
            for (Edge e : c.dag().edges()) {
                Edge original = g.edges().get(c.originalEdge(e.id()));
                assertEquals(e.weight(), original.weight());
                assertEquals(e.from(), scc.componentOf(original.from()));
                assertEquals(e.to(), scc.componentOf(original.to()));
            }
        }
    }

    @Test
    void cyclicGraphIsCondensedFirst() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("jobs_cyclic.txt"));
        CriticalPath.Result r = CriticalPath.run(g);
        // Components: {0} -> {1,2,3} -> {4,5} -> {6} -> {7}; crossing edges 0->1 (2), 3->4 (2), 5->6 (3), 6->7 (2)
        assertEquals(5, r.componentCount());
        assertEquals(9, r.length());
        assertEquals(List.of(2, 2, 3, 2), r.edges().stream().map(Edge::weight).toList());
    }
}
