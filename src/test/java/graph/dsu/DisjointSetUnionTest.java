package graph.dsu;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisjointSetUnionTest {

    /** The simplest possible reference: every element stores its set label; union relabels. */
    private static final class NaiveSets {
        final int[] label;

        NaiveSets(int n) {
            label = new int[n];
            for (int i = 0; i < n; i++) {
                label[i] = i;
            }
        }

        boolean same(int a, int b) {
            return label[a] == label[b];
        }

        boolean union(int a, int b) {
            int la = label[a];
            int lb = label[b];
            if (la == lb) {
                return false;
            }
            for (int i = 0; i < label.length; i++) {
                if (label[i] == lb) {
                    label[i] = la;
                }
            }
            return true;
        }

        int sets() {
            return (int) java.util.Arrays.stream(label).distinct().count();
        }
    }

    @RepeatedTest(50)
    void agreesWithNaiveReferenceAfterRandomUnions(RepetitionInfo info) {
        Random rnd = new Random(1000 + info.getCurrentRepetition());
        int n = 1 + rnd.nextInt(300);
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        NaiveSets ref = new NaiveSets(n);
        int successful = 0;
        for (int step = 0; step < 2 * n; step++) {
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n);
            boolean merged = dsu.union(a, b);
            assertEquals(ref.union(a, b), merged, "union result for " + a + "," + b);
            if (merged) {
                successful++;
            }
            int x = rnd.nextInt(n);
            int y = rnd.nextInt(n);
            assertEquals(ref.same(x, y), dsu.find(x) == dsu.find(y), "find agreement for " + x + "," + y);
        }
        assertEquals(ref.sets(), dsu.components(), "components() equals number of sets");
        assertEquals(n - successful, dsu.components());
        for (int x = 0; x < n; x++) {
            for (int y = x + 1; y < Math.min(n, x + 20); y++) {
                assertEquals(ref.same(x, y), dsu.connected(x, y));
            }
        }
        assertEquals(successful, dsu.unionCalls());
    }

    @Test
    void countsFindCallsAndOnlySuccessfulUnions() {
        DisjointSetUnion dsu = new DisjointSetUnion(4);
        assertEquals(0, dsu.operations());
        dsu.find(0);
        assertEquals(1, dsu.findCalls());
        assertTrue(dsu.union(0, 1));            // 2 finds + 1 successful union
        assertEquals(3, dsu.findCalls());
        assertEquals(1, dsu.unionCalls());
        assertFalse(dsu.union(1, 0));           // 2 finds, not a successful union
        assertEquals(5, dsu.findCalls());
        assertEquals(1, dsu.unionCalls());
        assertEquals(6, dsu.operations());
        assertEquals(3, dsu.components());
    }

    @Test
    void pathCompressionLinksVisitedNodesToTheRoot() {
        DisjointSetUnion dsu = new DisjointSetUnion(8);
        // Build a tree of height 3 with union by rank: pairs, then pairs of pairs, ...
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(4, 5);
        dsu.union(6, 7);
        dsu.union(0, 2);
        dsu.union(4, 6);
        dsu.union(0, 4);
        int root = dsu.find(0);
        int deepest = -1;
        int maxDepth = -1;
        for (int x = 0; x < 8; x++) {
            int depth = 0;
            for (int v = x; dsu.parentOf(v) != v; v = dsu.parentOf(v)) {
                depth++;
            }
            if (depth > maxDepth) {
                maxDepth = depth;
                deepest = x;
            }
        }
        assertTrue(maxDepth >= 2, "a deep node exists before compression");
        assertEquals(root, dsu.find(deepest));
        assertEquals(root, dsu.parentOf(deepest), "after find the node points straight to the root");
    }

    @Test
    void unionByRankKeepsTreesShallow() {
        int n = 1 << 12;
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        Random rnd = new Random(7);
        for (int i = 0; i < 3 * n; i++) {
            dsu.union(rnd.nextInt(n), rnd.nextInt(n));
        }
        int log2 = 12;
        for (int x = 0; x < n; x++) {
            assertTrue(dsu.rankOf(x) <= log2, "rank never exceeds log2(n)");
        }
        // A worst-case chain of unions still gives rank <= log2 n.
        DisjointSetUnion chain = new DisjointSetUnion(n);
        for (int i = 1; i < n; i++) {
            chain.union(i - 1, i);
        }
        assertTrue(chain.rankOf(chain.find(0)) <= log2);
        assertEquals(1, chain.components());
    }

    @Test
    void makeSetResetsEverything() {
        DisjointSetUnion dsu = new DisjointSetUnion(3);
        dsu.union(0, 1);
        dsu.makeSet(5);
        assertEquals(5, dsu.size());
        assertEquals(5, dsu.components());
        assertEquals(0, dsu.operations());
        assertFalse(dsu.connected(0, 1));
    }

    @Test
    void edgeCases() {
        DisjointSetUnion empty = new DisjointSetUnion(0);
        assertEquals(0, empty.components());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.find(0));
        assertThrows(IllegalArgumentException.class, () -> new DisjointSetUnion(-1));
        DisjointSetUnion one = new DisjointSetUnion(1);
        assertFalse(one.union(0, 0), "self-union is a no-op");
        assertEquals(1, one.components());
    }
}
