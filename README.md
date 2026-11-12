# DAA Assignment 3 — Graphs: City Network & Job Scheduler

Java 17 / Maven project with hand-written graph algorithms, operation counters and a
self-checking benchmark:

| Component | Class(es) |
|---|---|
| Graph core (adjacency lists, directed + undirected, file reader) | `graph.Graph`, `graph.Edge`, `graph.GraphReader` |
| Disjoint Set Union (union by rank + path compression, counters) | `graph.dsu.DisjointSetUnion` |
| Minimum spanning tree / forest | `graph.algorithms.Kruskal`, `graph.algorithms.Prim`, `graph.algorithms.EdgeSorter` |
| Strongly connected components (iterative DFS, explicit stack) | `graph.algorithms.Kosaraju` |
| Topological order with cycle detection | `graph.algorithms.TopologicalSort` (Kahn) |
| Shortest paths in a DAG (super-source, negative weights, path restoring) | `graph.algorithms.DagShortestPaths` |
| Bonus A: Tarjan SCC (single pass) | `graph.algorithms.TarjanScc` |
| Bonus B: condensation + critical path | `graph.algorithms.Condensation`, `graph.algorithms.CriticalPath` |
| Counters, median, log-log slope | `graph.metrics.Metrics`, `graph.metrics.Stats` |
| Exact generator, benchmark, CSV, analysis, PNG plots | `graph.benchmark.*` |

All 30 reference values of the assignment (MST weight, SCC count and DAG distance sum for the
ten graphs) are reproduced exactly; see [`REPORT.md`](REPORT.md) for the analysis.

## Requirements

* JDK 17 or newer (tested with OpenJDK 21, compiled with `--release 17`)
* Maven 3.8+

No other dependencies: JUnit 5 is the only (test) library, and the plots are drawn with Java2D.

## Build and test

```bash
mvn clean test
```

Runs 142 JUnit 5 tests (about 20 s): DSU vs a naive reference, Kruskal = Prim on 100 random
connected graphs, Kosaraju/Tarjan vs brute-force reachability, Kahn order validation and cycle
detection, DAG shortest paths vs Bellman-Ford (with negative weights), a 100 000-vertex
stack-safety test and the full reference table.

## Run the benchmark

One command regenerates every number in the report:

```bash
mvn -q compile exec:exec
```

or, with a packaged jar:

```bash
mvn -q clean package
```

```bash
java -Xmx3g -jar target/daa-assignment3-graphs-1.0.jar
```

The run takes about a minute and writes:

| File | Content |
|---|---|
| `results/results.csv` | 50 rows (5 algorithms × 10 graphs): `algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result` |
| `results/plots/operations_vs_n.png` | total counted operations vs n, log-log, per algorithm and density |
| `results/plots/scans_per_edge.png` | `edge_scans / m` vs n |
| `results/plots/kruskal_edge_share.png` | share of edges Kruskal examines, sparse vs dense |
| `results/analysis.md` | counter/formula ratios, log-log slopes, median times, reference check |
| `results/bonus.csv` | Tarjan vs Kosaraju edge scans, condensation and critical-path lengths |

The benchmark **checks itself** and exits with status 1, without writing the CSV, if any of
these fail: Kruskal and Prim agree on the MST weight, the Kahn order satisfies `pos[u] < pos[v]`,
the DAG distances pass an optimality certificate, Kosaraju scans exactly `2m`, Kahn and the DAG
relaxation exactly `m` edges, the counters are identical in all 5 runs, and every computed
reference value equals the assignment table (computed and expected values are printed side by
side).

Options (after `java -jar ...jar`):

```text
--out DIR        output directory (default results)
--runs K         timed runs per algorithm, median reported (default 5)
--sizes a,b,c    vertex counts (default 1000,3162,10000,31623,100000)
--no-plots       skip the PNG plots
--no-bonus       skip Tarjan / critical path
```

Regenerate only the plots from an existing CSV:

```bash
java -cp target/classes graph.benchmark.Plots results/results.csv results/plots
```

## Input format and sample graphs

```text
n m directed|undirected
u v w          (m lines; vertices 0..n-1, integer weights, '#' starts a comment)
```

| File | Kind | Purpose |
|---|---|---|
| `data/city_roads.txt` | undirected, connected | parallel edge + self-loop; MST weight 14 |
| `data/disconnected_roads.txt` | undirected, **disconnected** | isolated vertex + self-loop; spanning forest of 4 components, weight 10 |
| `data/jobs_cyclic.txt` | directed, **cyclic** | 5 SCCs; Kahn must report a cycle |
| `data/pipeline_dag.txt` | DAG, negative weights | distances `[0,0,3,-2,4,-1,1]`, path `1 -> 4 -> 6`, critical path 11 |

All four files are used by the tests.

## Benchmark generator

Every graph is generated with its own `new Random(42)`, drawing numbers in exactly this order.
The undirected graph does a Fisher-Yates shuffle of `perm`, then adds the spanning-tree edges
`(perm[nextInt(i)], perm[i])`, then random edges `a = nextInt(n)`, `b = nextInt(n)` with `b`
redrawn while `b == a`. The directed graph uses only the random edges. The DAG shuffles `rank`
and orients each random edge from lower to higher rank. Every weight is `1 + nextInt(1000)`,
drawn right after its edge's endpoints. Drawing weights after all endpoints, or redrawing both
endpoints of a self-loop, produces different graphs and misses the reference table.

## Project layout

```text
src/main/java/graph/            Graph, Edge, GraphReader
src/main/java/graph/dsu/        DisjointSetUnion
src/main/java/graph/algorithms/ Kruskal, Prim, EdgeSorter, Kosaraju, TarjanScc, TopologicalSort,
                                DagShortestPaths, Condensation, CriticalPath + result types
src/main/java/graph/metrics/    Metrics (counters), Stats (median, log-log slope)
src/main/java/graph/benchmark/  GraphGenerator, ReferenceValues, BenchmarkRunner, Analysis,
                                ResultsCsv, Plots, LineChart
src/test/java/graph/...         JUnit 5 tests (same package structure)
data/                           sample input graphs
results/                        results.csv, analysis.md, bonus.csv, plots/
REPORT.md                       analysis report
```

## Git workflow

`main` holds only working code and is tagged `v1.0`. Each part was developed on its own branch
(`feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics`) and merged with
`--no-ff`, so the branch structure stays visible in `git log --graph`.

## AI-use acknowledgement

This project was developed with the help of an AI coding assistant (Claude, by Anthropic), used
through Claude Code. It assisted with the implementation, the tests, the benchmark and plot code,
and the drafting of this README and the report. The assistant's work was checked mechanically:
every algorithm is cross-checked by the test suite (brute force, Bellman-Ford, exhaustive
enumeration). The benchmark reproduces the assignment's reference table exactly and verifies the
counter identities on every run. All numbers in the report are taken from the generated
`results/` files. The submitting student is responsible for understanding and defending the code.
