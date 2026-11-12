package graph.algorithms;

import graph.Graph;
import graph.GraphReader;
import graph.TestGraphs;
import graph.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologicalSortTest {

    /** Every consecutive pair of the reported cycle (and last -> first) must be an edge. */
    private static void assertIsDirectedCycle(Graph g, List<Integer> cycle) {
        assertFalse(cycle.isEmpty(), "a cycle must be reported");
        for (int i = 0; i < cycle.size(); i++) {
            int u = cycle.get(i);
            int v = cycle.get((i + 1) % cycle.size());
            assertTrue(g.adjacent(u).stream().anyMatch(e -> e.to() == v), "edge " + u + " -> " + v + " exists");
        }
    }

    @Test
    void validOrderOnRandomDags() {
        Random rnd = new Random(41);
        for (int t = 0; t < 100; t++) {
            int n = 1 + rnd.nextInt(300);
            Graph dag = TestGraphs.randomDag(n, rnd.nextInt(4 * n + 1), rnd, 1, 9);
            Metrics m = new Metrics();
            TopoResult r = TopologicalSort.run(dag, m);
            assertTrue(r.isAcyclic());
            assertTrue(r.cycle().isEmpty());
            int[] order = r.order();
            assertEquals(n, order.length);
            assertTrue(TopologicalSort.isValidOrder(dag, order), "pos[u] < pos[v] for every edge");
            assertEquals(dag.edgeCount(), m.edgeScans(), "Kahn scans every edge exactly once");
            assertEquals(0, m.comparisons());
        }
    }

    @Test
    void positionsRespectEveryEdgeOnTheSampleDag() throws IOException {
        Graph dag = GraphReader.read(TestGraphs.DATA.resolve("pipeline_dag.txt"));
        int[] order = TopologicalSort.run(dag).order();
        int[] pos = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            pos[order[i]] = i;
        }
        dag.edges().forEach(e -> assertTrue(pos[e.from()] < pos[e.to()], "edge " + e));
        assertArrayEquals(new int[] {0, 1, 2, 4, 3, 5, 6}, order, "FIFO queue, sources in index order");
    }

    @Test
    void cycleIsDetectedOnTheCyclicSampleGraph() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("jobs_cyclic.txt"));
        TopoResult r = TopologicalSort.run(g);
        assertTrue(r.hasCycle());
        assertEquals(1, r.processedCount(), "only job 0 can run before the deadlock");
        assertIsDirectedCycle(g, r.cycle());
        IllegalStateException e = assertThrows(IllegalStateException.class, r::order,
                "no wrong order is ever returned");
        assertTrue(e.getMessage().contains("cycle"));
    }

    @Test
    void cycleIsDetectedOnRandomCyclicGraphs() {
        Random rnd = new Random(42);
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(100);
            Graph g = TestGraphs.randomDag(n, rnd.nextInt(3 * n + 1), rnd, 1, 9);
            int[] order = TopologicalSort.run(g).order();
            // add one back edge from a later to an earlier vertex: now there is a cycle
            int i = rnd.nextInt(n - 1);
            int j = i + 1 + rnd.nextInt(n - 1 - i);
            if (!TestGraphs.reachable(g, order[i])[order[j]]) {
                g.addEdge(order[i], order[j], 1);           // make sure order[i] reaches order[j]
            }
            g.addEdge(order[j], order[i], 1);
            TopoResult r = TopologicalSort.run(g);
            assertTrue(r.hasCycle(), "graph " + t);
            assertTrue(r.processedCount() < n);
            assertIsDirectedCycle(g, r.cycle());
        }
    }

    @Test
    void selfLoopAndTwoCycleAreCycles() {
        Graph loop = Graph.directed(2);
        loop.addEdge(0, 1, 1);
        loop.addEdge(1, 1, 1);
        TopoResult r = TopologicalSort.run(loop);
        assertTrue(r.hasCycle());
        assertEquals(List.of(1), r.cycle());

        Graph two = Graph.directed(2);
        two.addEdge(0, 1, 1);
        two.addEdge(1, 0, 1);
        assertTrue(TopologicalSort.run(two).hasCycle());
        assertEquals(0, TopologicalSort.run(two).processedCount());
    }

    @Test
    void validatorRejectsWrongOrders() {
        Graph g = Graph.directed(3);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        assertTrue(TopologicalSort.isValidOrder(g, new int[] {0, 1, 2}));
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {1, 0, 2}), "edge 0 -> 1 violated");
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 1}), "missing vertex");
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 0, 2}), "not a permutation");
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 1, 3}), "out of range");
        assertFalse(TopologicalSort.isValidOrder(g, null));
    }

    @Test
    void edgeCases() {
        assertEquals(0, TopologicalSort.run(Graph.directed(0)).order().length);
        assertArrayEquals(new int[] {0, 1, 2}, TopologicalSort.run(Graph.directed(3)).order(),
                "isolated vertices in index order");
        assertThrows(IllegalArgumentException.class, () -> TopologicalSort.run(Graph.undirected(2)));
    }

    @Test
    void handlesALongChainWithoutRecursion() {
        int n = 100_000;
        Graph path = TestGraphs.directedPath(n);
        Metrics m = new Metrics();
        int[] order = TopologicalSort.run(path, m).order();
        assertEquals(n, order.length);
        assertEquals(n - 1, m.edgeScans());
        assertTrue(TopologicalSort.isValidOrder(path, order));
    }
}
