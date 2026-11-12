package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Reads a graph from the text format of the assignment:
 *
 * <pre>
 * n m directed|undirected
 * u v w      (m lines)
 * </pre>
 *
 * Blank lines are ignored and {@code #} starts a comment that runs to the end of the line.
 * Every problem in the input (wrong token count, vertex out of range, too few or too many
 * edge lines, ...) is reported as a {@link GraphFormatException} with the line number.
 */
public final class GraphReader {

    private GraphReader() {
    }

    /** Thrown when the input does not follow the graph file format. */
    public static final class GraphFormatException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        GraphFormatException(String source, int line, String message) {
            super(source + ":" + line + ": " + message);
        }
    }

    /**
     * Reads a graph file.
     *
     * @throws IOException          if the file cannot be read
     * @throws GraphFormatException if the content is malformed
     */
    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader, path.toString());
        }
    }

    /**
     * Parses a graph from a string in the file format.
     *
     * @throws GraphFormatException if the content is malformed
     */
    public static Graph parse(String text) {
        try {
            return read(new BufferedReader(new StringReader(text)), "<string>");
        } catch (IOException e) {
            throw new UncheckedIOException(e); // cannot happen for a StringReader
        }
    }

    private static Graph read(BufferedReader reader, String source) throws IOException {
        Graph graph = null;
        int expectedEdges = 0;
        int readEdges = 0;
        int lineNo = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            int hash = line.indexOf('#');
            String content = (hash >= 0 ? line.substring(0, hash) : line).trim();
            if (content.isEmpty()) {
                continue;
            }
            String[] tokens = content.split("\\s+");
            if (graph == null) {
                if (tokens.length != 3) {
                    throw new GraphFormatException(source, lineNo,
                            "header must be 'n m directed|undirected', got '" + content + "'");
                }
                int n = parseInt(tokens[0], source, lineNo, "vertex count");
                expectedEdges = parseInt(tokens[1], source, lineNo, "edge count");
                if (n < 0 || expectedEdges < 0) {
                    throw new GraphFormatException(source, lineNo, "n and m must be non-negative");
                }
                String kind = tokens[2].toLowerCase(Locale.ROOT);
                boolean directed;
                if (kind.equals("directed")) {
                    directed = true;
                } else if (kind.equals("undirected")) {
                    directed = false;
                } else {
                    throw new GraphFormatException(source, lineNo,
                            "graph kind must be 'directed' or 'undirected', got '" + tokens[2] + "'");
                }
                graph = new Graph(n, directed, expectedEdges);
                continue;
            }
            if (tokens.length != 3) {
                throw new GraphFormatException(source, lineNo, "edge line must be 'u v w', got '" + content + "'");
            }
            if (readEdges == expectedEdges) {
                throw new GraphFormatException(source, lineNo, "more than the declared " + expectedEdges + " edges");
            }
            int u = parseInt(tokens[0], source, lineNo, "vertex u");
            int v = parseInt(tokens[1], source, lineNo, "vertex v");
            int w = parseInt(tokens[2], source, lineNo, "weight w");
            int n = graph.vertexCount();
            if (u < 0 || u >= n || v < 0 || v >= n) {
                throw new GraphFormatException(source, lineNo,
                        "edge " + u + " " + v + " has a vertex outside 0.." + (n - 1));
            }
            graph.addEdge(u, v, w);
            readEdges++;
        }
        if (graph == null) {
            throw new GraphFormatException(source, lineNo, "missing header line");
        }
        if (readEdges != expectedEdges) {
            throw new GraphFormatException(source, lineNo,
                    "declared " + expectedEdges + " edges but found " + readEdges);
        }
        return graph;
    }

    private static int parseInt(String token, String source, int line, String what) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new GraphFormatException(source, line, what + " is not an integer: '" + token + "'");
        }
    }
}
