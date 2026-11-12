package graph.algorithms;

import graph.Edge;
import graph.Graph;
import graph.GraphReader;
import graph.TestGraphs;
import graph.dsu.DisjointSetUnion;
import graph.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MstTest {

    /** Checks that {@code edges} is a forest of the graph with exactly {@code n - components} edges. */
    private static void assertSpanningForest(Graph g, MstResult r) {
        DisjointSetUnion check = new DisjointSetUnion(g.vertexCount());
        long sum = 0;
        for (Edge e : r.edges()) {
            assertTrue(check.union(e.from(), e.to()), "chosen edges must not form a cycle: " + e);
            assertTrue(g.edges().contains(e), "chosen edge belongs to the graph");
            sum += e.weight();
        }
        assertEquals(sum, r.totalWeight(), "totalWeight is the sum of the chosen edges");
        assertEquals(TestGraphs.connectedComponents(g), r.components());
        assertEquals(g.vertexCount() - r.components(), r.edgeCount());
        assertEquals(r.components(), check.components(), "forest spans every component");
    }

    @Test
    void kruskalAndPrimAgreeOn100RandomConnectedGraphs() {
        Random rnd = new Random(2024);
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(150);
            int extra = rnd.nextInt(4 * n);
            Graph g = TestGraphs.randomConnected(n, extra, rnd, 1, t % 2 == 0 ? 10 : 1000);
            MstResult k = Kruskal.run(g);
            MstResult p = Prim.run(g);
            assertEquals(k.totalWeight(), p.totalWeight(), "same total weight on graph " + t);
            assertEquals(n - 1, k.edgeCount(), "Kruskal tree has n-1 edges");
            assertEquals(n - 1, p.edgeCount(), "Prim tree has n-1 edges");
            assertTrue(k.isSpanningTree() && p.isSpanningTree());
            assertSpanningForest(g, k);
            assertSpanningForest(g, p);
        }
    }

    @Test
    void matchesSlowReferenceIncludingNegativeWeights() {
        Random rnd = new Random(99);
        for (int t = 0; t < 60; t++) {
            int n = 1 + rnd.nextInt(40);
            Graph g = TestGraphs.randomUndirected(n, rnd.nextInt(3 * n + 1), rnd, -20, 20);
            long expected = TestGraphs.naiveMinimumForestWeight(g);
            assertEquals(expected, Kruskal.run(g).totalWeight(), "Kruskal vs O(VE) reference, graph " + t);
            assertEquals(expected, Prim.run(g).totalWeight(), "Prim vs O(VE) reference, graph " + t);
        }
    }

    @Test
    void exhaustiveCheckOnTinyGraphs() {
        Random rnd = new Random(5);
        for (int t = 0; t < 40; t++) {
            int n = 2 + rnd.nextInt(4);
            Graph g = TestGraphs.randomConnected(n, rnd.nextInt(6), rnd, 1, 9);
            List<Edge> edges = g.edges();
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << edges.size()); mask++) {
                if (Integer.bitCount(mask) != n - 1) {
                    continue;
                }
                DisjointSetUnion dsu = new DisjointSetUnion(n);
                long w = 0;
                boolean acyclic = true;
                for (int i = 0; i < edges.size() && acyclic; i++) {
                    if ((mask >> i & 1) == 1) {
                        acyclic = dsu.union(edges.get(i).from(), edges.get(i).to());
                        w += edges.get(i).weight();
                    }
                }
                if (acyclic) {
                    best = Math.min(best, w);
                }
            }
            assertEquals(best, Kruskal.run(g).totalWeight(), "minimum over all spanning trees");
            assertEquals(best, Prim.run(g).totalWeight());
        }
    }

    @Test
    void disconnectedGraphGivesAMinimumSpanningForest() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("disconnected_roads.txt"));
        MstResult k = assertDoesNotThrow(() -> Kruskal.run(g));
        MstResult p = assertDoesNotThrow(() -> Prim.run(g));
        for (MstResult r : List.of(k, p)) {
            assertEquals(10, r.totalWeight());
            assertEquals(4, r.components(), "{0,1,2}, {3,4,5}, {6}, {7}");
            assertEquals(4, r.edgeCount());
            assertFalse(r.isSpanningTree());
            assertSpanningForest(g, r);
        }
    }

    @Test
    void randomDisconnectedGraphsAgree() {
        Random rnd = new Random(17);
        for (int t = 0; t < 50; t++) {
            int n = 1 + rnd.nextInt(80);
            Graph g = TestGraphs.randomUndirected(n, rnd.nextInt(n + 1), rnd, 1, 50);
            MstResult k = Kruskal.run(g);
            MstResult p = Prim.run(g);
            assertEquals(k.totalWeight(), p.totalWeight());
            assertEquals(k.components(), p.components());
            assertSpanningForest(g, k);
            assertSpanningForest(g, p);
        }
    }

    @Test
    void cityRoadsWithParallelEdgeAndSelfLoop() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("city_roads.txt"));
        MstResult k = Kruskal.run(g);
        MstResult p = Prim.run(g);
        assertEquals(14, k.totalWeight());
        assertEquals(14, p.totalWeight());
        assertTrue(k.edges().stream().noneMatch(Edge::isSelfLoop));
        assertTrue(k.edges().stream().anyMatch(e -> e.from() == 0 && e.to() == 1 && e.weight() == 2),
                "the cheaper of the parallel edges is used");
    }

    @Test
    void trivialGraphs() {
        assertEquals(0, Kruskal.run(Graph.undirected(0)).totalWeight());
        assertEquals(0, Prim.run(Graph.undirected(0)).components());
        MstResult single = Prim.run(Graph.undirected(1));
        assertEquals(1, single.components());
        assertEquals(0, single.edgeCount());
        Graph isolated = Graph.undirected(3);
        assertEquals(3, Kruskal.run(isolated).components());
        assertEquals(3, Prim.run(isolated).components());
    }

    @Test
    void directedGraphIsRejected() {
        Graph d = Graph.directed(2);
        assertThrows(IllegalArgumentException.class, () -> Kruskal.run(d));
        assertThrows(IllegalArgumentException.class, () -> Prim.run(d));
    }

    @Test
    void countersAreCountedInsideTheAlgorithms() {
        Random rnd = new Random(3);
        Graph g = TestGraphs.randomConnected(200, 600, rnd, 1, 1000);
        // the random builder may add self-loops; an undirected self-loop is one adjacency entry
        long loops = g.edges().stream().filter(Edge::isSelfLoop).count();
        Metrics km = new Metrics();
        MstResult k = Kruskal.run(g, km);
        assertTrue(km.edgeScans() <= g.edgeCount());
        assertEquals(2 * km.edgeScans() + 3L * k.edgeCount(), km.dsuOps(),
                "two finds per examined edge, union (2 finds + 1) per accepted edge");
        assertTrue(km.comparisons() > 0);
        assertEquals(0, km.maxDepth());

        Metrics pm = new Metrics();
        Prim.run(g, pm);
        assertEquals(2L * g.edgeCount() - loops, pm.edgeScans(), "every adjacency entry once");
        assertTrue(pm.comparisons() > 0);
        assertEquals(0, pm.dsuOps());
    }

    @Test
    void kruskalStopsEarlyOnceTheTreeIsComplete() {
        Graph g = Graph.undirected(3);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 2);
        g.addEdge(0, 2, 3);
        g.addEdge(0, 2, 4);
        Metrics m = new Metrics();
        Kruskal.run(g, m);
        assertEquals(2, m.edgeScans(), "the two lightest edges already form the tree");
    }
}
