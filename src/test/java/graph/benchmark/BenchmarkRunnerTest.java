package graph.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkRunnerTest {

    @Test
    void smallRunWritesCsvAnalysisPlotsAndBonus(@TempDir Path dir) throws IOException {
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        BenchmarkRunner.Config config = new BenchmarkRunner.Config(dir, new int[] {1000, 3162}, 2, true, true);
        BenchmarkRunner.Summary summary =
                new BenchmarkRunner(config, new PrintStream(log, true, StandardCharsets.UTF_8)).run();

        List<String> lines = Files.readAllLines(dir.resolve("results.csv"));
        assertEquals(ResultRow.CSV_HEADER, lines.get(0));
        assertEquals(1 + 2 * 2 * 5, lines.size(), "5 algorithms x 2 densities x 2 sizes");
        assertEquals(20, summary.rows().size());
        assertEquals(12, summary.referenceChecks().size());
        assertTrue(summary.referenceChecks().stream().allMatch(BenchmarkRunner.ReferenceCheck::ok));
        List<ResultRow> fromCsv = ResultsCsv.read(dir.resolve("results.csv"));
        for (int i = 0; i < fromCsv.size(); i++) {
            ResultRow written = summary.rows().get(i);
            ResultRow read = fromCsv.get(i);
            assertEquals(withoutTime(written), withoutTime(read), "CSV row " + i);
            assertEquals(written.timeMs(), read.timeMs(), 0.0005, "time is written with 3 decimals");
        }

        for (String plot : List.of("operations_vs_n.png", "scans_per_edge.png", "kruskal_edge_share.png")) {
            Path png = dir.resolve("plots").resolve(plot);
            assertTrue(Files.size(png) > 10_000, plot + " was rendered");
        }
        assertTrue(Files.readString(dir.resolve("analysis.md")).contains("Log-log slopes"));
        assertEquals(1 + 2 * 2 * 2, Files.readAllLines(dir.resolve("bonus.csv")).size());
        String out = log.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("147970 = 147970 OK"), "reference values are printed next to the computed ones");
    }

    private static ResultRow withoutTime(ResultRow r) {
        return new ResultRow(r.algorithm(), r.graph(), r.density(), r.n(), r.m(), 0, r.dsuOps(), r.edgeScans(),
                r.comparisons(), r.maxDepth(), r.result());
    }

    @Test
    void csvRowsRoundTrip() {
        ResultRow row = new ResultRow("Kruskal", "undirected_sparse_1000", "sparse", 1000, 4000, 1.5, 9161, 3082,
                42889, 0, 147970);
        ResultRow back = ResultRow.fromCsv(row.toCsv());
        assertEquals(row, back);
        assertEquals("Kruskal,undirected_sparse_1000,1000,4000,1.500,9161,3082,42889,0,147970", row.toCsv());
        assertThrows(IllegalArgumentException.class, () -> ResultRow.fromCsv("a,b,c"));
    }

    @Test
    void parsesCommandLine() {
        BenchmarkRunner.Config c = BenchmarkRunner.parseArgs(
                new String[] {"--out", "x", "--runs", "3", "--sizes", "1000, 3162", "--no-plots", "--no-bonus"});
        assertEquals(Path.of("x"), c.outputDir());
        assertEquals(3, c.runs());
        assertArrayEquals(new int[] {1000, 3162}, c.sizes());
        assertTrue(!c.plots() && !c.bonus());
        BenchmarkRunner.Config d = BenchmarkRunner.parseArgs(new String[0]);
        assertArrayEquals(BenchmarkRunner.DEFAULT_SIZES, d.sizes());
        assertEquals(5, d.runs());
        assertThrows(IllegalArgumentException.class, () -> BenchmarkRunner.parseArgs(new String[] {"--bogus"}));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkRunner.parseArgs(new String[] {"--out"}));
    }
}
