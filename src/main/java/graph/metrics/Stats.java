package graph.metrics;

import java.util.Arrays;

/** Small statistics helpers for the benchmark: median and log-log slope. */
public final class Stats {

    private Stats() {
    }

    /** @return the median of {@code values} (mean of the two middle values for even length) */
    public static double median(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("median of an empty array");
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2.0;
    }

    /**
     * Least-squares slope of {@code log(y)} against {@code log(x)}. On log-log axes a slope of
     * 1 means linear growth, 2 quadratic; {@code n log n} shows up as slightly above 1.
     */
    public static double logLogSlope(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) {
            throw new IllegalArgumentException("need at least two (x, y) pairs");
        }
        int k = x.length;
        double sx = 0;
        double sy = 0;
        for (int i = 0; i < k; i++) {
            if (x[i] <= 0 || y[i] <= 0) {
                throw new IllegalArgumentException("log-log fit needs positive values");
            }
            sx += Math.log(x[i]);
            sy += Math.log(y[i]);
        }
        double mx = sx / k;
        double my = sy / k;
        double num = 0;
        double den = 0;
        for (int i = 0; i < k; i++) {
            double dx = Math.log(x[i]) - mx;
            num += dx * (Math.log(y[i]) - my);
            den += dx * dx;
        }
        return num / den;
    }
}
