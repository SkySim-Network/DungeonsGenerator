package me.kp56.dungeonsgen.generator.graphs.traverse;

import lombok.Getter;
import me.kp56.dungeonsgen.generator.graphs.Graph;
import org.apache.commons.lang.SerializationUtils;

import java.util.HashMap;
import java.util.Map;

public class DFS extends TraverseAlgorithm {
    @Getter
    private boolean[] visited;
    public final int startingNode;

    public DFS(Graph graph, TraverseStepHandler handler, int startingNode) {
        super(graph, handler);

        this.startingNode = startingNode;
    }

    @Override
    public void traverse() {
        visited = new boolean[graph.size()];

        for (int i = 0; i < graph.size(); i++) {
            int n = (i + startingNode) % graph.size();

            if (!visited[n]) {
                recursiveFunction(n, new HashMap<>());
            }
        }
    }

    private void recursiveFunction(int at, HashMap<String, Object> info) {
        visited[at] = true;

        for (int i : graph.getConnected(at)) {
            if (!visited[i]) {
                HashMap<String, Object> copy = (HashMap<String, Object>) SerializationUtils.clone(info);
                if (handler.handleStep(at, i, this, copy)) {
                    recursiveFunction(i, copy);
                }
            }
        }
    }
}
