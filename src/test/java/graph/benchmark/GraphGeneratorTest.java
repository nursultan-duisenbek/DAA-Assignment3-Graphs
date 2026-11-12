package graph.benchmark;

import graph.Edge;
import graph.Graph;
import graph.TestGraphs;
import graph.algorithms.TopologicalSort;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphGeneratorTest {

    @Test
    void undirectedGraphIsConnectedWithExactlyMEdges() {
        Graph g = GraphGenerator.undirected(1000, 4000);
        assertFalse(g.isDirected());
        assertEquals(1000, g.vertexCount());
        assertEquals(4000, g.edgeCount());
        assertEquals(1, TestGraphs.connectedComponents(g), "the random spanning tree connects everything");
        for (Edge e : g.edges()) {
            assertFalse(e.isSelfLoop());
            assertTrue(e.weight() >= 1 && e.weight() <= GraphGenerator.MAX_WEIGHT);
        }
    }

    @Test
    void directedGraphHasNoSelfLoops() {
        Graph g = GraphGenerator.directed(500, 2000);
        assertTrue(g.isDirected());
        assertEquals(2000, g.edgeCount());
        assertTrue(g.edges().stream().noneMatch(Edge::isSelfLoop));
    }

    @Test
    void dagIsAcyclic() {
        Graph g = GraphGenerator.dag(1000, 20000);
        assertEquals(20000, g.edgeCount());
        assertTrue(TopologicalSort.run(g).isAcyclic());
    }

    @Test
    void everyGraphUsesAFreshRandom42() {
        Graph a = GraphGenerator.directed(300, 1200);
        GraphGenerator.undirected(300, 1200);              // must not influence the next graph
        Graph b = GraphGenerator.directed(300, 1200);
        assertEquals(a.edges(), b.edges(), "deterministic: same seed, same edges");
        assertEquals(a.edges(), GraphGenerator.directed(300, 1200, new Random(42)).edges());
        assertEquals(a.edges(), GraphGenerator.generate(GraphGenerator.Kind.DIRECTED, 300, 1200).edges());
    }

    @Test
    void firstEdgesFollowTheSpecifiedDrawOrder() {
        // Re-derive the first spanning-tree edge and the first directed edge by hand from Random(42).
        Random r = new Random(42);
        int n = 10;
        int[] perm = GraphGenerator.shuffledIdentity(n, r);
        int a = perm[r.nextInt(1)];
        int w = 1 + r.nextInt(1000);
        Edge first = GraphGenerator.undirected(n, 20).edges().get(0);
        assertEquals(a, first.from());
        assertEquals(perm[1], first.to());
        assertEquals(w, first.weight(), "weight is drawn right after the endpoints");

        Random d = new Random(42);
        int u = d.nextInt(n);
        int v = d.nextInt(n);
        while (v == u) {
            v = d.nextInt(n);
        }
        int dw = 1 + d.nextInt(1000);
        Edge firstDirected = GraphGenerator.directed(n, 5).edges().get(0);
        assertArrayEquals(new int[] {u, v, dw},
                new int[] {firstDirected.from(), firstDirected.to(), firstDirected.weight()});
    }

    @Test
    void fisherYatesShuffleIsAPermutation() {
        int[] p = GraphGenerator.shuffledIdentity(1000, new Random(1));
        boolean[] seen = new boolean[1000];
        for (int x : p) {
            assertFalse(seen[x]);
            seen[x] = true;
        }
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.undirected(10, 5), "m < n-1");
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.directed(1, 3), "no loops possible");
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.dag(5, -1));
        assertEquals(0, GraphGenerator.undirected(1, 0).edgeCount());
    }
}
