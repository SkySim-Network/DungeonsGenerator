package me.kp56.dungeonsgen.generator.graphs.traverse;

import lombok.Getter;
import me.kp56.dungeonsgen.generator.graphs.Graph;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class BFS extends TraverseAlgorithm {
    @Getter
    private boolean[] visited;
    public final int startingNode;

    public BFS(Graph graph, TraverseStepHandler handler, int startingNode) {
        super(graph, handler);

        this.startingNode = startingNode;
    }

    @Override
    public void traverse() {
        visited = new boolean[graph.size()];

        for (int node = 0; node < graph.size(); node++) {
            int n = (node + startingNode) % graph.size();
            if (!visited[n]) {
                Queue<Pair<Integer, Map<String, Object>>> queue = new ArrayDeque<>();
                queue.add(Pair.of(n, new HashMap<>()));

                while (!queue.isEmpty()) {
                    Pair<Integer, Map<String, Object>> a = queue.poll();
                    int i = a.getKey();
                    HashMap<String, Object> info = (HashMap<String, Object>) a.getValue();

                    visited[i] = true;

                    for (int j : graph.getConnected(i)) {
                        if (!visited[j]) {
                            Map<String, Object> copy = (Map<String, Object>) SerializationUtils.clone(info);
                            if (handler.handleStep(i, j, this, copy)) {
                                queue.add(Pair.of(j, copy));
                            }
                        }
                    }
                }
            }
        }
    }
}
