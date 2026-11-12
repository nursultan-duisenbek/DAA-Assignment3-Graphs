package graph.benchmark;

import graph.Graph;
import graph.algorithms.DagPathResult;
import graph.algorithms.DagShortestPaths;
import graph.algorithms.Kosaraju;
import graph.algorithms.Kruskal;
import graph.algorithms.MstResult;
import graph.algorithms.Prim;
import graph.algorithms.TopoResult;
import graph.algorithms.TopologicalSort;
import graph.metrics.Metrics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regenerates all ten benchmark graphs and checks the computed values against the reference
 * table of the assignment, plus the exact counter identities (2m / m / m).
 */
class ReferenceValuesTest {

    static List<ReferenceValues.Expected> rows() {
        return ReferenceValues.all();
    }

    @ParameterizedTest(name = "n={0}")
    @MethodSource("rows")
    void reproducesTheReferenceTable(ReferenceValues.Expected expected) {
        int n = expected.n();
        int m = expected.m();

        Graph undirected = GraphGenerator.undirected(n, m);
        MstResult kruskal = Kruskal.run(undirected);
        MstResult prim = Prim.run(undirected);
        assertEquals(expected.mstWeight(), kruskal.totalWeight(), "Kruskal MST weight");
        assertEquals(expected.mstWeight(), prim.totalWeight(), "Prim MST weight");
        assertEquals(n - 1, kruskal.edgeCount());
        undirected = null;

        Graph directed = GraphGenerator.directed(n, m);
        Metrics km = new Metrics();
        assertEquals(expected.sccCount(), Kosaraju.run(directed, km).count(), "SCC count");
        assertEquals(2L * m, km.edgeScans(), "Kosaraju scans 2m edges");
        directed = null;

        Graph dag = GraphGenerator.dag(n, m);
        Metrics tm = new Metrics();
        TopoResult topo = TopologicalSort.run(dag, tm);
        assertTrue(topo.isAcyclic());
        assertEquals((long) m, tm.edgeScans(), "Kahn scans m edges");
        Metrics pm = new Metrics();
        DagPathResult paths = DagShortestPaths.run(dag, topo.order(), pm);
        assertEquals(expected.dagDistSum(), paths.sumOfDistances(), "sum of DAG distances");
        assertEquals((long) m, pm.edgeScans(), "DAG relaxation scans m edges");
    }
}
