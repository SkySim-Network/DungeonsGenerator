package me.kp56.dungeonsgen.generator.graphs.traverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShortestPathFinder implements TraverseAlgorithm.TraverseStepHandler {
    private final FoundPath onceFound;
    public final int to;

    private boolean foundPath = false;

    public ShortestPathFinder(int to, FoundPath onceFound) {
        this.to = to;
        this.onceFound = onceFound;
    }

    @Override
    public boolean handleStep(int node1, int node2, TraverseAlgorithm algorithm, Map<String, Object> info) {
        if (!info.containsKey("prev")) {
            info.put("prev", new ArrayList<Integer>());
        }

        List<Integer> prev = (List<Integer>) info.get("prev");
        prev.add(node1);

        if (node1 == to) {
            foundPath = true;
            onceFound.onceFound(prev);
        }

        return !foundPath;
    }

    public static interface FoundPath {
        void onceFound(List<Integer> path);
    }
}
