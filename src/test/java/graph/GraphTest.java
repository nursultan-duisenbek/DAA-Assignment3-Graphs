package graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    @Test
    void directedEdgeIsStoredOnceAndCountsInDegree() {
        Graph g = Graph.directed(3);
        Edge e = g.addEdge(0, 2, 7);
        assertTrue(g.isDirected());
        assertEquals(1, g.edgeCount());
        assertEquals(List.of(e), g.adjacent(0));
        assertTrue(g.adjacent(2).isEmpty());
        assertEquals(1, g.inDegree(2));
        assertEquals(0, g.inDegree(0));
        assertEquals(2, e.other(0));
    }

    @Test
    void undirectedEdgeIsSharedByBothEndpoints() {
        Graph g = Graph.undirected(3);
        Edge e = g.addEdge(0, 1, 5);
        assertSame(e, g.adjacent(0).get(0));
        assertSame(e, g.adjacent(1).get(0));
        assertEquals(1, g.edgeCount());
        assertEquals(0, e.other(1));
        assertEquals(1, g.inDegree(1));
    }

    @Test
    void selfLoopsParallelEdgesAndIsolatedVerticesAreKept() {
        Graph g = Graph.undirected(4);
        Edge loop = g.addEdge(1, 1, 3);
        g.addEdge(0, 2, 1);
        g.addEdge(0, 2, 9);
        assertTrue(loop.isSelfLoop());
        assertEquals(1, loop.other(1));
        assertEquals(1, g.degree(1), "an undirected self-loop is one adjacency entry");
        assertEquals(2, g.degree(0), "parallel edges are both stored");
        assertEquals(0, g.degree(3), "isolated vertex");
        assertEquals(3, g.edgeCount());

        Graph d = Graph.directed(2);
        d.addEdge(1, 1, 4);
        assertEquals(1, d.inDegree(1));
        assertEquals(1, d.degree(1));
    }

    @Test
    void edgeIdsFollowInsertionOrder() {
        Graph g = Graph.directed(3);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 2);
        for (int i = 0; i < g.edgeCount(); i++) {
            assertEquals(i, g.edges().get(i).id());
        }
        assertSame(g.edges().get(1), g.edgeAt(1, 0));
    }

    @Test
    void reversedSwapsDirectionAndKeepsWeightsAndIds() {
        Graph g = Graph.directed(3);
        g.addEdge(0, 1, 4);
        g.addEdge(1, 2, -3);
        Graph r = g.reversed();
        assertEquals(2, r.edgeCount());
        assertEquals(0, r.adjacent(1).get(0).to(), "edge 0 -> 1 became 1 -> 0");
        Edge back = r.edges().get(1);
        assertEquals(2, back.from());
        assertEquals(1, back.to());
        assertEquals(-3, back.weight());
        assertEquals(1, back.id());
        assertEquals(1, r.inDegree(0));
        assertEquals(0, r.inDegree(2));
    }

    @Test
    void reversedUndirectedGraphIsAnEqualCopy() {
        Graph g = Graph.undirected(3);
        g.addEdge(0, 1, 2);
        Graph r = g.reversed();
        assertFalse(r.isDirected());
        assertEquals(g.edges(), r.edges());
    }

    @Test
    void inDegreesReturnsACopy() {
        Graph g = Graph.directed(2);
        g.addEdge(0, 1, 1);
        int[] in = g.inDegrees();
        in[1] = 42;
        assertEquals(1, g.inDegree(1));
    }

    @Test
    void invalidInputIsRejected() {
        Graph g = Graph.directed(2);
        assertThrows(IndexOutOfBoundsException.class, () -> g.addEdge(0, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> g.addEdge(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
        assertThrows(UnsupportedOperationException.class, () -> g.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> g.adjacent(0).clear());
        Edge e = g.addEdge(0, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> e.other(5));
    }

    @Test
    void emptyGraphWorks() {
        Graph g = Graph.undirected(0);
        assertEquals(0, g.vertexCount());
        assertEquals(0, g.edgeCount());
        assertEquals(0, g.reversed().vertexCount());
    }
}
