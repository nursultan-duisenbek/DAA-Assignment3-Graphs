package graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphReaderTest {

    @Test
    void readsCityRoads() throws IOException {
        Graph g = GraphReader.read(TestGraphs.DATA.resolve("city_roads.txt"));
        assertFalse(g.isDirected());
        assertEquals(6, g.vertexCount());
        assertEquals(10, g.edgeCount());
        Edge loop = g.edges().get(9);
        assertTrue(loop.isSelfLoop());
        assertEquals(9, loop.weight());
    }

    @Test
    void readsAllSampleGraphs() throws IOException {
        Graph disconnected = GraphReader.read(TestGraphs.DATA.resolve("disconnected_roads.txt"));
        assertEquals(8, disconnected.vertexCount());
        assertEquals(7, disconnected.edgeCount());

        Graph cyclic = GraphReader.read(TestGraphs.DATA.resolve("jobs_cyclic.txt"));
        assertTrue(cyclic.isDirected());
        assertEquals(10, cyclic.edgeCount());

        Graph dag = GraphReader.read(TestGraphs.DATA.resolve("pipeline_dag.txt"));
        assertTrue(dag.isDirected());
        assertEquals(-2, dag.edges().get(2).weight(), "negative weights are read");
    }

    @Test
    void ignoresCommentsBlankLinesAndCase() {
        Graph g = GraphReader.parse("""
                # header comment

                3 2 DIRECTED   # trailing comment
                0 1 5
                  1   2   -7
                """);
        assertTrue(g.isDirected());
        assertEquals(2, g.edgeCount());
        assertEquals(-7, g.edges().get(1).weight());
    }

    @Test
    void zeroEdgesAndIsolatedVertices() {
        Graph g = GraphReader.parse("5 0 undirected\n");
        assertEquals(5, g.vertexCount());
        assertEquals(0, g.edgeCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",                                   // missing header
        "3 1\n0 1 1\n",                       // header without kind
        "3 1 mixed\n0 1 1\n",                 // unknown kind
        "x 1 directed\n0 1 1\n",              // n not an integer
        "-1 0 directed\n",                    // negative n
        "3 2 directed\n0 1 1\n",              // too few edges
        "3 1 directed\n0 1 1\n1 2 1\n",       // too many edges
        "3 1 directed\n0 3 1\n",              // vertex out of range
        "3 1 directed\n0 1\n",                // missing weight
        "3 1 directed\n0 1 w\n"               // weight not an integer
    })
    void rejectsMalformedInputWithAClearError(String text) {
        GraphReader.GraphFormatException e =
                assertThrows(GraphReader.GraphFormatException.class, () -> GraphReader.parse(text));
        assertTrue(e.getMessage().startsWith("<string>:"), "message carries the line: " + e.getMessage());
    }

    @Test
    void missingFileIsAnIoException() {
        assertThrows(IOException.class, () -> GraphReader.read(TestGraphs.DATA.resolve("does-not-exist.txt")));
    }
}
