package graph.algorithms;

import graph.Graph;
import graph.GraphReader;
import graph.TestGraphs;
import graph.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SccTest {

    private static void assertMatchesBruteForce(Graph g, SccResult r) {
        int[] rep = TestGraphs.bruteForceSccRepresentatives(g);
        assertEquals(TestGraphs.countDistinct(rep), r.count(), "component count");
        for (int u = 0; u < g.vertexCount(); u++) {
            for (int v = 0; v < g.vertexCount(); v++) {
                assertEquals(rep[u] == rep[v], r.sameComponent(u, v), "vertices " + u + " and " + v);
            }
        }
    }

    @Test
    void kosarajuMatchesBruteForceOnRandomGraphs() {
        Random rnd = new Random(31);
        for (int t = 0; t < 80; t++) {
            int n = 1 + rnd.nextInt(200);
            int m = rnd.nextInt(3 * n);
            Graph g = TestGraphs.randomDirected(n, m, rnd);
            Metrics metrics = new Metrics();
            SccResult r = Kosaraju.run(g, metrics);
            assertMatchesBruteForce(g, r);
            assertEquals(2L * m, metrics.edgeScans(), "Kosaraju scans every edge once per pass");
            assertTrue(metrics.maxDepth() <= n);
        }
    }

    @Test
    void tarjanMatchesKosarajuAndBruteForce() {
        Random rnd = new Random(32);
        for (int t = 0; t < 80; t++) {
            int n = 1 + rnd.nextInt(200);
            int m = rnd.nextInt(3 * n);
            Graph g = TestGraphs.randomDirected(n, m, rnd);
            Metrics metrics = new Metrics();
            SccResult tarjan = TarjanScc.run(g, metrics);
            assertMatchesBruteForce(g, tarjan);
            assertTrue(tarjan.samePartitionAs(Kosaraju.run(g)), "same components as Kosaraju");
            assertEquals(m, metrics.edgeScans(), "Tarjan needs a single pass");
        }
    }

    @Test
    void cyclicSampleGraph() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("jobs_cyclic.txt"));
        for (SccResult r : List.of(Kosaraju.run(g), TarjanScc.run(g))) {
            assertEquals(5, r.count());
            assertTrue(r.sameComponent(1, 2) && r.sameComponent(2, 3), "deadlock 1 -> 2 -> 3 -> 1");
            assertTrue(r.sameComponent(4, 5));
            assertFalse(r.sameComponent(0, 1));
            assertFalse(r.sameComponent(3, 4));
            assertFalse(r.sameComponent(6, 7));
            assertEquals(3, r.largestComponentSize());
        }
    }

    @Test
    void kosarajuNumbersComponentsInTopologicalOrderOfTheCondensation() {
        Random rnd = new Random(33);
        Graph g = TestGraphs.randomDirected(150, 220, rnd);
        SccResult r = Kosaraju.run(g);
        g.edges().forEach(e -> assertTrue(r.componentOf(e.from()) <= r.componentOf(e.to()),
                "edges between components go forward in Kosaraju's numbering"));
    }

    @Test
    void stackSafetyOnAPathOf100000Vertices() {
        int n = 100_000;
        Graph path = TestGraphs.directedPath(n);
        Metrics metrics = new Metrics();
        SccResult r = assertDoesNotThrow(() -> Kosaraju.run(path, metrics), "no StackOverflowError");
        assertEquals(n, r.count(), "every vertex of a path is its own component");
        assertEquals(n, metrics.maxDepth(), "explicit stack reaches depth n on a path");
        assertEquals(2L * (n - 1), metrics.edgeScans());

        Metrics tm = new Metrics();
        SccResult t = assertDoesNotThrow(() -> TarjanScc.run(path, tm));
        assertEquals(n, t.count());
        assertEquals(n, tm.maxDepth());
    }

    @Test
    void stackSafetyOnACycleOf100000Vertices() {
        int n = 100_000;
        Graph cycle = TestGraphs.directedPath(n);
        cycle.addEdge(n - 1, 0, 1);
        assertEquals(1, Kosaraju.run(cycle).count());
        assertEquals(1, TarjanScc.run(cycle).count());
    }

    @Test
    void undirectedGraphGivesConnectedComponents() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("disconnected_roads.txt"));
        assertEquals(4, Kosaraju.run(g).count());
        assertEquals(4, TarjanScc.run(g).count());
    }

    @Test
    void edgeCases() {
        assertEquals(0, Kosaraju.run(Graph.directed(0)).count());
        Graph loops = Graph.directed(3);
        loops.addEdge(0, 0, 1);
        loops.addEdge(1, 1, 1);
        loops.addEdge(0, 1, 1);
        loops.addEdge(0, 1, 1);
        SccResult r = Kosaraju.run(loops);
        assertEquals(3, r.count(), "self-loops and parallel edges do not merge components");
        assertTrue(r.samePartitionAs(TarjanScc.run(loops)));
        assertEquals(List.of(List.of(0), List.of(1), List.of(2)),
                List.of(r.components().get(r.componentOf(0)), r.components().get(r.componentOf(1)),
                        r.components().get(r.componentOf(2))));
    }
}
