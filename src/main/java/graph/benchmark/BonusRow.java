package graph.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One row of {@code bonus.csv} (kept separate so that {@code results.csv} has exactly the 50
 * required rows). Directed graphs compare Tarjan with Kosaraju and give the critical path of
 * their condensation; DAG rows give the critical (longest) path of the DAG itself, where every
 * vertex is its own component and the Tarjan/Kosaraju columns are {@code -1}.
 */
public record BonusRow(String graph, String density, int n, int m, long kosarajuScans, long tarjanScans,
                       int kosarajuComponents, int tarjanComponents, long kosarajuMaxDepth, long tarjanMaxDepth,
                       double kosarajuMs, double tarjanMs, int largestComponent, long criticalPathLength,
                       int criticalPathVertices) {

    /** CSV header of {@code bonus.csv}. */
    public static final String CSV_HEADER = "graph,n,m,kosaraju_edge_scans,tarjan_edge_scans,kosaraju_components,"
            + "tarjan_components,kosaraju_max_depth,tarjan_max_depth,kosaraju_time_ms,tarjan_time_ms,"
            + "largest_component,critical_path_length,critical_path_vertices";

    /** @return this row as a CSV line */
    public String toCsv() {
        return String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%d,%d,%d", graph, n, m, kosarajuScans,
                tarjanScans, kosarajuComponents, tarjanComponents, kosarajuMaxDepth, tarjanMaxDepth, ms(kosarajuMs),
                ms(tarjanMs), largestComponent, criticalPathLength, criticalPathVertices);
    }

    private static String ms(double value) {
        return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%.3f", value);
    }

    /** Writes {@code bonus.csv}. */
    public static void write(Path file, List<BonusRow> rows) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER);
        for (BonusRow row : rows) {
            lines.add(row.toCsv());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
