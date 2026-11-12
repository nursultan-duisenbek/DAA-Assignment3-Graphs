package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.GraphReader;
import graph.TestGraphs;
import graph.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DagShortestPathsTest {

    /** The restored path starts at a start vertex, uses real edges and adds up to the distance. */
    private static void assertPathIsRealAndTight(Graph g, DagPathResult r, int v, boolean superSource, int source) {
        List<Integer> path = r.pathTo(v);
        if (!r.isReachable(v)) {
            assertTrue(path.isEmpty());
            return;
        }
        assertEquals(v, path.get(path.size() - 1));
        int start = path.get(0);
        if (superSource) {
            assertEquals(0, g.inDegree(start), "path starts at an in-degree-0 vertex");
        } else {
            assertEquals(source, start);
        }
        long length = 0;
        for (int i = 1; i < path.size(); i++) {
            int x = path.get(i);
            Edge e = g.edges().get(r.parentEdge(x));
            assertEquals(path.get(i - 1), e.from());
            assertEquals(x, e.to());
            length += e.weight();
        }
        assertEquals(r.distance(v), length, "path weight equals the distance of " + v);
    }

    @Test
    void superSourceMatchesBellmanFordOnRandomDagsWithNegativeWeights() {
        Random rnd = new Random(51);
        for (int t = 0; t < 150; t++) {
            int n = 1 + rnd.nextInt(60);
            boolean negative = t % 2 == 0;
            Graph dag = TestGraphs.randomDag(n, rnd.nextInt(3 * n + 1), rnd, negative ? -30 : 1, 30);
            int[] order = TopologicalSort.run(dag).order();
            Metrics m = new Metrics();
            DagPathResult r = DagShortestPaths.run(dag, order, m);
            long[] expected = TestGraphs.bellmanFord(dag, TestGraphs.superSourceStart(dag));
            assertArrayEquals(expected, r.distances(), "graph " + t + (negative ? " (negative weights)" : ""));
            assertEquals(n, r.reachableCount(), "the super-source reaches the whole DAG");
            assertEquals(dag.edgeCount(), m.edgeScans(), "one pass: every edge relaxed once");
            assertEquals(dag.edgeCount(), m.comparisons());
            for (int v = 0; v < n; v++) {
                assertPathIsRealAndTight(dag, r, v, true, -1);
            }
        }
    }

    @Test
    void singleSourceMatchesBellmanFord() {
        Random rnd = new Random(52);
        for (int t = 0; t < 100; t++) {
            int n = 1 + rnd.nextInt(50);
            Graph dag = TestGraphs.randomDag(n, rnd.nextInt(3 * n + 1), rnd, -25, 25);
            int[] order = TopologicalSort.run(dag).order();
            int s = rnd.nextInt(n);
            DagPathResult r = DagShortestPaths.fromSource(dag, order, s, new Metrics());
            long[] start = new long[n];
            Arrays.fill(start, Long.MAX_VALUE);
            start[s] = 0;
            assertArrayEquals(TestGraphs.bellmanFord(dag, start), r.distances(), "graph " + t);
            for (int v = 0; v < n; v++) {
                assertPathIsRealAndTight(dag, r, v, false, s);
            }
        }
    }

    @Test
    void sampleDagWithNegativeWeights() throws IOException {
        Graph dag = GraphReader.read(TestGraphs.DATA.resolve("pipeline_dag.txt"));
        DagPathResult r = DagShortestPaths.run(dag);
        assertArrayEquals(new long[] {0, 0, 3, -2, 4, -1, 1}, r.distances());
        assertEquals(5, r.sumOfDistances());
        assertEquals(List.of(1, 4, 6), r.pathTo(6));
        assertEquals(List.of(1, 3, 5), r.pathTo(5));
        assertEquals(List.of(0), r.pathTo(0));
        assertEquals("1 -> 4 -> 6 (distance 1)", r.formatPath(6));
        assertEquals(-1, r.parent(1), "sources have no parent");
    }

    @Test
    void negativeWeightsAreSafeInADagButNotForDijkstraStyleGreed() {
        // 0 -> 1 costs 1, but the detour 0 -> 2 -> 1 costs 5 - 10 = -5.
        Graph g = Graph.directed(3);
        g.addEdge(0, 1, 1);
        g.addEdge(0, 2, 5);
        g.addEdge(2, 1, -10);
        DagPathResult r = DagShortestPaths.run(g);
        assertEquals(-5, r.distance(1));
        assertEquals(List.of(0, 2, 1), r.pathTo(1));
    }

    @Test
    void unreachableVerticesInSingleSourceMode() {
        Graph g = Graph.directed(3);
        g.addEdge(1, 2, 4);
        int[] order = TopologicalSort.run(g).order();
        DagPathResult r = DagShortestPaths.fromSource(g, order, 1, new Metrics());
        assertFalse(r.isReachable(0));
        assertEquals(DagPathResult.UNREACHABLE, r.distance(0));
        assertTrue(r.pathTo(0).isEmpty());
        assertEquals("0 is unreachable", r.formatPath(0));
        assertEquals(4, r.sumOfDistances(), "only reachable vertices are summed");
    }

    @Test
    void cyclicOrUndirectedInputIsRejected() throws IOException {
        Graph cyclic = GraphReader.read(TestGraphs.DATA.resolve("jobs_cyclic.txt"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DagShortestPaths.run(cyclic));
        assertTrue(e.getMessage().contains("cycle"));
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPaths.run(Graph.undirected(2), new int[] {0, 1}, new Metrics()));
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPaths.run(Graph.directed(3), new int[] {0, 1}, new Metrics()));
    }

    @Test
    void parallelEdgesUseTheCheaperOne() {
        Graph g = Graph.directed(2);
        g.addEdge(0, 1, 7);
        Edge cheap = g.addEdge(0, 1, 3);
        DagPathResult r = DagShortestPaths.run(g);
        assertEquals(3, r.distance(1));
        assertEquals(cheap.id(), r.parentEdge(1));
    }

    @Test
    void longChainHasNoRecursion() {
        int n = 100_000;
        Graph path = TestGraphs.directedPath(n);
        DagPathResult r = DagShortestPaths.run(path);
        assertEquals(n - 1, r.distance(n - 1));
        assertEquals(n, r.pathTo(n - 1).size());
    }
}
