package graph.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Strongly connected components: {@code componentOf[v]} is the id ({@code 0 .. count-1}) of
 * the component that contains {@code v}.
 *
 * <p>Kosaraju numbers components in a topological order of the condensation (component 0
 * has no incoming edge from another component); Tarjan numbers them in reverse topological
 * order. Both partitions are the same.
 */
public final class SccResult {

    private final int count;
    private final int[] componentOf;

    SccResult(int count, int[] componentOf) {
        this.count = count;
        this.componentOf = componentOf;
    }

    /** @return number of strongly connected components */
    public int count() {
        return count;
    }

    /** @return component id of vertex {@code v} */
    public int componentOf(int v) {
        return componentOf[v];
    }

    /** @return copy of the component id of every vertex */
    public int[] componentIds() {
        return componentOf.clone();
    }

    /** @return {@code true} if {@code u} and {@code v} are mutually reachable */
    public boolean sameComponent(int u, int v) {
        return componentOf[u] == componentOf[v];
    }

    /** @return vertices of each component, grouped by component id, each list ascending */
    public List<List<Integer>> components() {
        List<List<Integer>> groups = new ArrayList<>(count);
        for (int c = 0; c < count; c++) {
            groups.add(new ArrayList<>());
        }
        for (int v = 0; v < componentOf.length; v++) {
            groups.get(componentOf[v]).add(v);
        }
        List<List<Integer>> result = new ArrayList<>(count);
        for (List<Integer> g : groups) {
            result.add(Collections.unmodifiableList(g));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * @return {@code true} if {@code other} groups the vertices into exactly the same components
     *         (component ids may be numbered differently)
     */
    public boolean samePartitionAs(SccResult other) {
        if (other.componentOf.length != componentOf.length || other.count != count) {
            return false;
        }
        int[] mapping = new int[count];
        Arrays.fill(mapping, -1);
        int[] reverse = new int[count];
        Arrays.fill(reverse, -1);
        for (int v = 0; v < componentOf.length; v++) {
            int a = componentOf[v];
            int b = other.componentOf[v];
            if (mapping[a] == -1 && reverse[b] == -1) {
                mapping[a] = b;
                reverse[b] = a;
            } else if (mapping[a] != b || reverse[b] != a) {
                return false;
            }
        }
        return true;
    }

    /** @return size of the largest component (0 for an empty graph) */
    public int largestComponentSize() {
        int[] sizes = new int[count];
        int best = 0;
        for (int c : componentOf) {
            best = Math.max(best, ++sizes[c]);
        }
        return best;
    }
}
