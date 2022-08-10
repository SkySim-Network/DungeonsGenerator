package me.kp56.dungeonsgen.generator.graphs.traverse;

import lombok.Getter;
import me.kp56.dungeonsgen.generator.graphs.Graph;

import java.util.Map;

public abstract class TraverseAlgorithm {
    @Getter
    protected Graph graph;
    protected TraverseAlgorithm.TraverseStepHandler handler;

    protected TraverseAlgorithm(Graph graph, TraverseAlgorithm.TraverseStepHandler handler) {
        this.graph = graph;
        this.handler = handler;
    }

    abstract void traverse();

    @FunctionalInterface
    public static interface TraverseStepHandler {
        boolean handleStep(int node1, int node2, TraverseAlgorithm algorithm, Map<String, Object> info);
    }
}
