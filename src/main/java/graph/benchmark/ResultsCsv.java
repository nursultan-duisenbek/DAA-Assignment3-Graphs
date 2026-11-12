package graph.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Writes and reads {@code results.csv}. */
public final class ResultsCsv {

    private ResultsCsv() {
    }

    /** Writes the header and one line per row. */
    public static void write(Path file, List<ResultRow> rows) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        List<String> lines = new ArrayList<>(rows.size() + 1);
        lines.add(ResultRow.CSV_HEADER);
        for (ResultRow row : rows) {
            lines.add(row.toCsv());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Reads a file written by {@link #write(Path, List)}. */
    public static List<ResultRow> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).trim().equals(ResultRow.CSV_HEADER)) {
            throw new IOException(file + " does not start with the header " + ResultRow.CSV_HEADER);
        }
        List<ResultRow> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (!line.isBlank()) {
                rows.add(ResultRow.fromCsv(line.trim()));
            }
        }
        return rows;
    }
}
