package graph.benchmark;

import java.util.List;
import java.util.Optional;

/**
 * The reference table of the assignment (section 7). These are <b>expected</b> values used
 * only to check the computed ones; the benchmark always prints them next to the computed value
 * and fails if they differ.
 */
public final class ReferenceValues {

    /**
     * One row of the reference table.
     *
     * @param n           vertices
     * @param m           edges
     * @param mstWeight   expected MST total weight (undirected graph)
     * @param sccCount    expected number of strongly connected components (directed graph)
     * @param dagDistSum  expected sum of all super-source distances (DAG)
     */
    public record Expected(int n, int m, long mstWeight, int sccCount, long dagDistSum) {
    }

    private static final List<Expected> TABLE = List.of(
            new Expected(1_000, 4_000, 147_970L, 27, 396_274L),
            new Expected(1_000, 20_000, 30_363L, 1, 252_411L),
            new Expected(3_162, 12_648, 468_911L, 137, 1_271_911L),
            new Expected(3_162, 63_240, 97_589L, 1, 723_157L),
            new Expected(10_000, 40_000, 1_486_139L, 373, 3_941_828L),
            new Expected(10_000, 200_000, 309_550L, 1, 2_381_342L),
            new Expected(31_623, 126_492, 4_715_515L, 1_188, 12_551_891L),
            new Expected(31_623, 632_460, 968_517L, 1, 7_376_612L),
            new Expected(100_000, 400_000, 15_012_094L, 3_923, 40_096_539L),
            new Expected(100_000, 2_000_000, 3_056_681L, 1, 23_512_087L));

    private ReferenceValues() {
    }

    /** @return all ten rows of the table */
    public static List<Expected> all() {
        return TABLE;
    }

    /** @return the row for {@code (n, m)}, if the table has one */
    public static Optional<Expected> lookup(int n, int m) {
        return TABLE.stream().filter(e -> e.n() == n && e.m() == m).findFirst();
    }
}
