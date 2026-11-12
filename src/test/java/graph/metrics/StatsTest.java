package graph.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsTest {

    @Test
    void medianOfOddAndEvenLength() {
        assertEquals(3.0, Stats.median(new double[] {9, 1, 3, 7, 2}));
        assertEquals(2.5, Stats.median(new double[] {4, 1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> Stats.median(new double[0]));
    }

    @Test
    void logLogSlopeRecognisesPowerLaws() {
        double[] n = {1000, 3162, 10000, 31623, 100000};
        double[] linear = new double[n.length];
        double[] quadratic = new double[n.length];
        double[] nLogN = new double[n.length];
        for (int i = 0; i < n.length; i++) {
            linear[i] = 4 * n[i];
            quadratic[i] = n[i] * n[i];
            nLogN[i] = n[i] * Math.log(n[i]);
        }
        assertEquals(1.0, Stats.logLogSlope(n, linear), 1e-12);
        assertEquals(2.0, Stats.logLogSlope(n, quadratic), 1e-12);
        double s = Stats.logLogSlope(n, nLogN);
        assertTrue(s > 1.05 && s < 1.15, "n log n is slightly steeper than linear: " + s);
        assertThrows(IllegalArgumentException.class, () -> Stats.logLogSlope(new double[] {1}, new double[] {1}));
        assertThrows(IllegalArgumentException.class,
                () -> Stats.logLogSlope(new double[] {1, 2}, new double[] {0, 1}));
    }

    @Test
    void metricsCountAndCompare() {
        Metrics a = new Metrics();
        a.incEdgeScans();
        a.incComparisons();
        a.addDsuOps(3);
        a.recordDepth(5);
        a.recordDepth(2);
        assertEquals(1, a.edgeScans());
        assertEquals(1, a.comparisons());
        assertEquals(3, a.dsuOps());
        assertEquals(5, a.maxDepth(), "keeps the maximum depth");
        Metrics b = new Metrics();
        assertFalse(a.sameCountsAs(b));
        a.reset();
        assertTrue(a.sameCountsAs(b));
    }
}
