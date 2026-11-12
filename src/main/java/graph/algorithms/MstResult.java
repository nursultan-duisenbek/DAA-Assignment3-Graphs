package graph.algorithms;

import graph.Edge;

import java.util.List;

/**
 * Result of Kruskal or Prim: a minimum spanning <b>forest</b> (a tree when the graph is
 * connected).
 *
 * @param totalWeight sum of the weights of the chosen edges
 * @param edges       the chosen edges, {@code n - components} of them
 * @param components  number of connected components of the input graph (isolated vertices
 *                    count as components); {@code 1} means a spanning tree
 */
public record MstResult(long totalWeight, List<Edge> edges, int components) {

    public MstResult {
        edges = List.copyOf(edges);
    }

    /** @return number of edges in the forest */
    public int edgeCount() {
        return edges.size();
    }

    /** @return {@code true} if the graph was connected, i.e. the forest is one spanning tree */
    public boolean isSpanningTree() {
        return components <= 1;
    }
}
