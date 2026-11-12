package graph.algorithms;

import graph.Edge;
import graph.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeSorterTest {

    @Test
    void sortsStablyAndCountsComparisons() {
        Random rnd = new Random(11);
        for (int t = 0; t < 30; t++) {
            int n = rnd.nextInt(500);
            Edge[] edges = new Edge[n];
            for (int i = 0; i < n; i++) {
                edges[i] = new Edge(i, 0, 0, rnd.nextInt(20) - 5);
            }
            Edge[] expected = edges.clone();
            Arrays.sort(expected, Comparator.comparingInt(Edge::weight)); // stable reference
            Metrics m = new Metrics();
            EdgeSorter.sortByWeight(edges, m);
            assertArrayEquals(expected, edges, "same order as a stable sort, including ties");
            long bound = n < 2 ? 0 : (long) n * (64 - Long.numberOfLeadingZeros(n - 1));
            assertTrue(m.comparisons() <= bound, "at most n * ceil(log2 n) comparisons");
        }
    }

    @Test
    void trivialArrays() {
        Metrics m = new Metrics();
        EdgeSorter.sortByWeight(new Edge[0], m);
        EdgeSorter.sortByWeight(new Edge[] {new Edge(0, 0, 1, 5)}, m);
        assertEquals(0, m.comparisons());
    }
}
